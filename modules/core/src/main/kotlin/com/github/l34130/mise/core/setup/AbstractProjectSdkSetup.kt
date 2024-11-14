package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.NotificationService
import com.github.l34130.mise.core.setting.MiseSettings
import com.github.l34130.mise.core.util.TerminalUtils
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.io.FileUtil
import kotlin.reflect.KClass

abstract class AbstractProjectSdkSetup :
    DumbAwareAction(),
    StartupActivity.DumbAware {
    final override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { configureSdk(it, true) }
    }

    final override fun runActivity(project: Project) {
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
        val devToolName = getDevToolName()
        val notificationService = project.service<NotificationService>()

        val profile = MiseSettings.getService(project).state.miseProfile
        val tools =
            MiseCommandLine(project).loadDevTools(profile = profile)[devToolName]

        if (tools.isNullOrEmpty() || tools.size > 1) {
            if (!isUserInteraction) return

            val noOrMultiple =
                if (tools.isNullOrEmpty()) {
                    "No"
                } else {
                    "Multiple"
                }

            notificationService.warn(
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

            return
        }

        val tool = tools.first()

        if (!tool.installed) {
            notificationService.warn(
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
            return
        }

        WriteAction.runAndWait<Throwable> {
            try {
                val updated = setupSdk(tool, project)
                if (updated || isUserInteraction) {
                    notificationService.info(
                        "${devToolName.canonicalName()} configured to $devToolName@${tool.version}",
                        tool.source?.absolutePath?.let(FileUtil::getLocationRelativeToUserHome) ?: "unknown source",
                    )
                }
            } catch (e: Exception) {
                notificationService.error(
                    "Failed to set ${devToolName.canonicalName()} to $devToolName@${tool.version}",
                    e.message ?: e.javaClass.simpleName,
                )
            }
        }
    }
}
