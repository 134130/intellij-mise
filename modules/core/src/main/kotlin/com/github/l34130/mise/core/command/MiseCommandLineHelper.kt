package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.github.l34130.mise.core.wsl.WslPathUtils.maybeConvertWindowsUncToUnixPath
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.nio.file.Paths
import kotlin.io.path.pathString

object MiseCommandLineHelper {
    /**
     * Marker added to environment variables to prevent double-injection in the case where
     * multiple env customizers are called.
     * This marker is checked by all customizers to skip injection if already done.
     */
    const val INJECTION_MARKER_KEY = "_MISE_PLUGIN_ENV_VARS_CUSTOMIZATION"
    const val INJECTION_MARKER_VALUE_DONE = "done"
    const val INJECTION_MARKER_VALUE_SKIP = "skipped"

    /**
     * Check if the mise plugin needs to customize environment variables.
     * @return true if customization is required, false otherwise
     */
    fun <K, V> environmentNeedsCustomization(environment: MutableMap<K, V>): Boolean
            where K : String?, V : String? {
        @Suppress("UNCHECKED_CAST")
        return !environment.containsKey(INJECTION_MARKER_KEY as K) ||
                (
                        environment[INJECTION_MARKER_KEY as K] != INJECTION_MARKER_VALUE_DONE
                                && environment[INJECTION_MARKER_KEY as K] != INJECTION_MARKER_VALUE_SKIP
                        )
    }

    /**
     * Add injection marker to environment to prevent double-injection.
     * This marker is checked by all customizers to skip injection if already done.
     * Supports both nullable and non-nullable map types.
     */
    fun <K, V> environmentHasBeenCustomized(environment: MutableMap<K, V>)
            where K : String?, V : String? {
        @Suppress("UNCHECKED_CAST")
        environment[INJECTION_MARKER_KEY as K] = INJECTION_MARKER_VALUE_DONE as V
    }

    /**
     * Add injection marker to environment to prevent double-injection.
     * This marker is checked by all customizers to skip injection if already done.
     */
    fun environmentSkipCustomization(environment: MutableMap<String?, String?>) {
        environment[INJECTION_MARKER_KEY] = INJECTION_MARKER_VALUE_SKIP
    }

    /**
     * Remove injection marker when customization fails, allowing retry.
     * Supports both nullable and non-nullable map types.
     */
    fun <K, V> environmentCustomizationFailed(environment: MutableMap<K, V>)
            where K : String?, V : String? {
        @Suppress("UNCHECKED_CAST")
        environment.remove(INJECTION_MARKER_KEY as K)
    }

    /**
     * Safely gets an executable path from GeneralCommandLine, returning null if IllegalStateException occurs.
     * @return executable path or null if not set
     */
    fun safeGetExePath(commandLine: GeneralCommandLine): String? = runCatching { commandLine.exePath }.getOrNull()

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
        val cacheKey = MiseCacheKey.DevTools(workDir, configEnvironment, MiseDevToolsScope.COMBINED)
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
        require(scope != MiseDevToolsScope.COMBINED) { "Use getDevTools without a scope for combined results." }

        val cache = project.service<MiseCommandCache>()
        val cacheKey = MiseCacheKey.DevTools(workDir, configEnvironment, scope)
        return cache.getCachedWithProgress(cacheKey) {
            val commandLineArgs = mutableListOf("ls", scope.requireCommandFlag(), "--json")
            val executablePath = project.service<MiseExecutableManager>().getExecutablePath()

            val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
            miseCommandLine
                .runCommandLine<Map<String, List<MiseDevTool>>>(commandLineArgs)
                .map { devTools ->
                    devTools.mapKeys { (toolName, _) -> MiseDevToolName(toolName) }
                        .mapValues { (_, tools) ->
                            tools.onEach { tool ->
                                tool.miseExecutablePath = executablePath
                            }
                        }
                }
        }
    }

    internal fun mergeDevTools(
        local: Map<MiseDevToolName, List<MiseDevTool>>,
        global: Map<MiseDevToolName, List<MiseDevTool>>,
    ): Map<MiseDevToolName, List<MiseDevTool>> {
        if (local.isEmpty()) return global
        if (global.isEmpty()) return local

        val merged = global.toMutableMap()
        for ((toolName, tools) in local) {
            merged[toolName] = tools
        }
        return merged
    }

    // mise which
    fun getBinPath(
        commonBinName: String,
        project: Project,
        workDir: String = project.guessMiseProjectPath()
    ): Result<String> {
        val settings = project.service<MiseProjectSettings>().state
        val configEnvironment = settings.miseConfigEnvironment
        val cache = project.service<MiseCommandCache>()
        val cacheKey = MiseCacheKey.WhichBin(commonBinName, workDir, configEnvironment)
        return cache.getCachedWithProgress(cacheKey) {
            val executablePath = project.service<MiseExecutableManager>().getExecutablePath()
            val commandLineArgs = mutableListOf("which", commonBinName)
            val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
            miseCommandLine.runRawCommandLine(commandLineArgs).map { WslPathUtils.maybeConvertUnixPathToWsl(it.trim(), executablePath) }
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
    ): Result<List<String>> {
        val commandLineArgs = mutableListOf("config", "--tracked-configs")

        // Use the project's base path as the working directory to ensure correct mise context
        // (Windows mise for Windows projects, WSL mise for WSL projects)
        val workDir = project.guessMiseProjectPath()

        val miseCommandLine = MiseCommandLine(project, workDir, configEnvironment)
        return miseCommandLine
            .runRawCommandLine(commandLineArgs)
            .map { it.lines().map { line -> line.trim() }.filter { trimmed -> trimmed.isNotEmpty() } }
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
