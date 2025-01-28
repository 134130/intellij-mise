package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.lang.MiseTomlFileType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.childrenOfType
import org.toml.lang.TomlLanguage
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlTable

class MiseTomlFile(
    viewProvider: FileViewProvider,
) : PsiFileBase(viewProvider, TomlLanguage) {
    override fun getFileType() = MiseTomlFileType

    override fun toString() = "Mise Toml File"

    class TaskConfig(
        origin: TomlTable,
    ) : TomlTable by origin {
        val includes: List<String>?
            get() = (getValueWithKey("includes") as? TomlArray)?.elements?.mapNotNull { it.stringValue }

        companion object {
            fun resolveOrNull(file: MiseTomlFile): TaskConfig? {
                val table = file.childrenOfType<TomlTable>().firstOrNull { it.header.key?.textMatches("task_config") == true }
                return table?.let { TaskConfig(it) }
            }
        }
    }
}
