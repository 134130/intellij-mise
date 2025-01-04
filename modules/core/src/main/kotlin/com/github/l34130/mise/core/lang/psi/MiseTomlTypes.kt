package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.lang.MiseTomlLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

object MiseTomlTypes {
    val TASK_NAME: IElementType = IElementType("TASK_NAME", MiseTomlLanguage)
}

val MISE_TOML_KEY = TokenSet.create(MiseTomlTypes.TASK_NAME)
