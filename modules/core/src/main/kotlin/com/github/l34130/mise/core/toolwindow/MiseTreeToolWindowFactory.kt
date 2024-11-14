package com.github.l34130.mise.core.toolwindow

import com.github.l34130.mise.core.setting.MiseConfigurable
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.options.ShowSettingsUtil
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
                object : AnAction("Settings", "Open Mise settings", AllIcons.General.Settings) {
                    override fun actionPerformed(e: AnActionEvent) {
                        runInEdt {
                            ShowSettingsUtil
                                .getInstance()
                                .showSettingsDialog(project, MiseConfigurable::class.java)
                        }
                    }
                },
                object : AnAction("Refresh", "Refresh the tree", AllIcons.Actions.Refresh) {
                    override fun actionPerformed(e: AnActionEvent) {
                        runInEdt {
                            component.redrawContent()
                        }
                    }
                },
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
        toolWindow.component.size.width = toolWindow.component.size.width * 2
    }
}
