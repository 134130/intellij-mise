package com.github.l34130.mise.sh.lang.completion

import com.github.l34130.mise.sh.lang.psi.MiseFileTaskPsiPatterns
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
