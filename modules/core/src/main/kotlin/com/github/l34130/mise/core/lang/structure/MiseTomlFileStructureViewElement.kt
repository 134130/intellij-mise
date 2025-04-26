package com.github.l34130.mise.core.lang.structure

import com.github.l34130.mise.core.model.MiseTomlFile
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.util.childrenOfType
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTable
import javax.swing.Icon

class MiseTomlFileStructureViewElement(
    private val element: NavigatablePsiElement,
) : StructureViewTreeElement,
    SortableTreeElement {
    override fun getValue() = element

    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)

    override fun canNavigate(): Boolean = element.canNavigate()

    override fun canNavigateToSource(): Boolean = element.canNavigateToSource()

    override fun getAlphaSortKey(): String = element.name ?: ""

    override fun getPresentation(): ItemPresentation = element.presentation ?: PresentationData()

    override fun getChildren(): Array<out TreeElement> {
        if (element !is TomlFile) return emptyArray()
        if (!MiseTomlFile.isMiseTomlFile(element.project, element.virtualFile)) return emptyArray()

        val tools =
            treeElement(file = element, name = "Tools") {
                val toolsTable =
                    element.childrenOfType<TomlTable>().firstOrNull { it.header.key?.textMatches("tools") == true }
                        ?: return@treeElement emptyArray()

                toolsTable.entries
                    .mapNotNull {
                        val keySegment = it.key.segments.firstOrNull() ?: return@mapNotNull null
                        MiseTomlTableToolStructureViewElement(keySegment, it.value)
                    }.toTypedArray()
            }

        val tasks =
            treeElement(file = element, name = "Tasks") {
                MiseTomlTableTask
                    .resolveAllFromTomlFile(element)
                    .map { it.keySegment }
                    .map { MiseTomlTableTaskStructureViewElement(it) }
                    .toTypedArray()
            }

        val envs =
            treeElement(file = element, name = "Environments") {
                val envTable =
                    element.childrenOfType<TomlTable>().firstOrNull { it.header.key?.textMatches("env") == true }
                        ?: return@treeElement emptyArray()

                envTable.entries
                    .mapNotNull {
                        val keySegment = it.key.segments.firstOrNull() ?: return@mapNotNull null
                        MiseTomlTableEnvStructureViewElement(keySegment, it.value)
                    }.toTypedArray()
            }

        return arrayOf(tools, tasks, envs)
    }

    private fun treeElement(
        file: TomlFile,
        name: String,
        icon: Icon? = null,
        children: () -> Array<out TreeElement>,
    ): StructureViewTreeElement {
        val children = children()
        return object : StructureViewTreeElement {
            override fun getPresentation(): ItemPresentation = PresentationData(name, null, icon, null)

            override fun getChildren(): Array<out TreeElement> = children

            override fun getValue() = file
        }
    }
}
