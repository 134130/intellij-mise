package com.github.l34130.mise.deno

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.notifications.Notification
import com.intellij.deno.DenoConfigurable
import com.intellij.deno.DenoSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.io.FileUtil
import kotlin.io.path.Path

class ProjectDenoSetup :
    AnAction(),
    StartupActivity {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { runActivity(it) }
    }

    override fun runActivity(project: Project) {
        val denoSettings = project.service<DenoSettings>()
        val denoTools = MiseCmd.loadTools(project.basePath)["deno"] ?: return

        if (denoTools.size > 1) {
            Notification.notify("Multiple Deno SDKs found. Not setting any.", NotificationType.WARNING, project)
            return
        }

        val denoTool = denoTools.first()
        val absolutePathDenoDir =
            Path(FileUtil.expandUserHome(denoTool.installPath), "bin", "deno")
                .toAbsolutePath()
                .normalize()
                .toString()

        WriteAction.runAndWait<Throwable> {
            try {
                denoSettings.setDenoPath(absolutePathDenoDir)
                Notification.notifyWithLinkToSettings(
                    "Deno SDK set to deno@${denoTool.version} from ${denoTool.source.type}",
                    DenoConfigurable::class,
                    NotificationType.INFORMATION,
                    project,
                )
            } catch (e: Exception) {
                Notification.notify(
                    "Failed to set Deno SDK: ${e.message}",
                    NotificationType.ERROR,
                    project,
                )
            }
        }
    }
}
