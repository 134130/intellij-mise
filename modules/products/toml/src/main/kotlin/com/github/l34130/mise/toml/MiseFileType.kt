package com.github.l34130.mise.toml

import com.github.l34130.mise.icons.MiseIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import org.toml.lang.TomlLanguage
import javax.swing.Icon

object MiseFileType : LanguageFileType(TomlLanguage) {
    override fun getName(): String = "mise"

    override fun getDescription(): String = "Mise Configuration file"

    override fun getDefaultExtension(): String = "toml"

    override fun getIcon(): Icon = MiseIcons.DEFAULT
}
