package com.github.l34130.mise.core.toolwindow.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes

class MiseEmptyNode(
    project: Project,
    value: String = "No data",
) : MiseNode<String>(project, value, null) {
    override fun getChildren(): Collection<MiseNode<*>> = emptyList()

    override fun update(presentation: PresentationData) {
        presentation.addText(value, SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
}
