package com.github.l34130.mise.core.lang.structure

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import org.jetbrains.plugins.terminal.TerminalIcons

class MiseTomlTableTaskStructureViewElement(
    private val element: NavigatablePsiElement,
) : StructureViewTreeElement,
    SortableTreeElement {
    override fun getValue() = element

    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)

    override fun canNavigate(): Boolean = element.canNavigate()

    override fun canNavigateToSource(): Boolean = element.canNavigateToSource()

    override fun getAlphaSortKey(): String = element.name ?: ""

    override fun getPresentation(): ItemPresentation =
        PresentationData(
            element.presentation?.presentableText ?: "",
            element.containingFile?.name,
            TerminalIcons.Command,
            null,
        )

    override fun getChildren(): Array<out TreeElement> = emptyArray()
}
