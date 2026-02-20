package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.wsl.WslPathUtils.maybeConvertWindowsUncToUnixPath
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.containers.CollectionFactory
import java.nio.file.Paths
import kotlin.io.path.pathString

object MiseCommandLineHelper {
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
     * @param commandLine the command line to resolve the project from
     * @return Project or null if not found
     */
    fun resolveProjectFromCommandLine(commandLine: GeneralCommandLine): Project? {
        val workDir = commandLine.workingDirectory?.pathString ?: return null
        val vf = LocalFileSystem.getInstance().findFileByPath(workDir) ?: return null
        return ProjectLocator.getInstance().guessProjectForFile(vf)
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


    // mise ls
    fun getDevTools(
        project: Project,
        workDir: String = project.guessMiseProjectPath(),
        configEnvironment: String? = null,
    ): Result<Map<MiseDevToolName, List<MiseDevTool>>> {
        val cache = project.service<MiseCommandCache>()
        val cacheKey = MiseCacheKey.DevTools(workDir, configEnvironment)
        return cache.getCachedWithProgress(cacheKey) {
            val commandLineArgs = mutableListOf("ls", "--local", "--json")

            val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
            miseCommandLine
                .runCommandLine<Map<String, List<MiseDevTool>>>(commandLineArgs)
                .map { devTools ->
                    devTools.mapKeys { (toolName, _) -> MiseDevToolName(toolName) }
                }
        }
    }

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
