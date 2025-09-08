package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.icon.MiseIcons
import com.intellij.diagram.DiagramNodeBase
import com.intellij.diagram.DiagramProvider
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class MiseTaskGraphNode(
    private val miseTask: MiseTaskGraphableTaskWrapper<*>,
    provider: DiagramProvider<MiseTaskGraphable>,
) : DiagramNodeBase<MiseTaskGraphable>(provider) {
    override fun getIdentifyingElement(): MiseTaskGraphable = miseTask

    override fun getTooltip(): @Nls String? = miseTask.task.description

    override fun getIcon(): Icon? = MiseIcons.DEFAULT
}
