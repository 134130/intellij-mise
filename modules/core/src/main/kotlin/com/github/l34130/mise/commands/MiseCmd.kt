package com.github.l34130.mise.commands

import com.github.l34130.mise.notifications.Notification
import com.github.l34130.mise.utils.fromJson
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.use
import org.jetbrains.plugins.terminal.TerminalView

object MiseCmd {
    fun loadEnv(
        workDir: String?,
        miseProfile: String,
        project: Project,
    ): Map<String, String> {
        try {
            val cliResult = runCommandLine(listOf("mise", "env", "--profile", miseProfile), workDir)

            if (cliResult.isFailure) {
                Notification.notify(
                    "Failed to import Mise environment: ${cliResult.exceptionOrNull() ?: "Unknown exception"}",
                    NotificationType.ERROR,
                )
                return emptyMap()
            }

            return cliResult
                .getOrThrow()
                .split("\n")
                .map { it.removePrefix("export ") }
                .filter { it.isNotBlank() }
                .map { it.split("=", limit = 2) }
                .associate { it[0] to it[1] }
        } catch (e: MiseCmdException) {
            handleMiseCmdException(project, e)
            return emptyMap()
        } catch (e: Exception) {
            Notification.notify("Failed to import Mise environment: ${e.message}", NotificationType.ERROR)
            return emptyMap()
        }
    }

    fun loadTasks(
        workDir: String?,
        miseProfile: String,
        project: Project,
        notify: Boolean = true,
    ): List<MiseTask> {
        try {
            val cliResult = runCommandLine(listOf("mise", "tasks", "ls", "--json", "--profile", miseProfile), workDir)

            if (cliResult.isFailure) {
                if (notify) {
                    Notification.notify(
                        "Failed to import Mise tasks: ${cliResult.exceptionOrNull() ?: "Unknown exception"}",
                        NotificationType.WARNING,
                    )
                }
                return emptyList()
            }

            return GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .fromJson<List<MiseTask>>(cliResult.getOrThrow())
        } catch (e: MiseCmdException) {
            handleMiseCmdException(project, e)
            return emptyList()
        } catch (e: Exception) {
            if (notify) {
                Notification.notify("Failed to import Mise tasks: ${e.message}", NotificationType.ERROR)
            }
            return emptyList()
        }
    }

    fun loadTools(
        workDir: String?,
        miseProfile: String,
        project: Project,
    ): Map<String, List<MiseTool>> {
        try {
            val miseVersionStr =
                runCommandLine(listOf("mise", "version"), workDir).getOrNull()

            val miseVersion =
                if (miseVersionStr == null) {
                    MiseVersion(0, 0, 0)
                } else {
                    MiseVersion.from(miseVersionStr)
                }

            // https://github.com/jdx/mise/commit/6e7e4074989bda47e40900cb651b694c72d39f4d
            val supportsOfflineFlag = miseVersion >= MiseVersion(2024, 11, 4)
            val commandLineArgs = listOf("mise", "ls", "--current", "--json", "--profile", miseProfile)

            val cliResult =
                if (supportsOfflineFlag) {
                    runCommandLine(commandLineArgs + "--offline", workDir)
                } else {
                    runCommandLine(commandLineArgs, workDir)
                }

            if (cliResult.isFailure) {
                Notification.notify(
                    "Failed to import Mise tools: ${cliResult.exceptionOrNull() ?: "Unknown exception"}",
                    NotificationType.WARNING,
                )
                return emptyMap()
            }

            return GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .fromJson<Map<String, List<MiseTool>>>(cliResult.getOrThrow())
        } catch (e: MiseCmdException) {
            handleMiseCmdException(project, e)
            return emptyMap()
        } catch (e: Exception) {
            Notification.notify("Failed to import Mise tools: ${e.message}", NotificationType.ERROR)
            return emptyMap()
        }
    }

    private fun runCommandLine(
        commandLineArgs: List<String>,
        workDir: String?,
    ): Result<String> {
        val process =
            GeneralCommandLine(commandLineArgs)
                .withWorkDirectory(workDir)
                .createProcess()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val stderr = process.errorReader().use { it.readText() }
            throw MiseCmdException.parseFromStderr(stderr) ?: RuntimeException(stderr)
        }

        return Result.success(process.inputReader().use { it.readText() })
    }

    private fun handleMiseCmdException(
        project: Project,
        e: MiseCmdException,
    ) {
        when (e) {
            is MiseCmdNotTrustedConfigFileException -> {
                Notification.notifyWithAction(
                    content =
                        """
                        <b>Mise configuration file is not trusted.</b>
                        ${e.configFilePath}
                        """.trimIndent(),
                    type = NotificationType.WARNING,
                    actionName = "Trust the config file",
                    project = project,
                ) {
                    val widget =
                        project
                            .service<TerminalView>()
                            .createLocalShellWidget(
                                project.basePath,
                                "mise trust",
                            )
                    widget.executeCommand("mise trust '${FileUtil.expandUserHome(e.configFilePath)}'")
                }
            }

            is MiseCmdErrorParsingConfigFileException -> {
                Notification.notify(
                    content =
                        """
                        <b>Failed to parse Mise configuration file</b>
                        ${e.configFilePath}
                        """.trimIndent(),
                    type = NotificationType.ERROR,
                    project = project,
                )
            }
        }
    }
}
