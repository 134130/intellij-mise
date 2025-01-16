package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.lang.MiseTomlFileType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import org.toml.lang.TomlLanguage

class MiseTomlFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, TomlLanguage) {
    override fun getFileType() = MiseTomlFileType

    override fun toString() = "Mise Toml File"
}
