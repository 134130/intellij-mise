
package com.github.l34130.mise.toml.completion

import com.github.l34130.mise.commands.MiseTask
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable

object MiseConfigPsiUtils {
    fun getMiseTasks(psiFile: PsiFile): List<MiseTask> = psiFile.children.filterIsInstance<TomlTable>().mapNotNull { it.parseMiseTask() }

    fun isInTaskDepends(psiElement: PsiElement): Boolean {
        val tomlArray = psiElement.parent.parent as? TomlArray ?: return false
        val tomlKeyValue = tomlArray.parent as? TomlKeyValue ?: return false
        val tomlTable = tomlKeyValue.parent as? TomlTable ?: return false

        if (!tomlTable.isMiseTask()) {
            return false
        }

        return tomlKeyValue.key.textMatches("depends")
    }
}

fun TomlTable.isMiseTask(): Boolean {
    val headerKey = header.key ?: return false
    val firstSegment = headerKey.segments.firstOrNull() ?: return false
    return ElementManipulators.getValueText(firstSegment) == "tasks"
}

fun TomlTable.parseMiseTask(): MiseTask? {
    if (!isMiseTask()) {
        return null
    }

    val headerKey = header.key!!
    if (headerKey.segments.size > 2) {
        // TODO: handle nested tasks
        return null
    }

    var aliases: List<String>? = null
    var depends: List<String>? = null
    var description: String? = null
    var hide = false

    for (entry in entries) {
        val key = entry.key
        val value = entry.value
        when {
            key.textMatches("alias") -> {
                when (value) {
                    is TomlLiteral -> {
                        aliases = listOf(ElementManipulators.getValueText(value))
                    }

                    is TomlArray -> {
                        aliases =
                            value.elements
                                .mapNotNull { it as? TomlLiteral }
                                .map { ElementManipulators.getValueText(it) }
                    }
                }
            }

            key.textMatches("depends") -> {
                when (value) {
                    is TomlArray -> {
                        depends =
                            value.elements
                                .mapNotNull { it as? TomlLiteral }
                                .map { ElementManipulators.getValueText(it) }
                    }
                }
            }

            key.textMatches("description") -> {
                if (value is TomlLiteral) {
                    description = ElementManipulators.getValueText(value)
                }
            }

            key.textMatches("hide") -> {
                if (value is TomlLiteral) {
                    hide = ElementManipulators.getValueText(value).toBoolean()
                }
            }
        }
    }

    return MiseTask(
        name = ElementManipulators.getValueText(headerKey.segments[1]),
        aliases = aliases,
        depends = depends,
        description = description,
        hide = hide,
    )
}
