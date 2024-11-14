package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.command.MiseTool
import com.github.l34130.mise.core.notification.Notification
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
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

    abstract fun getToolRequest(): MiseToolRequest

    abstract fun setupSdk(
        tool: MiseTool,
        project: Project,
    ): Boolean

    abstract fun <T : Configurable> getConfigurableClass(): KClass<out T>?

    private fun configureSdk(
        project: Project,
        isUserInteraction: Boolean,
    ) {
        val toolRequest = getToolRequest()
        val configurableClass = getConfigurableClass<Configurable>()

        val notify = { content: String, type: NotificationType ->
            if (configurableClass == null) {
                Notification.notify(content, type, project)
            } else {
                Notification.notifyWithLinkToSettings(
                    content = content,
                    configurableClass = configurableClass,
                    type = type,
                    project = project,
                )
            }
        }

        val profile = MiseSettings.getService(project).state.miseProfile
        val loadedTools =
            MiseCommandLine(
                project = project,
                workDir = project.basePath,
            ).loadDevTools(profile = profile)

        val tools = loadedTools[toolRequest.name]
        if (tools.isNullOrEmpty() || tools.size > 1) {
            if (!isUserInteraction) return

            val noOrMultiple =
                if (tools.isNullOrEmpty()) {
                    "No"
                } else {
                    "Multiple"
                }

            notify(
                """
                <b>$noOrMultiple tools configuration for ${toolRequest.canonicalName} found</b>
                
                Check your Mise configuration or configure it manually.
                """.trimIndent(),
                NotificationType.WARNING,
            )
            return
        }

        val tool = tools.first()

        if (!tool.installed) {
            notify(
                """
                <b>${toolRequest.name}@${tool.version} is not installed</b>
                
                Run `mise install` command to install the tool
                """.trimIndent(),
                NotificationType.WARNING,
            )
            return
        }

        WriteAction.runAndWait<Throwable> {
            try {
                val updated = setupSdk(tool, project)
                if (updated || isUserInteraction) {
                    notify(
                        """
                        <b>${toolRequest.canonicalName} configured to ${toolRequest.name}@${tool.version}</b>
                        
                        ${tool.source?.path?.let(FileUtil::getLocationRelativeToUserHome) ?: "unknown source"}
                        """.trimIndent(),
                        NotificationType.INFORMATION,
                    )
                }
            } catch (e: Exception) {
                notify(
                    """
                    Failed to set ${toolRequest.canonicalName} to ${tool.requestedVersion}: ${e.message}
                    """.trimIndent(),
                    NotificationType.ERROR,
                )
            }
        }
    }
}
