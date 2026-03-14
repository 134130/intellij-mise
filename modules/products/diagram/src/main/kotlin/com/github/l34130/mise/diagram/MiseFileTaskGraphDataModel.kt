package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.diagram.DiagramNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiManager

class MiseFileTaskGraphDataModel(
    project: Project,
    private val tomlFile: MiseTaskGraphableTomlFile,
    provider: MiseTaskGraphProvider,
) : AbstractMiseTaskGraphDataModel(project, provider) {
    override fun getModificationTracker(): ModificationTracker = PsiManager.getInstance(project).modificationTracker

    override fun getNodeName(node: DiagramNode<MiseTaskGraphable>): String =
        (node.identifyingElement as? MiseTaskGraphableTaskWrapper<*>)?.task?.name
            ?: super.getNodeName(node)

    override fun addElement(element: MiseTaskGraphable?): DiagramNode<MiseTaskGraphable>? = null

    override fun computeNodesBlocking(): List<MiseTaskGraphNode> =
        mutableListOf<MiseTaskGraphNode>()
            .apply {
                val myTasks =
                    com.intellij.openapi.application.runReadAction {
                        MiseTomlTableTask.resolveAllFromTomlFile(tomlFile.tomlFile)
                    }
                val tasks = project.service<MiseTaskResolver>().getCachedTasksOrEmptyList()

                for (myTask in myTasks) {
                    myTask.depends?.map { it.first() }?.let { depends ->
                        addAll(
                            tasks
                                .filter { task -> task.name in depends }
                                .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) },
                        )
                    }
                    myTask.waitFor?.map { it.first() }?.let { waitFor ->
                        addAll(
                            tasks
                                .filter { task -> task.name in waitFor }
                                .map { MiseTaskGraphNode(MiseTaskGraphableTaskWrapper(it), provider) },
                        )
                    }
                    myTask.dependsPost?.map { it.first() }?.let { dependsPost ->
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
}
