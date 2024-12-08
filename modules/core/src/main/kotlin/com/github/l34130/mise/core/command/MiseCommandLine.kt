package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.notification.NotificationService
import com.github.l34130.mise.core.util.fromJson
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

private val LOG = Logger.getInstance(MiseCommandLine::class.java)

class MiseCommandLine(
    private val project: Project,
    private val workDir: String? = project.basePath,
) {
    // `mise ls`
    fun loadDevTools(
        profile: String?,
        notify: Boolean = true,
    ): Map<MiseDevToolName, List<MiseDevTool>> {
        val versionString =
            runCommandLine<String>("mise", "version").getOrNull()

        val miseVersion =
            if (versionString == null) {
                MiseVersion(0, 0, 0)
            } else {
                MiseVersion.parse(versionString)
            }

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

        return runCommandLine<Map<String, List<MiseDevTool>>>(commandLineArgs)
            .getOrElse { exception ->
                if (!notify) {
                    return@getOrElse emptyMap()
                }

                val notificationService = project.service<NotificationService>()

                when (exception) {
                    is MiseCommandLineException -> {
                        notificationService.warn("Failed to load dev tools", exception.message)
                    }

                    else -> {
                        notificationService.error(
                            "Failed to load dev tools",
                            exception.message ?: exception.javaClass.simpleName,
                        )
                    }
                }

                emptyMap()
            }.mapKeys { (toolName, _) -> MiseDevToolName(toolName) }
    }

    // `mise env`
    fun loadEnvironmentVariables(
        profile: String?,
        notify: Boolean = true,
    ): Map<String, String> {
        val commandLineArgs = mutableListOf("mise", "env", "--json")

        if (!profile.isNullOrBlank()) {
            commandLineArgs.add("--profile")
            commandLineArgs.add("$profile")
        }

        return runCommandLine<Map<String, String>>(commandLineArgs).getOrElse { exception ->
            if (!notify) {
                return@getOrElse emptyMap()
            }

            val notificationService = project.service<NotificationService>()

            when (exception) {
                is MiseCommandLineException -> {
                    notificationService.warn("Failed to load environment variables", exception.message)
                }

                else -> {
                    notificationService.error(
                        "Failed to load environment variables",
                        exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }

            emptyMap()
        }
    }

    // `mise task ls`
    fun loadTasks(
        profile: String?,
        notify: Boolean = true,
    ): List<MiseTask> {
        val commandLineArgs = mutableListOf("mise", "task", "ls", "--json")

        if (!profile.isNullOrBlank()) {
            commandLineArgs.add("--profile")
            commandLineArgs.add("$profile")
        }

        return runCommandLine<List<MiseTask>>(commandLineArgs).getOrElse { exception ->
            if (!notify) {
                return@getOrElse emptyList()
            }

            val notificationService = project.service<NotificationService>()

            when (exception) {
                is MiseCommandLineException -> {
                    notificationService.warn("Failed to load tasks", exception.message)
                }

                else -> {
                    notificationService.error(
                        "Failed to load tasks",
                        exception.message ?: exception.javaClass.simpleName,
                    )
                }
            }

            emptyList()
        }
    }

    private inline fun <reified T> runCommandLine(vararg commandLineArgs: String): Result<T> = runCommandLine(commandLineArgs.toList())

    private inline fun <reified T> runCommandLine(commandLineArgs: List<String>): Result<T> {
        val generalCommandLine = GeneralCommandLine(commandLineArgs).withWorkDirectory(workDir)
        val processOutput = ExecUtil.execAndGetOutput(generalCommandLine, 1000)

        if (!processOutput.isExitCodeSet) {
            when {
                processOutput.isTimeout -> {
                    return Result.failure(Throwable("Command timed out. (command=$generalCommandLine)"))
                }

                processOutput.isCancelled -> {
                    return Result.failure(Throwable("Command was cancelled. (command=$generalCommandLine)"))
                }
            }
        }

        if (processOutput.exitCode != 0) {
            val stderr = processOutput.stderr
            val parsedError = MiseCommandLineException.parseFromStderr(generalCommandLine, stderr)
            if (parsedError == null) {
                LOG.error("Failed to parse error. (exitCode=${processOutput.exitCode}, command=$generalCommandLine)\n$stderr")
                return Result.failure(Throwable(stderr))
            } else {
                return Result.failure(parsedError)
            }
        }

        val result = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
            .fromJson<T>(processOutput.stdout)

        return Result.success(result)
    }
}
