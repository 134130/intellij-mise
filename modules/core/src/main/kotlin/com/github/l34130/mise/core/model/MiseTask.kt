package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.stringArray
import com.github.l34130.mise.core.lang.psi.stringValue
import com.intellij.openapi.vfs.VirtualFile
import org.toml.lang.psi.TomlKeySegment

sealed interface MiseTask {
    val name: String
    val aliases: List<String>?
    val depends: List<String>?
    val description: String?

    class ShellScript internal constructor(
        override val name: String,
        override val aliases: List<String>? = null,
        override val depends: List<String>? = null,
        override val description: String? = null,
        val file: VirtualFile,
    ) : MiseTask {
        companion object {
            fun resolveOrNull(
                baseDir: VirtualFile,
                file: VirtualFile,
            ): ShellScript? =
                ShellScript(
                    name = baseDir.toNioPath().relativize(file.toNioPath()).joinToString(":"),
                    file = file,
                )
        }
    }

    class TomlTable internal constructor(
        override val name: String,
        override val aliases: List<String>? = null,
        override val depends: List<String>? = null,
        override val description: String? = null,
        val keySegment: TomlKeySegment,
    ) : MiseTask {
        companion object {
            fun resolveOrNull(keySegment: TomlKeySegment): TomlTable? {
                val table = keySegment.parent.parent.parent as? org.toml.lang.psi.TomlTable ?: return null
                return TomlTable(
                    name = keySegment.name ?: return null,
                    description = table.getValueWithKey("description")?.stringValue,
                    depends = table.getValueWithKey("depends")?.stringArray,
                    aliases = table.getValueWithKey("alias")?.stringArray,
                    keySegment = keySegment,
                )
            }
        }
    }
}
