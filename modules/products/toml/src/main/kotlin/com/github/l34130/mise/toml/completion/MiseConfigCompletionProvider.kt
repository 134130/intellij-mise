package com.github.l34130.mise.toml.completion

import com.github.l34130.mise.icons.MiseIcons
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class MiseConfigCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        params: CompletionParameters,
        context: ProcessingContext,
        resultSet: CompletionResultSet,
    ) {
        val position = params.position
        val root = position.containingFile

        if (!MiseConfigPsiUtils.isInTaskDepends(position)) {
            return
        }

        val miseTasks = MiseConfigPsiUtils.getMiseTasks(root)

        resultSet.addAllElements(
            miseTasks
                .map {
                    LookupElementBuilder
                        .create(it.name)
                        .withIcon(MiseIcons.DEFAULT)
                        .withTypeText(it.description)
                        .withCaseSensitivity(false)
                },
        )
    }
}
