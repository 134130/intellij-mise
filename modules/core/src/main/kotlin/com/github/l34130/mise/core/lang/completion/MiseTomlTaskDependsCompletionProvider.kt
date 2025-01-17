package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.MiseTaskService
import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.github.l34130.mise.core.lang.psi.allTasks
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.lang.psi.taskName
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
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
        val miseTomlFile = element.containingFile as? MiseTomlFile ?: return

        val dependsArray = (element.parent.parent as? TomlArray)

        val parentTable = element.parentOfType<TomlTable>() ?: return
        val parentTaskName = parentTable.taskName

        // get all tasks from the current toml file
        for (task in miseTomlFile.allTasks()) {
            val taskName = task.name ?: continue
            if (dependsArray?.elements?.any { it.stringValue == taskName } == true) continue
            if (taskName == parentTaskName) continue

            result.addElement(
                LookupElementBuilder
                    .createWithSmartPointer(taskName, task)
                    .withInsertHandler(StringLiteralInsertionHandler()),
            )
        }

        // TODO: get all tasks from all toml files

        // get all tasks from the task directories
        val fileTaskDirs = MiseTaskService.getFileTaskDirectories(element.project)
        for (dir in fileTaskDirs) {
            dir.children
                .filter { it.toNioPathOrNull()?.isExecutable() == true }
                .forEach {
                    LookupElementBuilder
                        .createWithSmartPointer(it.name, it.findPsiFile(element.project)!!)
                        .withInsertHandler(StringLiteralInsertionHandler())
                        .let(result::addElement)
                }
        }
    }
}
