package com.github.l34130.mise.core

import com.github.l34130.mise.core.icon.MiseIcons
import com.github.l34130.mise.core.lang.MiseTomlFileType
import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import javax.swing.Icon

class MiseIconProvider :
    IconProvider(),
    DumbAware {
    override fun getIcon(
        element: PsiElement,
        flags: Int,
    ): Icon? =
        when (element.containingFile?.fileType) {
            is MiseTomlFileType -> MiseIcons.DEFAULT
            else -> null
        }
}
