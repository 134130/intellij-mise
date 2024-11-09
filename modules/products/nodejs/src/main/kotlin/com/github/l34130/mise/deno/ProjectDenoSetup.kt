package com.github.l34130.mise.deno

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.notifications.Notification
import com.intellij.deno.DenoSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ProjectDenoSetup :
    AnAction(),
    StartupActivity {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { runActivity(it) }
    }

    override fun runActivity(project: Project) {
        val denoSettings = project.service<DenoSettings>()
        val denoTools = MiseCmd.loadTools(project.basePath)["deno"] ?: return

        WriteAction.runAndWait<Throwable> {
            for (tool in denoTools) {
                tool.installPath

                if (denoTools.size == 1) {
                    denoSettings.setDenoPath(tool.installPath)
                    Notification.notify(
                        "Deno SDK set to ${tool.version} from ${tool.source.type}",
                        NotificationType.INFORMATION,
                        project,
                    )
                }
            }

            if (denoTools.size > 1) {
                Notification.notify("Multiple Deno SDKs found. Not setting any.", NotificationType.WARNING, project)
            }
        }
    }
}
