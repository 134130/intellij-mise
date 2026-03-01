package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.MiseTaskResolver
import com.intellij.diagram.DiagramNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiManager

class MiseFullTaskGraphDataModel(
    project: Project,
    provider: MiseTaskGraphProvider,
) : AbstractMiseTaskGraphDataModel(project, provider) {
    override fun getModificationTracker(): ModificationTracker = PsiManager.getInstance(project).modificationTracker

    override fun getNodeName(node: DiagramNode<MiseTaskGraphable>): String =
        (node.identifyingElement as? MiseTaskGraphableTaskWrapper<*>)?.task?.name
            ?: super.getNodeName(node)

    override fun addElement(element: MiseTaskGraphable?): DiagramNode<MiseTaskGraphable>? = null

    override fun computeNodesBlocking(): List<MiseTaskGraphNode> =
        project
            .service<MiseTaskResolver>()
            .getCachedTasksOrEmptyList()
            .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) }
}
