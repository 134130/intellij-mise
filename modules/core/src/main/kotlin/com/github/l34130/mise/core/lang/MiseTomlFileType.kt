package com.github.l34130.mise.core.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import org.toml.lang.TomlLanguage
import org.toml.lang.psi.TomlFileType
import javax.swing.Icon

object MiseTomlFileType : LanguageFileType(TomlLanguage) {
    override fun getName(): String = "mise"

    override fun getDescription(): String = "Mise Configuration file"

    override fun getDefaultExtension(): String = "toml"

    override fun getIcon(): Icon = TomlFileType.icon
}
