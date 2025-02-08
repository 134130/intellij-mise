package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.MiseService
import com.github.l34130.mise.core.collapsePath
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.model.MiseTask
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ProcessingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTable

/**
 * ```toml
 * [tasks.foo]
 * run = "..."
 * ```
 *
 * then
 *
 * ```toml
 * [tasks.<task-name>]
 * depends = [ "f<caret>" ]
 *             #^ Provides completion for "foo"
 * ```
 *
 * or
 *
 * ```toml
 * [tasks.<task-name>]
 * depends = "f<caret>"
 *           #^ Provides completion for "foo"
 * ```
 *
 * or
 *
 * ```toml
 * [tasks]
 * <task-name> = { depends = [ "f<caret>" ] }
 *                             #^ Provides completion for "foo"
 * ```
 * or
 *
 * ```toml
 * [tasks]
 * <task-name> = { depends = "f<caret>" }
 *                           #^ Provides completion for "foo"
 * ```
 */
class MiseTomlTaskCompletionProvider : CompletionProvider<CompletionParameters>() {
    @Suppress("ktlint:standard:chain-method-continuation")
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val element = parameters.position
        val project = element.project

        val currentPsiFile = element.containingFile as? TomlFile ?: return // element.containingFile is in memory file
        val originalFile = (currentPsiFile.viewProvider.virtualFile as LightVirtualFile).originalFile

        val dependsArray = (element.parent.parent as? TomlArray)

        var currentTaskSegment: TomlKeySegment? = null
        // [tasks.<task-name>]
        (element.parent.parent.parent as? TomlTable ?: element.parent.parent.parent.parent as? TomlTable)?.let { tomlTable ->
            tomlTable.header.key?.segments?.getOrNull(1)?.let { currentTaskSegment = it }
        }
        // [tasks]
        // <task-name> = { ... }
        (element.parent.parent.parent as? TomlInlineTable ?: element.parent.parent.parent.parent as? TomlInlineTable)?.let { tomlInlineTable ->
            (tomlInlineTable.parent as? TomlKeyValue)?.key?.segments?.singleOrNull()?.let { currentTaskSegment = it }
        }
        if (currentTaskSegment == null) return

        runBlocking(Dispatchers.IO) {
            smartReadAction(project) {
                for (task in project.service<MiseService>().getTasks()) {
                    if (dependsArray?.elements?.any { it.stringValue == task.name } == true) continue
                    if (task.name == currentTaskSegment.name) continue

                    val psiElement =
                        when (task) {
                            is MiseTask.ShellScript -> task.file.findPsiFile(project)!!
                            is MiseTask.TomlTable -> task.keySegment
                            is MiseTask.Unknown -> {
                                result.addElement(
                                    LookupElementBuilder.create(task.name)
                                        .withInsertHandler(StringLiteralInsertionHandler())
                                        .withTypeText(task.source),
                                )
                                continue
                            }
                        }

                    val path =
                        when {
                            task is MiseTask.TomlTable && task.keySegment.containingFile.virtualFile == originalFile -> "current file"
                            else -> collapsePath(psiElement.containingFile, project)
                        }

                    result.addElement(
                        LookupElementBuilder
                            .createWithSmartPointer(task.name, psiElement)
                            .withInsertHandler(StringLiteralInsertionHandler())
                            .withIcon(psiElement.getIcon(Iconable.ICON_FLAG_VISIBILITY))
                            .withTypeText(path)
                            .withPriority(-path.length - 100.0),
                    )
                }
            }
        }
    }

    private fun LookupElementBuilder.withPriority(priority: Double): LookupElement = PrioritizedLookupElement.withPriority(this, priority)

    private fun VirtualFile.leafChildren(): Sequence<VirtualFile> =
        children.asSequence().flatMap {
            if (it.isDirectory) {
                it.leafChildren()
            } else {
                sequenceOf(it)
            }
        }
}
