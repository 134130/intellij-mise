package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.MiseProjectService
import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramEdge
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
) : DiagramDataModel<MiseTaskGraphable>(project, provider) {
    private var nodes: List<MiseTaskGraphNode> = loadNodes()

    override fun getModificationTracker(): ModificationTracker = PsiManager.getInstance(project).modificationTracker

    override fun getNodes(): Collection<DiagramNode<MiseTaskGraphable>?> = nodes

    override fun getNodeName(node: DiagramNode<MiseTaskGraphable>): String =
        (node.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name

    override fun addElement(element: MiseTaskGraphable?): DiagramNode<MiseTaskGraphable>? = null

    override fun getEdges(): Collection<DiagramEdge<MiseTaskGraphable>> {
        val result = mutableListOf<MiseTaskGraphEdge>()

        for (node in nodes) {
            val task = (node.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task

            for (item in task.depends?.map { it.first() } ?: emptyList()) {
                val target = nodes.find { (it.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name == item } ?: continue
                result.add(MiseTaskGraphEdge(target, node))
            }
            for (item in task.waitFor?.map { it.first() } ?: emptyList()) {
                val target = nodes.find { (it.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name == item } ?: continue
                result.add(MiseTaskGraphEdge(target, node))
            }
            for (item in task.dependsPost?.map { it.first() } ?: emptyList()) {
                val target = nodes.find { (it.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name == item } ?: continue
                result.add(MiseTaskGraphEdge(node, target))
            }
        }

        return result
    }

    override fun refreshDataModel() {
        nodes = loadNodes()
        builder.presentationModel.settings.currentLayoutOrientation = GraphLayoutOrientation.LEFT_TO_RIGHT
        builder
            .queryUpdate()
            .withRelayout()
            .withDataReload()
            .withPresentationUpdate()
            .run()
    }

    override fun dispose() {
    }

    private fun loadNodes(): List<MiseTaskGraphNode> =
        sequence {
            val tasks = project.service<MiseProjectService>().getTasks()
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
}
