package com.github.l34130.mise.jdk

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.notifications.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.StartupActivity

class IdeaProjectJdkSetup : AnAction(), StartupActivity {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { runActivity(it) }
    }

    override fun runActivity(project: Project) {
        val javaTools = MiseCmd.loadTools(project.basePath)["java"] ?: return

        WriteAction.runAndWait<Throwable> {
            for (tool in javaTools) {
                val jdkName = "JDK of project ${project.name} (${tool.source.type})"
                val newJdk =
                    JavaSdk.getInstance().createJdk(
                        jdkName,
                        tool.installPath,
                        false,
                    )

                val oldJdk = ProjectJdkTable.getInstance().findJdk(jdkName)
                if (oldJdk != null) {
                    ProjectJdkTable.getInstance().updateJdk(oldJdk, newJdk)
                } else {
                    ProjectJdkTable.getInstance().addJdk(newJdk)
                }

                if (javaTools.size == 1 && oldJdk?.name != newJdk.name) {
                    ProjectRootManager.getInstance(project).projectSdk = newJdk
                    Notification.notify("JDK set to ${tool.version} from ${tool.source.type}", NotificationType.INFORMATION, project)
                }
            }

            if (javaTools.size > 1) {
                Notification.notify("Multiple JDKs found. Not setting any.", NotificationType.WARNING, project)
            }
        }
    }
}
