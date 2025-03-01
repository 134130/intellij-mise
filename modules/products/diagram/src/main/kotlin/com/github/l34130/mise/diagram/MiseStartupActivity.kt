package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.model.psiLocation
import com.github.l34130.mise.core.toolwindow.nodes.MiseTaskNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class MiseStartupActivity :
    ProjectActivity,
    DumbAware {
    override suspend fun execute(project: Project) {
        MiseTaskNode.EP_NAME.add(
            object : MiseTaskNode.MiseTaskNodeActionsContributor {
                override fun contributeActions(task: MiseTask): List<AnAction> {
                    val psiLocation = task.psiLocation(project) ?: return emptyList()

                    return listOf(
                        object : ShowMiseTaskGraphDiagramAction() {
                            override fun update(e: AnActionEvent) {
                                super.update(e)
                                templatePresentation.icon = AllIcons.FileTypes.Diagram
                            }

                            override fun actionPerformed(e: AnActionEvent) {
                                val dataContext =
                                    SimpleDataContext
                                        .builder()
                                        .add(CommonDataKeys.PSI_ELEMENT, psiLocation.psiElement)
                                        .add(CommonDataKeys.PSI_FILE, psiLocation.psiElement.containingFile)
                                        .add(CommonDataKeys.PROJECT, project)
                                        .setParent(e.dataContext)
                                        .build()

                                val newEvent =
                                    AnActionEvent(
                                        e.inputEvent,
                                        dataContext,
                                        e.place,
                                        e.presentation,
                                        e.actionManager,
                                        e.modifiers,
                                    )
                                super.actionPerformed(newEvent)
                            }
                        },
                    )
                }
            },
        )
    }
}
