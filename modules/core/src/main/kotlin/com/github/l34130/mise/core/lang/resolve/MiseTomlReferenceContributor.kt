package com.github.l34130.mise.core.lang.resolve

import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class MiseTomlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            StandardPatterns.or(
                MiseTomlPsiPatterns.miseTomlStringLiteral().withParent(MiseTomlPsiPatterns.onTaskDependsArray),
                MiseTomlPsiPatterns.onTaskDependsString,
            ),
            MiseTomlTaskDependsReferenceProvider(),
        )
    }
}
