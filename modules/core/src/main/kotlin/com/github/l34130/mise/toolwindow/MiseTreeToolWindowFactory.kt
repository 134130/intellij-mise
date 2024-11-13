package com.github.l34130.mise.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class MiseTreeToolWindowFactory :
    ToolWindowFactory,
    DumbAware {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val contentManager = toolWindow.contentManager

        val component = MiseTreeToolWindow(MiseTreeStructure(project))
        toolWindow.setTitleActions(
            listOf(
                SettingsAction(project),
                // RefreshAction
            ),
        )

        val content =
            contentManager.factory.createContent(component, null, false).also {
                it.isCloseable = true
                it.isPinnable = true
            }
        contentManager.addContent(content)
        toolWindow.activate(null)
        contentManager.setSelectedContent(content)

        // subscribe states
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = "Mise"
    }
}
