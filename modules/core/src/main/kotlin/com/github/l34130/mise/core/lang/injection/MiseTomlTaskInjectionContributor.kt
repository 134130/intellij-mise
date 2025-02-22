package com.github.l34130.mise.core.lang.injection

import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.psi.PsiElement
import com.intellij.sh.ShLanguage

class MiseTomlTaskInjectionContributor : LanguageInjectionContributor {
    override fun getInjection(context: PsiElement): Injection? {
        if (!MiseTomlPsiPatterns.inTaskRunString.accepts(context)) return null
        return runCatching {
            // Some IDEs doesn't have ShLanguage
            SimpleInjection(ShLanguage.INSTANCE, "", "", null)
        }.getOrNull()
    }
}
