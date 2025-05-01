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

    override fun getChildren(): Array<out TreeElement> = children

    private val children =
        run {
            if (element !is TomlFile) return@run emptyArray()
            if (!MiseTomlFile.isMiseTomlFile(element.project, element.virtualFile)) return@run emptyArray()

            val tools =
                element.childrenOfType<TomlTable>().firstOrNull { it.header.key?.textMatches("tools") == true }?.let { table ->
                    GroupingStructureViewElement(
                        element =
                            table.header.key
                                ?.segments
                                ?.firstOrNull() ?: return@let null,
                        name = "Tools",
                        icon = null,
                        children =
                            table.entries
                                .mapNotNull {
                                    val keySegment = it.key.segments.firstOrNull() ?: return@mapNotNull null
                                    MiseTomlTableToolStructureViewElement(keySegment, it.value)
                                }.toTypedArray(),
                    )
                }

            val tasks =
                GroupingStructureViewElement(
                    element = element,
                    name = "Tasks",
                    icon = null,
                    children =
                        run {
                            MiseTomlTableTask
                                .resolveAllFromTomlFile(element as TomlFile)
                                .map { it.keySegment }
                                .map { MiseTomlTableTaskStructureViewElement(it) }
                                .toTypedArray()
                        },
                )

            val envs =
                element.childrenOfType<TomlTable>().firstOrNull { it.header.key?.textMatches("env") == true }?.let { table ->
                    GroupingStructureViewElement(
                        element =
                            table.header.key
                                ?.segments
                                ?.firstOrNull() ?: return@let null,
                        name = "Environments",
                        icon = null,
                        children =
                            run {
                                table.entries
                                    .mapNotNull {
                                        val keySegment = it.key.segments.firstOrNull() ?: return@mapNotNull null
                                        MiseTomlTableEnvStructureViewElement(keySegment, it.value)
                                    }.toTypedArray()
                            },
                    )
                }

            listOfNotNull(tools, tasks, envs).toTypedArray()
        }

    private class GroupingStructureViewElement(
        private val element: NavigatablePsiElement,
        private val name: String,
        private val icon: Icon? = null,
        private val children: Array<out TreeElement>,
    ) : StructureViewTreeElement {
        override fun getValue() = element

        override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)

        override fun canNavigate(): Boolean = element.canNavigate()

        override fun canNavigateToSource(): Boolean = element.canNavigateToSource()

        override fun getPresentation(): ItemPresentation = PresentationData(name, null, icon, null)

        override fun getChildren(): Array<out TreeElement> = children
    }
}
