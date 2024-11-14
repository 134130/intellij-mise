package com.github.l34130.mise.core.toolwindow

import com.github.l34130.mise.core.toolwindow.nodes.MiseRootNode
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode

class MiseTreeStructureProvider : TreeStructureProvider {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings?,
    ): MutableCollection<AbstractTreeNode<*>> {
        if (parent !is MiseRootNode) {
            return children
        }

        return children
    }
}
