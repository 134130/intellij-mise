package com.github.l34130.mise.core.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.impl.ContentImpl
import javax.swing.JLabel
import javax.swing.SwingConstants

class MiseTreeToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        toolWindow.title = "Mise"
        val contentManager = toolWindow.contentManager

        val initializingLabel = JLabel("Initializing Mise", SwingConstants.CENTER).also {
            it.isOpaque = true
        }
        contentManager.addContent(ContentImpl(initializingLabel, "", false))

        val component = MiseTreeToolWindow(project, MiseTreeStructure(project))

        val content =
            contentManager.factory.createContent(component, null, false).also {
                it.isCloseable = true
                it.isPinnable = true
            }

        contentManager.removeAllContents(true)
        contentManager.addContent(content)

        toolWindow.activate(null)
        contentManager.setSelectedContent(content)

        // subscribe states
    }
}
