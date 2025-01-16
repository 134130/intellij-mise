package com.github.l34130.mise.core.lang.resolve

import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.github.l34130.mise.core.lang.psi.resolveTask
import com.github.l34130.mise.core.lang.psi.stringValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlLiteral

/**
 * ```
 * [tasks.foo]
 * run = "echo foo"
 * ```
 *
 * ```
 * [tasks.bar]
 * depends = [ "foo" ]
 *             #^ Provides a reference for "foo"
 * ```
 */
class MiseTomlTaskDependsReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext,
    ): Array<PsiReference> {
        if (element !is TomlLiteral) return PsiReference.EMPTY_ARRAY
        return arrayOf(MiseTomlTaskDependsReference(element))
    }

    private class MiseTomlTaskDependsReference(
        element: TomlLiteral,
    ) : PsiPolyVariantReferenceBase<TomlLiteral>(element) {
        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            val literalValue = element.stringValue ?: return ResolveResult.EMPTY_ARRAY
            val miseTomlFile = element.containingFile as? MiseTomlFile ?: return ResolveResult.EMPTY_ARRAY

            return miseTomlFile.resolveTask(literalValue).toList().toTypedArray()
        }
    }
}
