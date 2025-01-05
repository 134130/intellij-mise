package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class MiseTomlCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, MiseTomlPsiPatterns.inTaskDependsArray, MiseTomlTaskCompletionProvider())
    }
}
