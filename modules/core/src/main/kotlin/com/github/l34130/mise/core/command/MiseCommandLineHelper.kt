package com.github.l34130.mise.core.command

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

object MiseCommandLineHelper {
    // mise env
    @RequiresBackgroundThread
    fun getEnvVars(
        project: Project,
        workDir: String?,
        configEnvironment: String?,
    ): Result<Map<String, String>> {
        val commandLineArgs = mutableListOf("env", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLine(project, commandLineArgs)
    }

    // mise ls
    @RequiresBackgroundThread
    fun getDevTools(
        project: Project,
        workDir: String?,
        configEnvironment: String?,
    ): Result<Map<MiseDevToolName, List<MiseDevTool>>> {
        val commandLineArgs = mutableListOf("ls", "--current", "--json")

        val miseVersion = MiseCommandLine.getMiseVersion(project)

        // https://github.com/jdx/mise/commit/6e7e4074989bda47e40900cb651b694c72d39f4d
        val supportsOfflineFlag = miseVersion >= MiseVersion(2024, 11, 4)
        if (supportsOfflineFlag) {
            commandLineArgs.add("--offline")
        }

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine
            .runCommandLine<Map<String, List<MiseDevTool>>>(project, commandLineArgs)
            .map { devTools ->
                devTools.mapKeys { (toolName, _) -> MiseDevToolName(toolName) }
            }
    }

    // mise task ls
    @RequiresBackgroundThread
    fun getTasks(
        project: Project,
        workDir: String?,
        configEnvironment: String?,
    ): Result<List<MiseTask>> {
        val commandLineArgs = mutableListOf("task", "ls", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLine(project, commandLineArgs)
    }

    // mise trust
    @RequiresBackgroundThread
    fun trustConfigFile(
        project: Project,
        configFilePath: String,
    ): Result<Unit> {
        val commandLineArgs = mutableListOf("trust", configFilePath)

        val miseCommandLine = MiseCommandLine(null, null)
        return miseCommandLine.runCommandLine(project, commandLineArgs)
    }
}
