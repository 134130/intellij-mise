package com.github.l34130.mise.core.notification

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.l34130.mise.core.command.MiseCommandLineException
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotTrustedConfigFileException
import com.intellij.notification.NotificationAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.concurrency.runAsync
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object MiseNotificationServiceUtils {
    private val debounceMap: Cache<String, Any> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(5.seconds.toJavaDuration())
            .build()

    fun notifyException(
        title: String,
        throwable: Throwable,
        project: Project,
    ) {
        val notificationService = MiseNotificationService.getInstance(project)
        when (throwable) {
            // TODO: Handle other exceptions (e.g. MiseCommandLineNotFoundException)
            is MiseCommandLineException -> {
                when (throwable) {
                    is MiseCommandLineNotTrustedConfigFileException -> {
                        if (debounceMap.getIfPresent(MiseCommandLineNotTrustedConfigFileException::class.simpleName!!) != null) {
                            // Debounce duplicate notifications
                            return
                        }

                        notificationService.warn(
                            "Config file is not trusted.",
                            """
                            Trust the file <code>${FileUtil.getLocationRelativeToUserHome(throwable.configFilePath)}</code>
                            """.trimIndent(),
                        ) {
                            NotificationAction.createSimple(
                                "`mise trust`",
                            ) {
                                val absolutePath = FileUtil.expandUserHome(throwable.configFilePath)

                                runAsync {
                                    MiseCommandLineHelper
                                        .trustConfigFile(absolutePath)
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

                    else -> {
                        notificationService.warn(title, throwable.message)
                    }
                }
            }

            else -> {
                notificationService.error(title, throwable.message ?: throwable.javaClass.simpleName)
            }
        }
    }
}
