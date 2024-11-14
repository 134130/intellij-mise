package com.github.l34130.mise.core.toolwindow.nodes

import com.intellij.openapi.project.Project
import javax.swing.Icon

abstract class MiseLeafNode<T : Any>(
    project: Project,
    value: T,
    icon: Icon? = null,
) : MiseNode<T>(project, value, icon) {
    override fun isAlwaysLeaf(): Boolean = true

    override fun getChildren(): Collection<MiseNode<*>> = emptyList()
}
