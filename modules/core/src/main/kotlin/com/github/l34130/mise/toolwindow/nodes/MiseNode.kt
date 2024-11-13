package com.github.l34130.mise.toolwindow.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

abstract class MiseNode<T : Any>(
    val nodeProject: Project,
    value: T,
    private val icon: Icon?,
) : AbstractTreeNode<T>(nodeProject, value) {
    override fun update(presentation: PresentationData) {
        presentation.setIcon(icon)
        presentation.addText(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
}
