package com.github.l34130.mise.core.commands

import com.github.l34130.mise.core.settings.MiseSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView

class MiseRunAction(
    private val taskName: String,
) : AnAction(
        "Run with Mise",
        "Execute the command with Mise",
        AllIcons.Actions.Execute,
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        executeTask(project, taskName)
    }

    companion object {
        fun executeTask(
            project: Project,
            taskName: String,
        ) {
            val profile = project.service<MiseSettings>().state.miseProfile
            val shellTerminalWidget = project.service<TerminalView>().createLocalShellWidget(project.basePath, taskName)

            val profileArg = if (profile.isNotEmpty()) "--profile \"$profile\"" else ""
            val command = "mise run $profileArg '$taskName'"

            project
                .service<ToolWindowManager>()
                .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
                ?.show {
                    shellTerminalWidget.executeCommand(command)
                }
        }
    }
}
