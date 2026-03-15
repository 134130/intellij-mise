package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.util.getWslDistribution
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.github.l34130.mise.core.wsl.WslPathUtils.maybeConvertWindowsUncToUnixPath
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.containers.CollectionFactory
import java.nio.file.Paths
import kotlin.io.path.pathString

object MiseCommandLineHelper {
    private val logger = Logger.getInstance(MiseCommandLineHelper::class.java)

    /**
     * Marker added to environment variables to prevent double-injection in the case where
     * multiple env customizers are called.
     *
     * NOTE: We intentionally do NOT store this marker in the environment map anymore because it leaks into child
     * processes and can show up in run configurations. Instead we track marker state by env map identity.
     */
    private enum class EnvCustomizationState { DONE, SKIP }

    /**
     * Tracks customization state by env-map identity.
     *
     * Weak keys ensure we don't retain per-command maps longer than necessary.
     */
    private val envCustomizationStateByMap =
        CollectionFactory.createConcurrentWeakIdentityMap<MutableMap<String, String>, EnvCustomizationState>()

    /**
     * Check if the mise plugin needs to customize environment variables.
     * @return true if customization is required, false otherwise
     */
    fun environmentNeedsCustomization(environment: MutableMap<String, String>): Boolean =
        envCustomizationStateByMap[environment] == null

    /**
     * Mark env as customized to prevent double-injection for this env-map instance.
     */
    fun environmentHasBeenCustomized(environment: MutableMap<String, String>) {
        envCustomizationStateByMap[environment] = EnvCustomizationState.DONE
    }


    /**
     * Mark env customization as intentionally skipped for this env-map instance.
     * Used to prevent recursion when executing mise itself.
     */
    fun environmentSkipCustomization(environment: MutableMap<String, String>) {
        envCustomizationStateByMap[environment] = EnvCustomizationState.SKIP
    }

    /**
     * Clear marker when customization fails, allowing retry.
     */
    fun environmentCustomizationFailed(environment: MutableMap<String, String>) {
        envCustomizationStateByMap.remove(environment)
    }

    /**
     * Safely gets an executable path from GeneralCommandLine, returning null if IllegalStateException occurs.
     * @return executable path or null if not set
     */
    fun safeGetExePath(commandLine: GeneralCommandLine): String? {
        return try {
            commandLine.exePath
        } catch (_: IllegalStateException) {
            null
        }
    }

    /**
     * Checks if executable matches given names (e.g., "nx", "nx.cmd").
     * @param commandLine the command line to check
     * @param executableNames list of executable names to match against
     * @return true if executable matches any of the given names
     */
    fun matchesExecutableNames(commandLine: GeneralCommandLine, executableNames: List<String>): Boolean {
        val exePath = safeGetExePath(commandLine) ?: return false
        return Paths.get(exePath).fileName.toString() in executableNames
    }

    /**
     * Resolves project from GeneralCommandLine working directory.
     *
     * Uses fast paths that do not require a read action:
     * - Single open project: returned immediately
     * - Multi-project: falls back to basePath prefix matching (sufficient for env customization context)
     *
     * This approach is deadlock-safe even when called from a write-action context.
     *
     * @param commandLine the command line to resolve the project from
     * @return Project or null if not found
     */
    fun resolveProjectFromCommandLine(commandLine: GeneralCommandLine): Project? {
        val workDir = commandLine.workingDirectory?.pathString
        if (workDir == null) {
            logger.trace("Project resolve failed: workingDirectory is null (cmd=$commandLine)")
            return null
        }
        if (LocalFileSystem.getInstance().findFileByPath(workDir) == null) {
            logger.trace("Project resolve failed: workingDirectory not found in LocalFileSystem (workDir=$workDir)")
            return null
        }

        val projectManager = ProjectManager.getInstanceIfCreated()
        if (projectManager == null) {
            logger.trace("Project resolve failed: ProjectManager not created yet (workDir=$workDir)")
            return null
        }
        val openProjects = projectManager.openProjects
        if (openProjects.isEmpty()) {
            logger.trace("Project resolve failed: no open projects (workDir=$workDir)")
            return null
        }

        // Fast path: single open project — no read action needed
        if (openProjects.size == 1) return openProjects[0]

        // Multi-project: basePath prefix matching — normalize paths for WSL/UNC consistency
        val resolvedProject = openProjects.firstOrNull { project ->
            project.basePath?.let { basePath ->
                WslPathUtils.isAncestor(basePath, workDir, false)
            } == true
        }
        if (resolvedProject == null && logger.isTraceEnabled) {
            val basePaths = openProjects.mapNotNull { it.basePath }
            logger.trace("Project resolve failed: no basePath match (workDir=$workDir, openProjectBasePaths=$basePaths)")
        }
        return resolvedProject
    }

