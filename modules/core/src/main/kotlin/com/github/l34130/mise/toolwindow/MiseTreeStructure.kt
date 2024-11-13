package com.github.l34130.mise.toolwindow

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.impl.AbstractProjectTreeStructure
import com.intellij.openapi.project.Project

class MiseTreeStructure(
    project: Project,
) : AbstractProjectTreeStructure(project) { // NOTE: AbstractTreeStructureBase(project) is original
    override fun getProviders(): List<TreeStructureProvider> = listOf(MiseTreeStructureProvider())

    override fun isToBuildChildrenInBackground(element: Any) = true
}
