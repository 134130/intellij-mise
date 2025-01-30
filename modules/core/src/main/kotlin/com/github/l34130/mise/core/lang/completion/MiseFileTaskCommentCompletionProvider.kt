package com.github.l34130.mise.core.lang.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * #MISE alias=[]
 * #MISE wa<caret>
 *       #^ Provides completion for "wait_for"
 */
class MiseFileTaskCommentCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val element = parameters.position
        val project = element.project

        val elementText = element.text.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
        if (!elementText.startsWith("#MISE ")) return

        for (option in MISE_CONFIGURATION_OPTIONS) {
            result.addElement(
                LookupElementBuilder
                    .create(option.name)
                    .withTypeText(option.type),
            )
        }
    }

    companion object {
        private val MISE_CONFIGURATION_OPTIONS =
            listOf<TaskConfigOption>(
                TaskConfigOption(
                    name = "alias",
                    description = "alias for this task",
                    type = "string | string[]",
                ),
                TaskConfigOption(
                    name = "depends",
                    description = "task with args to run before this task",
                    type = "string | string[]",
                ),
                TaskConfigOption(
                    name = "depends_post",
                    description = "task with args to run after this task",
                    type = "string | string[]",
                ),
                TaskConfigOption(
                    name = "wait_for",
                    description = "task with args to wait for completion first",
                    type = "string | string[]",
                ),
                TaskConfigOption(
                    name = "description",
                    description = "description of task",
                    type = "string",
                ),
                TaskConfigOption(
                    name = "dir",
                    description = "directory to run script in, default is current working directory",
                    type = "string",
                    default = "{{ config_root }}",
                ),
                TaskConfigOption(
                    name = "env",
                    description = "environment variables",
                    type = "{ [key]: string | int | bool }",
                ),
                TaskConfigOption(
                    name = "tools",
                    description = "tools to install/activate before running this task",
                    type = "{ [key]: string }",
                ),
                TaskConfigOption(
                    name = "hide",
                    description = "do not display this task",
                    type = "bool",
                    default = "false",
                ),
                TaskConfigOption(
                    name = "outputs",
                    description = "glob pattern or path to files created by this task",
                    type = "string | string[] | { auto = true }",
                ),
                TaskConfigOption(
                    name = "quiet",
                    description = "do not display mise information for this task",
                    type = "bool",
                    default = "false",
                ),
                TaskConfigOption(
                    name = "sources",
                    description = "glob pattern or path to files that this task depends on",
                    type = "string | string[]",
                ),
                TaskConfigOption(
                    name = "shell",
                    description = "specify a shell command to run the script with",
                    type = "string",
                ),
                TaskConfigOption(
                    name = "usage",
                    description = "Specify usage (https://usage.jdx.dev/) specs for the task",
                    type = "string",
                ),
            )
    }

    class TaskConfigOption(
        val name: String,
        val description: String,
        val type: String,
        val default: String? = null,
    )
}
