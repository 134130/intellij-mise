package com.github.l34130.mise.toolwindow.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import java.awt.event.MouseEvent
import javax.swing.Icon

abstract class AbstractActionTreeNode(
    project: Project,
    value: String,
    private val icon: Icon?,
) : AbstractTreeNode<String>(project, value) {
    override fun update(presentation: PresentationData) {
        presentation.addText(value, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        icon?.let { presentation.setIcon(it) }
    }

    abstract fun onDoubleClick(event: MouseEvent)

    override fun getChildren(): Collection<AbstractTreeNode<*>> = emptyList()
}
