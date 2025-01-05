package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.lang.MiseTomlFileType
import com.github.l34130.mise.core.lang.MiseTomlLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class MiseTomlFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, MiseTomlLanguage) {
    override fun getFileType() = MiseTomlFileType

    override fun toString() = "Mise Toml File"
}