    /**
     * Resolves working directory from GeneralCommandLine, falling back to the project path if null.
     * @param commandLine the command line to get working directory from
     * @param project the project to use as fallback
     * @return working directory path
     */
    fun resolveWorkingDirectory(commandLine: GeneralCommandLine, project: Project): String {
        return commandLine.workingDirectory?.pathString ?: project.guessMiseProjectPath()
    }

    // mise env
    fun getEnvVars(
        project: Project,
        workDir: String = project.guessMiseProjectPath(),
        configEnvironment: String? = null,
    ): Result<Map<String, String>> {
        val cache = project.service<MiseCommandCache>()
        val cacheKey = MiseCacheKey.EnvVars(workDir, configEnvironment)
        return cache.getCachedWithProgress(cacheKey) {
            val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
            miseCommandLine.runCommandLine(listOf("env", "--json"))
        }
    }

    // mise env
    fun getEnvVarsExtended(
        project: Project,
        workDir: String = project.guessMiseProjectPath(),
        configEnvironment: String? = null,
    ): Result<Map<String, MiseEnvExtended>> {
        val cache = project.service<MiseCommandCache>()
        val cacheKey = MiseCacheKey.EnvVarsExtended(workDir, configEnvironment)
        return cache.getCachedWithProgress(cacheKey) {
            val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)

            val envs =
                miseCommandLine
                    .runCommandLine<Map<String, MiseEnv>>(listOf("env", "--json-extended"))
                    .getOrElse { return@getCachedWithProgress Result.failure(it) }

            val redactedEnvKeys =
                miseCommandLine
                    .runCommandLine<Map<String, String>>(listOf("env", "--json", "--redacted"))
                    .getOrElse { emptyMap() }
                    .keys

            val extendedEnvs =
                envs.mapValues { (key, env) ->
                    MiseEnvExtended(
                        value = env.value,
                        source = env.source,
                        tool = env.tool,
                        redacted = redactedEnvKeys.contains(key),
                    )
                }

            Result.success(extendedEnvs)
        }
    }

    /**
     * `mise ls`
     * Returns all dev tools visible to the project — both locally-pinned tools (from the project's
     * own `mise.toml`) and globally-installed tools (from `~/.config/mise/config.toml`).
     *
     * Results are fetched with two separate `mise ls` calls (`--local` and `--global`) and then
     * merged: locally-pinned tools take precedence over global ones, so if a tool is defined in
     * both places the project's version is used.
     *
     * Results are cached; call [getDevTools] with an explicit [MiseDevToolsScope] if you need only
     * local or only global tools.
     */
    fun getDevTools(
        project: Project,
        workDir: String = project.guessMiseProjectPath(),
        configEnvironment: String? = null,
    ): Result<Map<MiseDevToolName, List<MiseDevTool>>> {
        val cache = project.service<MiseCommandCache>()
        val cacheKey = MiseCacheKey.DevTools(workDir, configEnvironment)
        return cache.getCachedWithProgress(cacheKey) {
            val localResult = getDevTools(project, workDir, configEnvironment, MiseDevToolsScope.LOCAL)
            val globalResult = getDevTools(project, workDir, configEnvironment, MiseDevToolsScope.GLOBAL)

            val local = localResult.getOrElse { return@getCachedWithProgress Result.failure(it) }
            val global = globalResult.getOrElse { return@getCachedWithProgress Result.failure(it) }

            Result.success(mergeDevTools(local, global))
        }
    }

    fun getDevTools(
        project: Project,
        workDir: String = project.guessMiseProjectPath(),
        configEnvironment: String? = null,
        scope: MiseDevToolsScope,
    ): Result<Map<MiseDevToolName, List<MiseDevTool>>> {
        val cache = project.service<MiseCommandCache>()
        val cacheKey = MiseCacheKey.DevTools(workDir, configEnvironment, scope)
        return cache.getCachedWithProgress(cacheKey) {
            val commandLineArgs = mutableListOf("ls", scope.commandFlag, "--json")
            val wslDistributionMsId = project.getWslDistribution()?.msId

            val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
            miseCommandLine
                .runCommandLine<Map<String, List<MiseDevTool>>>(commandLineArgs)
                .map { rawDevTools ->
                    // mise ls returns {"node": [...], "python": [...]} — keys are plain strings.
                    // Wrap each key in a MiseDevToolName value object and stamp every tool with
                    // the WSL distribution it was fetched from in a single pass, so that
                    // resolvedInstallPath can later convert Unix paths to Windows UNC paths.
                    // copy() produces a new immutable instance; the original is not mutated.
                    rawDevTools.entries.associate { (rawName, toolVersions) ->
                        MiseDevToolName(rawName) to toolVersions.map { tool -> tool.copy(wslDistributionMsId = wslDistributionMsId) }
                    }
                }
        }
    }

    internal fun mergeDevTools(
        local: Map<MiseDevToolName, List<MiseDevTool>>,
        global: Map<MiseDevToolName, List<MiseDevTool>>,
    ): Map<MiseDevToolName, List<MiseDevTool>> = global + local

    // mise config get
    @RequiresBackgroundThread
    fun getConfig(
        project: Project,
        workDir: String?,
        configEnvironment: String?,
        key: String,
    ): Result<String> {
        val commandLineArgs = mutableListOf("config", "get", key)

        val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
        return miseCommandLine.runRawCommandLine(commandLineArgs)
    }

    // mise config --tracked-configs
    @RequiresBackgroundThread
    fun getTrackedConfigs(
        project: Project,
        configEnvironment: String,
        workDir: String = project.guessMiseProjectPath(),
    ): Result<List<String>> {
        val commandLineArgs = mutableListOf("config", "--tracked-configs")

        val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
        return miseCommandLine
            .runRawCommandLine(commandLineArgs)
            .map { it.lines().map { line -> line.trim() }.filter { trimmed -> trimmed.isNotEmpty() } }
    }

    // mise exec
    @RequiresBackgroundThread
    fun executeCommand(
        project: Project,
        workDir: String?,
        configEnvironment: String?,
        command: List<String>,
    ): Result<String> {
        val commandLineArgs = mutableListOf("exec", "--") + command

        val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
        return miseCommandLine.runRawCommandLine(commandLineArgs)
    }

    // mise trust
    @RequiresBackgroundThread
    fun trustConfigFile(
        project: Project,
        configFilePath: String,
        configEnvironment: String,
    ): Result<Unit> {
        val wslSafePathConfigPath = maybeConvertWindowsUncToUnixPath(configFilePath)
        val commandLineArgs = mutableListOf("trust", wslSafePathConfigPath)

        // Use the project's base path as the working directory to ensure correct mise context
        // (Windows mise for Windows projects, WSL mise for WSL projects)
        val workDir = project.guessMiseProjectPath()

        val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
        return miseCommandLine.runCommandLine(commandLineArgs)
    }
}
