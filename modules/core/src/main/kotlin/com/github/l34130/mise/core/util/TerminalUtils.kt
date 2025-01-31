package com.github.l34130.mise.core.util

import com.github.l34130.mise.core.baseDirectory
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
        val shellWidget = project.service<TerminalToolWindowManager>().createShellWidget(project.baseDirectory(), tabName ?: "Mise", true, true)
        val widget = ShellTerminalWidget.toShellJediTermWidgetOrThrow(shellWidget)

        project
            .service<ToolWindowManager>()
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
            ?.show {
                widget.executeCommand(command)
            }
    }
}
