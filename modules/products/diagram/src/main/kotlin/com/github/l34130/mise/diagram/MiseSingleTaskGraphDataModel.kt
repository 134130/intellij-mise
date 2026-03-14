package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.MiseTaskResolver
import com.intellij.diagram.DiagramNode
import com.intellij.openapi.components.service
import com.intellij.openapi.graph.GraphLayoutOrientation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiManager

class MiseSingleTaskGraphDataModel(
    project: Project,
    private val myTask: MiseTaskGraphableTaskWrapper<*>,
    provider: MiseTaskGraphProvider,
) : AbstractMiseTaskGraphDataModel(project, provider) {
    override fun getModificationTracker(): ModificationTracker = PsiManager.getInstance(project).modificationTracker

    override fun getNodeName(node: DiagramNode<MiseTaskGraphable>): String =
        (node.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name

    override fun addElement(element: MiseTaskGraphable?): DiagramNode<MiseTaskGraphable>? = null

    override fun computeNodesBlocking(): List<MiseTaskGraphNode> =
        sequence {
            val tasks = project.service<MiseTaskResolver>().getCachedTasksOrEmptyList()
            myTask.task.depends?.let { depends ->
                yieldAll(
                    tasks
                        .filter { task -> task.name in depends.map { it.first() } }
                        .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) },
                )
            }
            myTask.task.waitFor?.let { waitFor ->
                yieldAll(
                    tasks
                        .filter { task -> task.name in waitFor.map { it.first() } }
                        .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) },
                )
            }
            myTask.task.dependsPost?.let { dependsPost ->
                yieldAll(
                    tasks
                        .filter { task -> task.name in dependsPost.map { it.first() } }
                        .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) },
                )
            }

            yield(MiseTaskGraphNode(myTask, provider))
        }.toList()

    override fun configurePresentationBeforeRefresh() {
        builder.presentationModel.settings.currentLayoutOrientation = GraphLayoutOrientation.LEFT_TO_RIGHT
    }
}
