package com.github.l34130.mise.core.lang.resolve

import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class MiseTomlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.or(
                MiseTomlPsiPatterns.miseTomlStringLiteral.withParent(MiseTomlPsiPatterns.onTaskDependsArray),
                MiseTomlPsiPatterns.onTaskDependsString,
                MiseTomlPsiPatterns.miseTomlStringLiteral.withParent(MiseTomlPsiPatterns.onTaskWaitForArray),
                MiseTomlPsiPatterns.onTaskWaitForString,
            ),
            MiseTomlTaskDependencyReferenceProvider(),
        )
    }
}
