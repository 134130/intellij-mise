package com.github.l34130.mise.core.command

import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

object MiseCommandLineHelper {
    // mise env
    @RequiresBackgroundThread
    fun getEnvVars(
        workDir: String?,
        configEnvironment: String?,
    ): Result<Map<String, String>> {
        val commandLineArgs = mutableListOf("env", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLine(commandLineArgs)
    }

    // mise env
    @RequiresBackgroundThread
    fun getEnvVarsExtended(
        workDir: String?,
        configEnvironment: String?,
    ): Result<Map<String, MiseEnvExtended>> {
        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)

        val envs =
            miseCommandLine
                .runCommandLine<Map<String, MiseEnv>>(listOf("env", "--json-extended"))
                .getOrElse { return Result.failure(it) }

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

        return Result.success(extendedEnvs)
    }

    suspend fun getEnvVarsAsync(
        workDir: String?,
        configEnvironment: String?,
    ): Result<Map<String, String>> {
        val commandLineArgs = mutableListOf("env", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLineAsync(commandLineArgs)
    }

    // mise ls
    @RequiresBackgroundThread
    fun getDevTools(
        workDir: String?,
        configEnvironment: String?,
    ): Result<Map<MiseDevToolName, List<MiseDevTool>>> {
        val commandLineArgs = mutableListOf("ls", "--local", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine
            .runCommandLine<Map<String, List<MiseDevTool>>>(commandLineArgs)
            .map { devTools ->
                devTools.mapKeys { (toolName, _) -> MiseDevToolName(toolName) }
            }
    }

    // mise task ls
    @RequiresBackgroundThread
    fun getTasks(
        workDir: String?,
        configEnvironment: String?,
    ): Result<List<MiseTask>> {
        val commandLineArgs = mutableListOf("task", "ls", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLine(commandLineArgs)
    }

    // mise config get
    @RequiresBackgroundThread
    fun getConfig(
        workDir: String?,
        configEnvironment: String?,
        key: String,
    ): Result<String> {
        val commandLineArgs = mutableListOf("config", "get", key)

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runRawCommandLine(commandLineArgs)
    }

    // mise config --tracked-configs
    @RequiresBackgroundThread
    fun getTrackedConfigs(): Result<List<String>> {
        val commandLineArgs = mutableListOf("config", "--tracked-configs")
        val miseCommandLine = MiseCommandLine(null, null)
        return miseCommandLine
            .runRawCommandLine(commandLineArgs)
            .map { it.lines().map { it.trim() }.filter { it.isNotEmpty() } }
    }

    // mise exec
    @RequiresBackgroundThread
    fun executeCommand(
        workDir: String?,
        configEnvironment: String?,
        command: List<String>,
    ): Result<String> {
        val commandLineArgs = mutableListOf("exec", "--") + command

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runRawCommandLine(commandLineArgs)
    }

    // mise trust
    @RequiresBackgroundThread
    fun trustConfigFile(configFilePath: String): Result<Unit> {
        val commandLineArgs = mutableListOf("trust", configFilePath)

        val miseCommandLine = MiseCommandLine(null, null)
        return miseCommandLine.runCommandLine(commandLineArgs)
    }
}
