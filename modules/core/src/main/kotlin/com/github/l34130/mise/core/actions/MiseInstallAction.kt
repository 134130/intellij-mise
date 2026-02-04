package com.github.l34130.mise.core.actions

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.icon.MiseIcons
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.util.RunWindowUtils
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class MiseInstallAction : DumbAwareAction(
    "Run Mise Install",
    "Run mise install for this project",
    MiseIcons.DEFAULT,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        RunWindowUtils.executeMiseCommand(
            project = project,
            args = listOf("install", "--raw", "--yes"),
            tabName = "Mise install",
            onSuccess = {
                project.service<MiseCacheService>().invalidateAllCommands()
                AbstractProjectSdkSetup.runAll(project, isUserInteraction = false)
            },
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
