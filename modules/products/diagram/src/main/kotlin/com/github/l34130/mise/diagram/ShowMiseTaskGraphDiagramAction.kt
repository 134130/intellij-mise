package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.icon.MiseIcons
import com.intellij.diagram.DiagramProvider
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.uml.core.actions.ShowDiagram

class ShowMiseTaskGraphDiagramAction : ShowDiagram() {
    override fun getForcedProvider(): DiagramProvider<*>? = DiagramProvider.findByID<MiseTaskGraphProvider>(MiseTaskGraphProvider.ID)

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
        e.presentation.text = "Show Mise Task Graph Diagram"
        e.presentation.description = "Mise task graph dependency diagram"
        e.presentation.icon = MiseIcons.DEFAULT
    }
}
