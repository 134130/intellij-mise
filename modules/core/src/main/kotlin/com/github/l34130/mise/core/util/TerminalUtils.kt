package com.github.l34130.mise.core.util

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView

object TerminalUtils {
    fun executeCommand(
        project: Project,
        command: String,
        tabName: String? = null,
    ) {
        val widget = project.service<TerminalView>().createLocalShellWidget(project.basePath, tabName ?: "Mise")

        project
            .service<ToolWindowManager>()
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
            ?.show {
                widget.executeCommand(command)
            }
    }
}
