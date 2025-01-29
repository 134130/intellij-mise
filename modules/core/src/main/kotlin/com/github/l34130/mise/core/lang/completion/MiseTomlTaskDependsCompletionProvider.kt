package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.MiseService
import com.github.l34130.mise.core.collapsePath
import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.github.l34130.mise.core.lang.psi.allTasks
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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.util.parentOfType
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
        val currentPsiFile = element.containingFile as? MiseTomlFile ?: return

        val dependsArray = (element.parent.parent as? TomlArray)

        val parentTable = element.parentOfType<TomlTable>() ?: return
        val currentTaskName = parentTable.taskName

        runBlocking(Dispatchers.IO) {
            smartReadAction(project) {
                // get all tasks from the current toml file
                val currentTasks = currentPsiFile.allTasks().toList()
                for ((i, task) in currentTasks.withIndex()) {
                    if (dependsArray?.elements?.any { it.stringValue == task.name } == true) continue
                    if (task.name == currentTaskName) continue

                    result.addElement(
                        LookupElementBuilder
                            .createWithSmartPointer(task.name, task.keySegment)
                            .withInsertHandler(StringLiteralInsertionHandler())
                            .withTypeText("current file")
                            .withPriority(currentTasks.size - i.toDouble()),
                    )
                }

                for (task in project.service<MiseService>().getTasks()) {
                    if (dependsArray?.elements?.any { it.stringValue == task.name } == true) continue
                    if (task.name == currentTaskName) continue

                    val psiElement =
                        when (task) {
                            is MiseTask.ShellScript -> task.file.findPsiFile(project)!!
                            is MiseTask.TomlTable -> task.keySegment
                        }

                    result.addElement(
                        LookupElementBuilder
                            .createWithSmartPointer(task.name, psiElement)
                            .withInsertHandler(StringLiteralInsertionHandler())
                            .withTypeText(collapsePath(psiElement.containingFile, project))
                            .withPriority(currentTasks.size.toDouble()),
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
