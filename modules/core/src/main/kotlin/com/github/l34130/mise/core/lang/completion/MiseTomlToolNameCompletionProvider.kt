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
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTable

/**
 * Provides completion for tool names (keys) in the `[tools]` section of mise.toml.
 *
 * ```toml
 * [tools]
 * nod<caret>
 * #^ Provides completion for tool names like "nodejs", "node", etc.
 * ```
 */
class MiseTomlToolNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val element = parameters.position
        val project = element.project

        val workDir = project.guessMiseProjectPath()
        val configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment

        val registryTools =
            MiseCommandLineHelper
                .getRegistry(project, workDir, configEnvironment.ifBlank { null })
                .getOrNull() ?: return

        // Collect already-defined tool names in the [tools] table to exclude from suggestions
        val existingTools = collectExistingToolNames(element)

        for (toolName in registryTools) {
            if (toolName in existingTools) continue

            result.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                        .create(toolName)
                        .withIcon(AllIcons.General.Gear),
                    0.0,
                ),
            )
        }
    }

    private fun collectExistingToolNames(element: com.intellij.psi.PsiElement): Set<String> {
        var current = element.parent
        while (current != null) {
            if (current is TomlTable) {
                val segments = current.header.key?.segments
                if (segments?.singleOrNull()?.name == "tools") {
                    return current.entries
                        .mapNotNull { (it as? TomlKeyValue)?.key?.text }
                        .toSet()
                }
            }
            current = current.parent
        }
        return emptySet()
    }
}
