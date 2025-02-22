package com.github.l34130.mise.core.lang.annotation

import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement

class MiseAnnotator : Annotator {
    override fun annotate(
        element: PsiElement,
        holder: AnnotationHolder,
    ) {
        MiseTomlTableTask.resolveOrNull(element)?.let { task ->
            holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(DefaultLanguageHighlighterColors.CLASS_NAME)
                .create()
        }
    }
}
