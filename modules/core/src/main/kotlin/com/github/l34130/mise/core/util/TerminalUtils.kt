package com.github.l34130.mise.core.util

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

object TerminalUtils {
    fun executeCommand(
        project: Project,
        command: String,
        tabName: String? = null,
    ) {
        val shellWidget =
            project.service<TerminalToolWindowManager>().createShellWidget(
                project.guessMiseProjectPath(),
                tabName ?: "Mise",
                true,
                true,
            )

        val widget: ShellTerminalWidget? = ShellTerminalWidget.asShellJediTermWidget(shellWidget)
        val executeCommand: (command: String) -> Unit =
            if (widget != null) {
                widget::executeCommand
            } else {
                { command: String ->
                    runInEdt { shellWidget.sendCommandToExecute(command) }
                }
            }

        val terminalToolWindow =
            project
                .service<ToolWindowManager>()
                .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)

        if (terminalToolWindow == null) {
            executeCommand(command)
        } else {
            terminalToolWindow
                .show { executeCommand(command) }
        }
    }
}
