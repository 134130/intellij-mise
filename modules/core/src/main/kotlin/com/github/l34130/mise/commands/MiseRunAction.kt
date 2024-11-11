package com.github.l34130.mise.commands

import com.github.l34130.mise.settings.MiseSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalView

class MiseRunAction(private val taskName: String) : AnAction(
    "Run with Mise",
    "Execute the command with Mise",
    AllIcons.Actions.Execute
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        executeTask(project, taskName)
    }

    companion object {
        fun executeTask(project: Project, taskName: String) {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val terminalToolWindow = toolWindowManager.getToolWindow("Terminal")

            terminalToolWindow?.show {
                val profile = MiseSettings.getService(project).state.miseProfile
                val profileArg = if (profile.isNotEmpty()) "--profile \"$profile\"" else ""
                val command = "mise run $profileArg \"${taskName}\""

                val terminal = TerminalView.getInstance(project)
                val widget = terminal.createLocalShellWidget(project.basePath ?: "", null)
                widget.executeCommand(command)
            }
        }
    }
}