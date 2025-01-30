package com.github.l34130.mise.core.lang.psi

import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.VirtualFilePattern
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.sh.ShFileType

object MiseFileTaskPsiPatterns {
    private inline fun <reified I : PsiElement> shPsiElement(): PsiElementPattern.Capture<I> =
        psiElement<I>().inVirtualFile(
            VirtualFilePattern().ofType(ShFileType.INSTANCE),
        )

    val inComment = shPsiElement<PsiComment>()
}
