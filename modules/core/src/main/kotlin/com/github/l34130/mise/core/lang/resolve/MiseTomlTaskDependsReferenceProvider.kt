package com.github.l34130.mise.core.lang.resolve

import com.github.l34130.mise.core.MiseProjectService
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.model.MiseShellScriptTask
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.github.l34130.mise.core.model.MiseUnknownTask
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlFile
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
            if (element.containingFile !is TomlFile) return ResolveResult.EMPTY_ARRAY

            var value = literalValue.split(' ', ignoreCase = false, limit = 2).firstOrNull() ?: return ResolveResult.EMPTY_ARRAY
            val isWildcard = value.endsWith(":*")

            val project = element.project
            val tasks = project.service<MiseProjectService>().getTasks()

            val result =
                if (isWildcard) {
                    // FIXME: IDK why this is not working
                    tasks.filter { it.name.startsWith(value.dropLast(1)) }
                } else {
                    tasks.filter { it.name == value }
                }.mapNotNull {
                    when (it) {
                        is MiseShellScriptTask -> it.file.findPsiFile(project)
                        is MiseTomlTableTask -> it.keySegment
                        is MiseUnknownTask -> null
                    }
                }

            return PsiElementResolveResult.createResults(result)
        }
    }
}
