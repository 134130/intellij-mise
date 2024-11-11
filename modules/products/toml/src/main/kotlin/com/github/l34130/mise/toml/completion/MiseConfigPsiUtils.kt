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
    fun getMiseTasks(psiFile: PsiFile): List<MiseTask> {
        val tasks = mutableListOf<MiseTask>()

        // Handle table-style tasks ([tasks.abc])
        tasks.addAll(psiFile.children.filterIsInstance<TomlTable>().mapNotNull { it.parseTableStyleTask() })

        // Handle key-value style tasks (a = 'echo a')
        psiFile.children.filterIsInstance<TomlTable>()
            .find { it.header.key?.textMatches("tasks") == true }
            ?.let { tasksTable ->
                tasks.addAll(
                    tasksTable.entries
                        .mapNotNull { it.parseKeyValueStyleTask() })
            }

        return tasks
    }

    fun isInTaskDepends(psiElement: PsiElement): Boolean {
        val tomlArray = psiElement.parent.parent as? TomlArray ?: return false
        val tomlKeyValue = tomlArray.parent as? TomlKeyValue ?: return false

        // Check if we're in a table-style task
        val tomlTable = tomlKeyValue.parent as? TomlTable
        if (tomlTable != null && tomlTable.isMiseTask()) {
            return tomlKeyValue.key.textMatches("depends")
        }

        // Check if we're in a key-value style task's depends
        val tasksTable = tomlKeyValue.parent as? TomlTable
        return tasksTable?.header?.key?.textMatches("tasks") == true &&
                tomlKeyValue.key.textMatches("depends")
    }
}

fun TomlTable.isMiseTask(): Boolean {
    val headerKey = header.key ?: return false
    val firstSegment = headerKey.segments.firstOrNull() ?: return false
    return ElementManipulators.getValueText(firstSegment) == "tasks"
}

fun TomlTable.parseTableStyleTask(): MiseTask? {
    if (!isMiseTask()) {
        return null
    }

    val headerKey = header.key!!
    val segments = headerKey.segments.drop(1) // Skip the initial "tasks" segment
    val taskName = segments.joinToString(".") { ElementManipulators.getValueText(it) }

    return parseTaskProperties(entries)?.copy(name = taskName)
}

fun TomlKeyValue.parseKeyValueStyleTask(): MiseTask? {
    val taskName = ElementManipulators.getValueText(key)
    if (value is TomlLiteral) {
        return MiseTask(
            name = taskName,
            aliases = null,
            depends = null,
            description = null,
            hide = false,
            command = ElementManipulators.getValueText(value as TomlLiteral)
        )
    }

    // Handle table-style task definition
    val tableValue = value as? TomlTable ?: return null
    return parseTaskProperties(tableValue.entries)?.copy(name = taskName)
}

private fun parseTaskProperties(entries: List<TomlKeyValue>): MiseTask? {
    var aliases: List<String>? = null
    var depends: List<String>? = null
    var description: String? = null
    var hide = false
    var command: String? = null

    for (entry in entries) {
        val key = entry.key
        val value = entry.value
        when {
            key.textMatches("alias") -> {
                when (value) {
                    is TomlLiteral -> aliases = listOf(ElementManipulators.getValueText(value))
                    is TomlArray -> aliases = value.elements
                        .mapNotNull { it as? TomlLiteral }
                        .map { ElementManipulators.getValueText(it) }
                }
            }

            key.textMatches("depends") -> {
                when (value) {
                    is TomlArray -> depends = value.elements
                        .mapNotNull { it as? TomlLiteral }
                        .map { ElementManipulators.getValueText(it) }
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

            key.textMatches("run") -> {
                if (value is TomlLiteral) {
                    command = ElementManipulators.getValueText(value)
                }
            }
        }
    }

    return MiseTask(
        name = "", // set by caller
        aliases = aliases,
        depends = depends,
        description = description,
        hide = hide,
        command = command
    )
}