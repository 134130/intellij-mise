package com.github.l34130.mise.core.toolwindow.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes

class MiseErrorNode(
    project: Project,
    exception: Throwable,
) : MiseNode<Throwable>(project, exception, null) {
    override fun getChildren(): Collection<MiseNode<*>> = emptyList()

    override fun update(presentation: PresentationData) {
        presentation.apply {
            tooltip = value.message ?: value.javaClass.simpleName

            addText(value.message ?: value.javaClass.simpleName, SimpleTextAttributes.ERROR_ATTRIBUTES)
        }
    }
}
