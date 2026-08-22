package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTable

/**
 * Provides completion for tool version strings in the `[tools]` section of mise.toml.
 *
 * ```toml
 * [tools]
 * nodejs = "22.<caret>"
 *           #^ Provides version completion
 *
 * [tools]
 * python = ["3.11", "<caret>"]
 *                    #^ Provides version completion
 *
 * [tools]
 * go = {version = "<caret>"}
 *                  #^ Provides version completion
 *
 * [tools.ruby]
 * version = "<caret>"
 *            #^ Provides version completion
 * ```
 */
class MiseTomlToolVersionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val element = parameters.position
        val project = element.project

        val toolName = resolveToolName(element) ?: return

        val workDir = project.guessMiseProjectPath()
        val configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment

        val versions =
            MiseCommandLineHelper
                .getToolVersions(project, toolName, workDir, configEnvironment.ifBlank { null })
                .getOrNull() ?: return

        for ((index, version) in versions.withIndex()) {
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                        .create(version)
                        .withIcon(AllIcons.Nodes.Tag)
                        .withInsertHandler(StringLiteralInsertionHandler()),
                    -index.toDouble(),
                ),
            )
        }
    }

    companion object {
        /**
         * Resolves the tool name from the PSI context around the caret.
         */
        fun resolveToolName(element: com.intellij.psi.PsiElement): String? {
            // Walk up to find the context
            var current = element.parent
            while (current != null) {
                when (current) {
                    // Direct value: [tools] \n tool = "version"
                    is TomlKeyValue -> {
                        val parent = current.parent
                        if (parent is TomlTable) {
                            val segments = parent.header.key?.segments
                            if (segments?.singleOrNull()?.name == "tools") {
                                return current.key.text
                            }
                        }
                        // Inline table: [tools] \n tool = {version = "..."}
                        if (current.key.text == "version") {
                            val inlineTable = current.parent as? TomlInlineTable
                            val outerKeyValue = inlineTable?.parent as? TomlKeyValue
                            val table = outerKeyValue?.parent as? TomlTable
                            if (table != null) {
                                val segments = table.header.key?.segments
                                if (segments?.singleOrNull()?.name == "tools") {
                                    return outerKeyValue.key.text
                                }
                            }
                        }
                    }
                    // Array value: [tools] \n tool = ["version1", "version2"]
                    is TomlArray -> {
                        val keyValue = current.parent as? TomlKeyValue
                        val table = keyValue?.parent as? TomlTable
                        if (table != null) {
                            val segments = table.header.key?.segments
                            if (segments?.singleOrNull()?.name == "tools") {
                                return keyValue.key.text
                            }
                        }
                    }
                    // Tool-specific table: [tools.python] \n version = "3.11"
                    is TomlTable -> {
                        val segments = current.header.key?.segments
                        if (segments?.size == 2 && segments[0].name == "tools") {
                            return segments[1].name
                        }
                    }
                }
                current = current.parent
            }
            return null
        }
    }
}
