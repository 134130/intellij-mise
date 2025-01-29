package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.MiseService
import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.github.l34130.mise.core.lang.psi.allTasks
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.lang.psi.taskName
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import io.kinference.utils.runBlocking
import kotlinx.coroutines.Dispatchers
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlTable
import kotlin.io.path.isExecutable

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
                for (task in currentPsiFile.allTasks()) {
                    val taskName = task.name ?: continue
                    if (dependsArray?.elements?.any { it.stringValue == taskName } == true) continue
                    if (taskName == currentTaskName) continue

                    result.addElement(
                        LookupElementBuilder
                            .createWithSmartPointer(taskName, task)
                            .withInsertHandler(StringLiteralInsertionHandler()),
                    )
                }

                // get all tasks from all toml files in the project
                for (virtualFile in project.service<MiseService>().getMiseTomlFiles()) {
                    val psiFile = virtualFile.findPsiFile(project) as? MiseTomlFile ?: continue
                    if (psiFile == currentPsiFile) continue // FIXME: This doesn't work as expected (LightVirtualFile vs VirtualFile)

                    for (task in psiFile.allTasks()) {
                        val taskName = task.name ?: continue
                        if (dependsArray?.elements?.any { it.stringValue == taskName } == true) continue

                        result.addElement(
                            LookupElementBuilder
                                .createWithSmartPointer(taskName, task)
                                .withInsertHandler(StringLiteralInsertionHandler()),
                        )
                    }
                }

                // get all tasks from the task directories
                val fileTaskDirs = project.service<MiseService>().getFileTaskDirectories()
                for (dir in fileTaskDirs) {
                    val dirPath = dir.toNioPath()
                    dir
                        .leafChildren()
                        .filter { it.toNioPathOrNull()?.isExecutable() == true }
                        .forEach {
                            val taskName = dirPath.relativize(it.toNioPath()).joinToString(":")
                            LookupElementBuilder
                                .createWithSmartPointer(taskName, it.findPsiFile(element.project)!!)
                                .withInsertHandler(StringLiteralInsertionHandler())
                                .withIcon(it.fileType.icon)
                                .let(result::addElement)
                        }
                }
            }
        }
    }

    private fun VirtualFile.leafChildren(): Sequence<VirtualFile> =
        children.asSequence().flatMap {
            if (it.isDirectory) {
                it.leafChildren()
            } else {
                sequenceOf(it)
            }
        }
}
