package com.github.l34130.mise.core.command

object MiseCommandLineHelper {
    // mise env
    fun getEnvVars(workDir: String?, configEnvironment: String?): Result<Map<String, String>> {
        val commandLineArgs = mutableListOf("env", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLine(commandLineArgs)
    }

    // mise ls
    fun getDevTools(workDir: String?, configEnvironment: String?): Result<Map<MiseDevToolName, List<MiseDevTool>>> {
        val commandLineArgs = mutableListOf("ls", "--current", "--json")

        val miseVersion = MiseCommandLine.getMiseVersion()

        // https://github.com/jdx/mise/commit/6e7e4074989bda47e40900cb651b694c72d39f4d
        val supportsOfflineFlag = miseVersion >= MiseVersion(2024, 11, 4)
        if (supportsOfflineFlag) {
            commandLineArgs.add("--offline")
        }

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLine<Map<String, List<MiseDevTool>>>(commandLineArgs)
            .map { devTools ->
                devTools.mapKeys { (toolName, _) -> MiseDevToolName(toolName) }
            }
    }

    // mise task ls
    fun getTasks(workDir: String?, configEnvironment: String?): Result<List<MiseTask>> {
        val commandLineArgs = mutableListOf("task", "ls", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLine(commandLineArgs)
    }
}
