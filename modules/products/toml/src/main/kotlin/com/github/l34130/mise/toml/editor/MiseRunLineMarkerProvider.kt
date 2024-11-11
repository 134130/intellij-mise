package com.github.l34130.mise.toml.editor

import com.github.l34130.mise.commands.MiseRunAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.toml.lang.psi.*

class MiseRunLineMarkerProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        val isMiseFile = element.containingFile.name.contains("mise") && element.containingFile.name.endsWith(".toml")
        if (!isMiseFile || element !is LeafPsiElement) {
            return null
        }

        val taskName = getTaskInfo(element) ?: return null

        return Info(
            AllIcons.Actions.Execute,
            { "Run mise task: $taskName" },
            MiseRunAction(taskName)
        )
    }

    private fun getTaskInfo(element: LeafPsiElement): String? {
        val parent = element.parent ?: return null

        if (parent is TomlKeySegment) {
            // Case 1: [tasks] section
            // [tasks]
            // taskname = "..."
            // other = "..."
            val keyValue = parent.parent?.parent
            if (keyValue is TomlKeyValue) {
                val table = findParentTable(keyValue) ?: return null
                if (table.header.key?.text == "tasks") {
                    return parent.text
                }
            }


            // Case 2: Table-style task [tasks.taskname]
            // [tasks.taskname]
            // run = "..."
            // [tasks.other]
            // run = "..."
            if (parent.parent is TomlKey && parent.parent.parent is TomlTableHeader && element.text != "tasks") {
                val tomlKey = parent.parent as TomlKey
                val segments = tomlKey.segments
                if (segments.size == 2) {
                    if (segments[0].text == "tasks") {
                        return segments[1].text
                    }
                }
            }
        }

        return null
    }

    private fun findParentTable(element: PsiElement): TomlTable? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is TomlTable) {
                return current
            }
            current = current.parent
        }
        return null
    }
}