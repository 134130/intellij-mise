package com.github.l34130.mise.core.lang.resolve

import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.model.MiseShellScriptTask
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.github.l34130.mise.core.model.MiseUnknownTask
import com.github.l34130.mise.core.util.presentablePath
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.nullize
import kotlinx.coroutines.runBlocking
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
                is PsiFile -> {
                    if (element.language.id != "Shell Script") return null
                    val service = element.project.service<MiseTaskResolver>()
                    val tasks = runBlocking { service.getMiseTasks() }
                    tasks.firstOrNull { it is MiseShellScriptTask && it.file == element.virtualFile } as MiseShellScriptTask?
                }
                is TomlKeySegment ->
                    MiseTomlTableTask.resolveFromTaskChainedTable(element)
                        ?: MiseTomlTableTask.resolveFromInlineTableInTaskTable(element)
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
            appendKeyValueSection("Wait for:", task.waitFor.nullize()?.joinToString(", "))
            appendKeyValueSection("Depends Post:", task.dependsPost.nullize()?.joinToString(", "))
            appendKeyValueSection(
                "File:",
                when (task) {
                    is MiseShellScriptTask ->
                        presentablePath(
                            element.project,
                            (element as PsiFile)
                                .containingFile.viewProvider.virtualFile.path,
                        )
                    is MiseTomlTableTask -> presentablePath(element.project, task.keySegment.containingFile.viewProvider.virtualFile.path)
                    is MiseUnknownTask -> presentablePath(element.project, task.source)
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
