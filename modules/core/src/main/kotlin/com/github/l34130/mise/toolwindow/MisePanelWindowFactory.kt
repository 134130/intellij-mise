package com.github.l34130.mise.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class MisePanelWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val misePanel = MisePanel(project)

        toolWindow.setTitleActions(
            listOf(
                SettingsAction(project),
                RefreshAction(misePanel)
            )
        )

        val content = ContentFactory.getInstance().createContent(misePanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
