package com.github.l34130.mise.core.command

object MiseCommandLineHelper {
    fun getEnvVars(workDir: String?, profile: String?): Result<Map<String, String>> {
        val commandLineArgs = mutableListOf("mise", "env", "--json")

        if (!profile.isNullOrBlank()) {
            commandLineArgs.add("--profile")
            commandLineArgs.add("$profile")
        }

        val miseCommandLine = MiseCommandLine(workDir)
        return miseCommandLine.runCommandLine(commandLineArgs)
    }

    // mise ls
    fun getDevTools(workDir: String?, profile: String?): Result<Map<MiseDevToolName, List<MiseDevTool>>> {
        val miseVersion = getMiseVersion()

        val commandLineArgs = mutableListOf("mise", "ls", "--current", "--json")

        if (!profile.isNullOrBlank()) {
            commandLineArgs.add("--profile")
            commandLineArgs.add("$profile")
        }

        // https://github.com/jdx/mise/commit/6e7e4074989bda47e40900cb651b694c72d39f4d
        val supportsOfflineFlag = miseVersion >= MiseVersion(2024, 11, 4)
        if (supportsOfflineFlag) {
            commandLineArgs.add("--offline")
        }

        val miseCommandLine = MiseCommandLine(workDir)
        return miseCommandLine.runCommandLine<Map<String, List<MiseDevTool>>>(commandLineArgs)
            .map { devTools ->
                devTools.mapKeys { (toolName, _) -> MiseDevToolName(toolName) }
            }
    }

    // mise task ls
    fun getTasks(workDir: String?, profile: String?): Result<List<MiseTask>> {
        val commandLineArgs = mutableListOf("mise", "task", "ls", "--json")

        if (!profile.isNullOrBlank()) {
            commandLineArgs.add("--profile")
            commandLineArgs.add("$profile")
        }

        val miseCommandLine = MiseCommandLine(workDir)
        return miseCommandLine.runCommandLine(commandLineArgs)
    }

    private fun getMiseVersion(): MiseVersion {
        val miseCommandLine = MiseCommandLine()
        val versionString = miseCommandLine.runCommandLine<String>("mise", "version")

        val miseVersion = versionString.fold(
            onSuccess = {
                MiseVersion.parse(it)
            },
            onFailure = { exception ->
                MiseVersion(0, 0, 0)
            }
        )

        return miseVersion
    }
}
