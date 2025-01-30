package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.lang.psi.MiseFileTaskPsiPatterns
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class MiseFileTaskCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            MiseFileTaskPsiPatterns.inComment,
            MiseFileTaskCommentCompletionProvider(),
        )
    }
}
