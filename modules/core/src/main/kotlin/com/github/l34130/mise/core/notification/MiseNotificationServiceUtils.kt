package com.github.l34130.mise.core.notification

import com.github.l34130.mise.core.command.MiseCommandLineException
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotTrustedConfigFileException
import com.intellij.notification.NotificationAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.concurrency.runAsync

object MiseNotificationServiceUtils {
    fun notifyException(
        title: String,
        throwable: Throwable,
        project: Project,
    ) {
        val notificationService = MiseNotificationService.getInstance(project)
        when (throwable) {
            is MiseCommandLineException -> {
                when (throwable) {
                    is MiseCommandLineNotTrustedConfigFileException -> {
                        notificationService.warn(
                            title,
                            """
                            Config file <code>${throwable.configFilePath}</code> is not trusted.
                            """.trimIndent(),
                        ) {
                            NotificationAction.createSimple(
                                "Trust the config file",
                            ) {
                                val absolutePath = FileUtil.expandUserHome(throwable.configFilePath)

                                runAsync {
                                    MiseCommandLineHelper
                                        .trustConfigFile(project, absolutePath)
                                        .onSuccess {
                                            notificationService.info(
                                                "Config file trusted",
                                                "Config file <code>${throwable.configFilePath}</code> is now trusted",
                                            )
                                        }.onFailure {
                                            notifyException("Failed to trust config file", it, project)
                                        }
                                }
                            }
                        }
                    }

                    else -> notificationService.warn(title, throwable.message)
                }
            }
            else -> notificationService.error(title, throwable.message ?: throwable.javaClass.simpleName)
        }
    }
}
