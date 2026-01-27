package com.github.l34130.mise.core.notification

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.l34130.mise.core.command.MiseCommandLineException
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotTrustedConfigFileException
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.wsl.WslPathUtils.resolveUserHomeAbbreviations
import com.intellij.notification.NotificationAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectForFile
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.concurrency.runAsync
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object MiseNotificationServiceUtils {
    private val logger = logger<MiseNotificationServiceUtils>()
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
                        val debounceKey = "untrusted:${throwable.configFilePath}"
                        logger.debug("==> [DEBOUNCE] Checking debounce for key: $debounceKey")
                        if (debounceMap.getIfPresent(debounceKey) != null) {
                            logger.debug("==> [DEBOUNCE] Suppressed duplicate notification for: ${throwable.configFilePath}")
                            return
                        }
                        logger.debug("==> [DEBOUNCE] Showing notification and caching key: $debounceKey")
                        debounceMap.put(debounceKey, Unit)



                        notificationService.warn(
                            "Config file is not trusted.",
                            """
                            Trust the file <code>${FileUtil.getLocationRelativeToUserHome(throwable.configFilePath)}</code>
                            """.trimIndent(),
                        ) {
                            NotificationAction.createSimple(
                                "`mise trust`",
                            ) {
                                logger.debug("Trust action triggered for: ${throwable.configFilePath}")
                                runAsync {
                                    val vfsWorkingDir = VirtualFileManager.getInstance().findFileByNioPath(throwable.generalCommandLine.workDirectory.toPath())
                                    val guessedProjectCloseEnoughForUserHome = vfsWorkingDir?.let { guessProjectForFile(it) } ?: project
                                    // Returns a full wsl path if the file is on WSL, which is then handled appropriately by the trust command.
                                    val absolutePath = resolveUserHomeAbbreviations(throwable.configFilePath, guessedProjectCloseEnoughForUserHome).toString()
                                    val vf = VirtualFileManager.getInstance().findFileByUrl("file://$absolutePath")
                                    val guessedProject = vf?.let { guessProjectForFile(it) } ?: project
                                    // Get the config environment from the project settings
                                    val configEnvironment = guessedProject.service<MiseProjectSettings>().state.miseConfigEnvironment

                                    MiseCommandLineHelper
                                        .trustConfigFile(guessedProject, absolutePath, configEnvironment)
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
