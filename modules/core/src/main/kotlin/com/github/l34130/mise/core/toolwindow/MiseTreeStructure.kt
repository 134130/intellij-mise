package com.github.l34130.mise.core.toolwindow

import com.github.l34130.mise.core.toolwindow.nodes.MiseRootNode
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.project.Project

class MiseTreeStructure(
    project: Project,
) : AbstractTreeStructureBase(project) {
    override fun getProviders(): List<TreeStructureProvider> = listOf(defaultTreeStructureProvider)

    override fun getRootElement(): Any = MiseRootNode(myProject)

    override fun commit() {
    }

    override fun hasSomethingToCommit(): Boolean = false

    override fun isToBuildChildrenInBackground(element: Any) = true

    companion object {
        val defaultTreeStructureProvider = MiseTreeStructureProvider()
    }
}
