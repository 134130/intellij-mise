package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.github.l34130.mise.core.lang.psi.allTasks
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.lang.psi.taskName
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlTable

/**
 * ```
 * [tasks.foo]
 * ...
 *
 * [tasks.<task-name>]
 * depends = [ "f<caret>" ]
 *             #^ Provides completion for "foo"
 */
class MiseTomlTaskCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val element = parameters.position
        val miseTomlFile = element.containingFile as? MiseTomlFile ?: return

        val dependsArray = (element.parent.parent as? TomlArray) ?: return

        val parentTable = element.parentOfType<TomlTable>() ?: return
        val parentTaskName = parentTable.taskName

        for (task in miseTomlFile.allTasks()) {
            val taskName = task.name ?: continue
            if (dependsArray.elements.any { it.stringValue == taskName }) continue
            if (taskName == parentTaskName) continue

            result.addElement(
                LookupElementBuilder
                    .createWithSmartPointer(taskName, task)
                    .withInsertHandler(StringLiteralInsertionHandler()),
            )
        }
    }

    // TODO: Need to support literal string completion
}
