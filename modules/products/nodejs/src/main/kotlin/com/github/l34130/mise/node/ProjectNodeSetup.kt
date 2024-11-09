package com.github.l34130.mise.node

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.notifications.Notification
import com.github.l34130.mise.utils.PathUtils
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.nio.file.Paths

class ProjectNodeSetup : AnAction(), StartupActivity {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { runActivity(it) }
    }

    private fun detectPackageManager(projectPath: String): String {
        val pnpmLockFile = File(projectPath, "pnpm-lock.yaml")
        val yarnLockFile = File(projectPath, "yarn.lock")

        return when {
            pnpmLockFile.exists() -> "pnpm"
            yarnLockFile.exists() -> "yarn"
            else -> "npm"
        }
    }


    private fun setupNodePackageManger(project: Project, packageManager: String) {
        try {
            val nodePackageRef = NodePackageRef.create(packageManager)
            NpmManager.getInstance(project)
                .packageRef = nodePackageRef
        } catch (e: Exception) {
            Notification.notify(
                "Failed to set package manager to $packageManager: ${e.message}",
                NotificationType.ERROR,
                project
            )
        }
    }

    private fun setupNodeManager(project: Project, nodePath: String, version: String, sourceType: String) {
        val packageManager = detectPackageManager(project.basePath ?: return)
        val nodeJsInterpreterManager = NodeJsInterpreterManager.getInstance(project)

        val currentPath = nodeJsInterpreterManager.interpreterRef.referenceName
        val newInterpreterRef = NodeJsInterpreterRef.create(NodeJsLocalInterpreter(nodePath))

        nodeJsInterpreterManager.setInterpreterRef(newInterpreterRef)
        setupNodePackageManger(project, packageManager)

        if (currentPath != nodePath) {
            Notification.notifyWithLinkToSettings(
                """
                Node SDK set to $packageManager@$version from $sourceType.
                Path: ${PathUtils.abbrHomeDir(nodePath)}
                """.trimIndent(),
                settingName = "Settings | Languages & Frameworks | Node.js",
                NotificationType.INFORMATION,
                project
            )
        }

    }

    override fun runActivity(project: Project) {
        val nodeTools = MiseCmd.loadTools(project.basePath)["node"] ?: return

        if (nodeTools.size > 1) {
            Notification.notify(
                "Multiple Node SDKs found. Not setting any.",
                NotificationType.WARNING,
                project
            )
            return
        }

        WriteAction.runAndWait<Throwable> {
            val tool = nodeTools.first()
            val absolutePathNodeDir = Paths.get(FileUtil.expandUserHome(tool.installPath), "bin", "node")
                .toAbsolutePath()
                .normalize()
                .toString();

            ApplicationManager.getApplication().runWriteAction {
                try {
                    setupNodeManager(project, absolutePathNodeDir, tool.version, tool.source.type)
                } catch (e: Exception) {
                    Notification.notify(
                        "Failed to set Node SDK: ${e.message}",
                        NotificationType.ERROR,
                        project
                    )
                }
            }
        }
    }
}
