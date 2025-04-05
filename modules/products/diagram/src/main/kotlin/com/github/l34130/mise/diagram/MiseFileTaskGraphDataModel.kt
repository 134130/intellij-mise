package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.MiseProjectService
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramEdge
import com.intellij.diagram.DiagramNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiManager

class MiseFileTaskGraphDataModel(
    project: Project,
    tomlFile: MiseTaskGraphableTomlFile,
    provider: MiseTaskGraphProvider,
) : DiagramDataModel<MiseTaskGraphable>(project, provider) {
    private val nodes: List<MiseTaskGraphNode> =
        mutableListOf<MiseTaskGraphNode>()
            .apply {
                val myTasks = MiseTomlTableTask.resolveAllFromTomlFile(tomlFile.tomlFile)
                val tasks = project.service<MiseProjectService>().getTasks()

                for (myTask in myTasks) {
                    myTask.depends?.let { depends ->
                        addAll(
                            tasks
                                .filter { task -> task.name in depends }
                                .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) },
                        )
                    }
                    myTask.waitFor?.let { waitFor ->
                        addAll(
                            tasks
                                .filter { task -> task.name in waitFor }
                                .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) },
                        )
                    }
                    myTask.dependsPost?.let { dependsPost ->
                        addAll(
                            tasks
                                .filter { task -> task.name in dependsPost }
                                .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) },
                        )
                    }
                }

                addAll(
                    myTasks.map { myTask ->
                        MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(myTask), provider)
                    },
                )
            }.distinctBy { (it.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name }

    override fun getModificationTracker(): ModificationTracker = PsiManager.getInstance(project).modificationTracker

    override fun getNodes(): Collection<DiagramNode<MiseTaskGraphable>?> = nodes

    override fun getNodeName(node: DiagramNode<MiseTaskGraphable>): String = "" // node.identifyingElement.name

    override fun addElement(element: MiseTaskGraphable?): DiagramNode<MiseTaskGraphable>? = null

    override fun getEdges(): Collection<DiagramEdge<MiseTaskGraphable>> {
        val result = mutableListOf<MiseTaskGraphEdge>()

        for (node in nodes) {
            val task = (node.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task

            for (item in task.depends ?: emptyList()) {
                val target = nodes.find { (it.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name == item } ?: continue
                result.add(MiseTaskGraphEdge(target, node))
            }
            for (item in task.waitFor ?: emptyList()) {
                val target = nodes.find { (it.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name == item } ?: continue
                result.add(MiseTaskGraphEdge(target, node))
            }
            for (item in task.dependsPost ?: emptyList()) {
                val target = nodes.find { (it.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name == item } ?: continue
                result.add(MiseTaskGraphEdge(node, target))
            }
        }

        return result
    }

    override fun dispose() {
    }
}
