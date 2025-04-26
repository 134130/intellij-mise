package com.github.l34130.mise.core.lang.structure

import com.github.l34130.mise.core.lang.psi.stringValue
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import org.toml.lang.psi.TomlValue

class MiseTomlTableEnvStructureViewElement(
    private val key: NavigatablePsiElement,
    private val value: TomlValue?,
) : StructureViewTreeElement,
    SortableTreeElement {
    override fun getValue() = key

    override fun navigate(requestFocus: Boolean) = key.navigate(requestFocus)

    override fun canNavigate(): Boolean = key.canNavigate()

    override fun canNavigateToSource(): Boolean = key.canNavigateToSource()

    override fun getAlphaSortKey(): String = key.name ?: ""

    override fun getPresentation(): ItemPresentation =
        PresentationData(
            key.presentation?.presentableText ?: "",
            value?.stringValue,
            AllIcons.General.InlineVariables,
            null,
        )

    override fun getChildren(): Array<out TreeElement> = emptyArray()
}
