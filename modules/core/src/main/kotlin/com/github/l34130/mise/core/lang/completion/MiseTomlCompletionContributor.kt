package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.github.l34130.mise.core.lang.psi.or
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class MiseTomlCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            MiseTomlPsiPatterns.inTaskDependsArray or MiseTomlPsiPatterns.inTaskDependsString,
            MiseTomlTaskDependsCompletionProvider(),
        )
    }
}
