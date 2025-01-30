package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.stringValue
import com.intellij.psi.util.childrenOfType
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTable

class MiseTomlFile {
    class TaskConfig(
        origin: TomlTable,
    ) : TomlTable by origin {
        val includes: List<String>?
            get() = (getValueWithKey("includes") as? TomlArray)?.elements?.mapNotNull { it.stringValue }

        companion object {
            fun resolveOrNull(file: TomlFile): TaskConfig? {
                val table = file.childrenOfType<TomlTable>().firstOrNull { it.header.key?.textMatches("task_config") == true }
                return table?.let { TaskConfig(it) }
            }
        }
    }
}
