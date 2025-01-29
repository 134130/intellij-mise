package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.MiseService
import com.github.l34130.mise.core.collapsePath
import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.lang.psi.taskName
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
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ProcessingContext
import io.kinference.utils.runBlocking
import kotlinx.coroutines.Dispatchers
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlTable

/**
 * ```
 * [tasks.foo]
 * run = "..."
 * ```
 *
 * then
 *
 * ```
 * [tasks.<task-name>]
 * depends = [ "f<caret>" ]
 *             #^ Provides completion for "foo"
 * ```
 *
 * or
 *
 * ```
 * [tasks.<task-name>]
 * depends = "f<caret>"
 *           #^ Provides completion for "foo"
 * ```
 */
class MiseTomlTaskDependsCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val element = parameters.position
        val project = element.project
        val currentPsiFile = element.containingFile as? MiseTomlFile ?: return // element.containingFile is in memory file
        val originalFile = (currentPsiFile.viewProvider.virtualFile as LightVirtualFile).originalFile

        val dependsArray = (element.parent.parent as? TomlArray)

        val parentTable = element.parentOfType<TomlTable>() ?: return
        val currentTaskName = parentTable.taskName

        runBlocking(Dispatchers.IO) {
            smartReadAction(project) {
                for (task in project.service<MiseService>().getTasks()) {
                    if (dependsArray?.elements?.any { it.stringValue == task.name } == true) continue
                    if (task.name == currentTaskName) continue

                    val psiElement =
                        when (task) {
                            is MiseTask.ShellScript -> task.file.findPsiFile(project)!!
                            is MiseTask.TomlTable -> task.keySegment
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
