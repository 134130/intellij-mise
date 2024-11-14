package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.notification.NotificationService
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

private val LOG = Logger.getInstance(MiseCommandLine::class.java)

class MiseCommandLine(
    private val project: Project,
    private val workDir: String? = null,
) {
    // `mise ls`
    fun loadDevTools(
        profile: String?,
        notify: Boolean = true,
    ): Map<MiseToolName, List<MiseTool>> {
        val versionString =
            runCommandLine<String>("mise", "version").getOrNull()

        val miseVersion =
            if (versionString == null) {
                MiseVersion(0, 0, 0)
            } else {
                MiseVersion.parse(versionString)
            }

        val commandLineArgs = mutableListOf("mise", "ls", "--current", "--json")

        profile?.let {
            commandLineArgs.add("--profile")
            commandLineArgs.add("'$it'")
        }

        // https://github.com/jdx/mise/commit/6e7e4074989bda47e40900cb651b694c72d39f4d
        val supportsOfflineFlag = miseVersion >= MiseVersion(2024, 11, 4)
        if (supportsOfflineFlag) {
            commandLineArgs.add("--offline")
        }

        return runCommandLine<Map<MiseToolName, List<MiseTool>>>(commandLineArgs).getOrElse { exception ->
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
        }
    }

    // `mise env`
    fun loadEnvironmentVariables(
        profile: String?,
        notify: Boolean = true,
    ): Map<String, String> {
        val commandLineArgs = mutableListOf("mise", "env", "--json")

        profile?.let {
            commandLineArgs.add("--profile")
            commandLineArgs.add("'$it'")
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

        profile?.let {
            commandLineArgs.add("--profile")
            commandLineArgs.add("'$it'")
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

    private fun <T> runCommandLine(commandLineArgs: List<String>): Result<T> = runCommandLine(*commandLineArgs.toTypedArray())

    private fun <T> runCommandLine(vararg commandLineArgs: String): Result<T> {
        val process =
            GeneralCommandLine(*commandLineArgs)
                .withWorkDirectory(workDir)
                .createProcess()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val stderr = process.errorReader().use { it.readText() }
            val parsedError = MiseCommandLineException.parseFromStderr(stderr)
            if (parsedError == null) {
                LOG.error("Failed to parse error. (exitCode=$exitCode, command=${commandLineArgs.joinToString { " " }})\n$stderr")
                return Result.failure(Throwable(stderr))
            } else {
                return Result.failure(parsedError)
            }
        }

        val stdout = process.inputStream.bufferedReader()

        val result =
            GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .fromJson<T>(stdout, object : TypeToken<T>() {}.type)

        return Result.success(result)
    }
}
