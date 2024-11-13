package com.github.l34130.mise.toolwindow.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

abstract class MiseNode<T : Any>(
    nodeProject: Project,
    value: T,
    private val icon: Icon?,
) : AbstractTreeNode<T>(nodeProject, value) {
    override fun update(presentation: PresentationData) {
        presentation.setIcon(icon)
        val attr =
            if (isActive()) {
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            } else {
                SimpleTextAttributes.GRAYED_ATTRIBUTES
            }

        presentation.addText(displayName(), attr)
    }

    open fun displayName(): String = value.toString()

    open fun isActive(): Boolean = true

    override fun toString(): String = displayName()
}
