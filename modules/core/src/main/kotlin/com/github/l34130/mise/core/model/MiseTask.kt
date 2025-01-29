package com.github.l34130.mise.core.model

import com.intellij.openapi.vfs.VirtualFile
import org.toml.lang.psi.TomlKeySegment

sealed interface MiseTask {
    val name: String
    val aliases: List<String>?
    val description: String?

    class ShellScript(
        override val name: String,
        override val aliases: List<String>? = null,
        override val description: String? = null,
        val file: VirtualFile,
    ) : MiseTask

    class TomlTable(
        override val name: String,
        override val aliases: List<String>? = null,
        override val description: String? = null,
        val keySegment: TomlKeySegment,
    ) : MiseTask
}
