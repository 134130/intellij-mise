package com.github.l34130.mise.core.icon

import com.github.l34130.mise.core.model.MiseTomlFile
import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlFile
import javax.swing.Icon

class MiseIconProvider :
    IconProvider(),
    DumbAware {
    override fun getIcon(
        element: PsiElement,
        flags: Int,
    ): Icon? {
        if (element is TomlFile && MiseTomlFile.isMiseTomlFile(element.project, element.viewProvider.virtualFile)) {
            return MiseIcons.DEFAULT
        }
        return null
    }
}
