package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.github.l34130.mise.core.lang.psi.or
import com.github.l34130.mise.core.model.MiseTask
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader

class MiseTomlTaskRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (!(MiseTomlPsiPatterns.miseTomlStringLiteral or MiseTomlPsiPatterns.miseTomlLeafPsiElement).accepts(element)) return null
        val tomlKey = element.parent.parent as? TomlKey ?: return null

        val tomlTableHeader = tomlKey.parent as? TomlTableHeader
        if (tomlTableHeader != null) {
            val segments = tomlTableHeader.key?.segments ?: return null
            if (segments.size == 2 && segments.first().name == "tasks") {
                if (segments.first() != element.parent) { // escape self (tasks)
                    val task = MiseTask.TomlTable.resolveOrNull(segments[1]) ?: return null
                    return Info(AllIcons.Actions.Execute, { "" }, RunMiseTomlTaskAction(task))
                }
            }
        }

        val tomlTable = tomlKey.parent.parent as? TomlTable
        val taskName = tomlKey.segments.singleOrNull()
        if (tomlTable != null && taskName != null) {
            if (tomlKey.parent != tomlTable.header) { // escape self (tasks)
                val segments = tomlTable.header.key?.segments ?: return null
                if (segments.size == 1 && segments.first().name == "tasks") {
                    val task = MiseTask.TomlTable.resolveOrNull(taskName) ?: return null
                    return Info(AllIcons.Actions.Execute, { "" }, RunMiseTomlTaskAction(task))
                }
            }
        }

        return null
    }
}
