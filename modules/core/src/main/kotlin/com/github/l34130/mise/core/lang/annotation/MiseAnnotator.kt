package com.github.l34130.mise.core.lang.annotation

import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

class MiseAnnotator : Annotator {
    override fun annotate(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        if (element !is LeafPsiElement) return

        MiseTomlTableTask.resolveFromTaskChainedTable(element)
            ?: MiseTomlTableTask.resolveFromInlineTableInTaskTable(element)
            ?: return

        holder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
            .textAttributes(DefaultLanguageHighlighterColors.CLASS_NAME)
            .create()
    }
}
