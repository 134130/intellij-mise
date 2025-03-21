package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.setting.MiseSettings
import com.github.l34130.mise.core.util.TerminalUtils
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.io.FileUtil
import kotlin.reflect.KClass

abstract class AbstractProjectSdkSetup :
    DumbAwareAction(),
    ProjectActivity,
    DumbAware {
    final override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { configureSdk(it, true) }
    }

    override suspend fun execute(project: Project) {
        configureSdk(project, false)
    }

    abstract fun getDevToolName(): MiseDevToolName

    abstract fun setupSdk(
        tool: MiseDevTool,
        project: Project,
    ): Boolean

    abstract fun <T : Configurable> getConfigurableClass(): KClass<out T>?

    private fun configureSdk(
        project: Project,
        isUserInteraction: Boolean,
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val devToolName = getDevToolName()
            val miseNotificationService = project.service<MiseNotificationService>()

            val configEnvironment = project.service<MiseSettings>().state.miseConfigEnvironment
            val toolsResult =
                MiseCommandLineHelper.getDevTools(project, workDir = project.basePath, configEnvironment = configEnvironment)
            val tools =
                toolsResult.fold(
                    onSuccess = { tools -> tools[devToolName] },
                    onFailure = {
                        if (it !is MiseCommandLineNotFoundException) {
                            MiseNotificationServiceUtils.notifyException("Failed to load dev tools", it, project)
                        }
                        emptyList()
                    },
                )

            if (tools.isNullOrEmpty() || tools.size > 1) {
                if (!isUserInteraction) return@executeOnPooledThread

                val noOrMultiple =
                    if (tools.isNullOrEmpty()) {
                        "No"
                    } else {
                        "Multiple"
                    }

                miseNotificationService.warn(
                    "$noOrMultiple dev tools configuration for ${devToolName.canonicalName()} found",
                    "Check your Mise configuration or configure it manually",
                ) {
                    NotificationAction.createSimple("Configure") {
                        val configurableClass = getConfigurableClass<Configurable>()
                        if (configurableClass != null) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableClass.javaObjectType)
                        } else {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project)
                        }
                    }
                }

                return@executeOnPooledThread
            }

            val tool = tools.first()

            if (!tool.installed) {
                miseNotificationService.warn(
                    "$devToolName@${tool.version} is not installed",
                    "Run `mise install` command to install the tool",
                ) {
                    NotificationAction.createSimple("Run `mise install`") {
                        TerminalUtils.executeCommand(
                            project = project,
                            command = "mise install",
                            tabName = "mise install",
                        )
                    }
                }
                return@executeOnPooledThread
            }

            WriteAction.runAndWait<Throwable> {
                try {
                    val updated = setupSdk(tool, project)
                    if (updated || isUserInteraction) {
                        miseNotificationService.info(
                            "${devToolName.canonicalName()} configured to ${devToolName.value}@${tool.version}",
                            tool.source?.absolutePath?.let(FileUtil::getLocationRelativeToUserHome) ?: "unknown source",
                        )
                    }
                } catch (e: Exception) {
                    miseNotificationService.error(
                        "Failed to set ${devToolName.canonicalName()} to ${devToolName.value}@${tool.version}",
                        e.message ?: e.javaClass.simpleName,
                    )
                }
            }
        }
    }
}
