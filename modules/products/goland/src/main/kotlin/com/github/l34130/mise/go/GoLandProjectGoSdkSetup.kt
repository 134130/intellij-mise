package com.github.l34130.mise.go

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.notifications.Notification
import com.goide.sdk.GoSdkImpl
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoSdkUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.LocalFileSystem

class GoLandProjectGoSdkSetup : AnAction(), StartupActivity {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { runActivity(it) }
    }

    override fun runActivity(project: Project) {
        val goTools = MiseCmd.loadTools(project.basePath)["go"] ?: return

        WriteAction.runAndWait<Throwable> {
            for (tool in goTools) {
                val localFileSystem = LocalFileSystem.getInstance()
                val homeDir = localFileSystem.findFileByPath(tool.installPath)
                val sdkRoot = GoSdkUtil.adjustSdkDir(homeDir) ?: continue

                val newSdk = GoSdkImpl(sdkRoot.url, tool.version, null)

                if (goTools.size == 1) {
                    GoSdkService.getInstance(project).setSdk(newSdk)
                    Notification.notify("Go SDK set to ${tool.version} from ${tool.source.type}", NotificationType.INFORMATION, project)
                }
            }

            if (goTools.size > 1) {
                Notification.notify("Multiple Go SDKs found. Not setting any.", NotificationType.WARNING, project)
            }
        }
    }
}
