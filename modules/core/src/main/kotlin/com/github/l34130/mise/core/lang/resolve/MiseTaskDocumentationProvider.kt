package com.github.l34130.mise.core.lang.resolve

import com.github.l34130.mise.core.MiseService
import com.github.l34130.mise.core.collapsePath
import com.github.l34130.mise.core.model.MiseTask
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.sh.psi.ShFile
import com.intellij.util.containers.nullize
import org.jetbrains.annotations.Nls
import org.toml.lang.psi.TomlKeySegment

class MiseTaskDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(
        element: PsiElement?,
        originalElement: PsiElement?,
    ): @Nls String? {
        if (element == null) return null

        val task =
            when (element) {
                is ShFile -> {
                    val service = element.project.service<MiseService>()
                    val tasks = service.getTasks()
                    tasks.firstOrNull { it is MiseTask.ShellScript && it.file == element.virtualFile } as MiseTask.ShellScript
                }
                is TomlKeySegment -> MiseTask.TomlTable.resolveOrNull(element)
                else -> return null
            } ?: return null

        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append(task.name)
            append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START)
            append(task.description ?: DocumentationMarkup.GRAYED_ELEMENT.addText("No description provided."))
            append(DocumentationMarkup.CONTENT_END)
            append(DocumentationMarkup.SECTIONS_START)
            appendKeyValueSection("Alias:", task.aliases.nullize()?.joinToString(", "))
            appendKeyValueSection("Depends:", task.depends.nullize()?.joinToString(", "))
            appendKeyValueSection(
                "File:",
                when (task) {
                    is MiseTask.ShellScript -> collapsePath(element as ShFile, element.project)
                    is MiseTask.TomlTable -> collapsePath(task.keySegment.containingFile, element.project)
                },
            )
            append(DocumentationMarkup.SECTIONS_END)
        }
    }

    private fun StringBuilder.appendKeyValueSection(
        key: String,
        value: String?,
    ) {
        append(DocumentationMarkup.SECTION_HEADER_START)
        append(key)
        append(DocumentationMarkup.SECTION_SEPARATOR)
        append("<p>")
        append(value ?: DocumentationMarkup.GRAYED_ELEMENT.addText("None"))
        append(DocumentationMarkup.SECTION_END)
    }
}
