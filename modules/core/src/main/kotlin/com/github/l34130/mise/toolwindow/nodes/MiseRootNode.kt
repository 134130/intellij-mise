package com.github.l34130.mise.toolwindow.nodes

import com.github.l34130.mise.settings.MiseSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project

class MiseRootNode(
    private val nodeProject: Project,
) : AbstractTreeNode<Any>(nodeProject, Object()) {
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val settings = MiseSettings.getService(nodeProject)

        return
    }
}
