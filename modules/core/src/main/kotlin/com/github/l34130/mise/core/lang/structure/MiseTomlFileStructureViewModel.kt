package com.github.l34130.mise.core.lang.structure

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.toml.lang.psi.TomlTable

class MiseTomlFileStructureViewModel(
    psiFile: PsiFile,
    editor: Editor?,
) : StructureViewModelBase(psiFile, editor, MiseTomlFileStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {
    override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false

    override fun getSuitableClasses(): Array<out Class<*>> = arrayOf(TomlTable::class.java)
}
