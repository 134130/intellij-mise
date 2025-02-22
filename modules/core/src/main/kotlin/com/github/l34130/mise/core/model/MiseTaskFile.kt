package com.github.l34130.mise.core.model

import com.intellij.psi.util.childrenOfType
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTable

class MiseTaskFile {
    companion object {
        fun resolveTasks(file: TomlFile): List<MiseTask> =
            file
                .childrenOfType<TomlTable>()
                .mapNotNull { table ->
                    table.header.key
                        ?.segments
                        ?.singleOrNull()
                }.mapNotNull {
                    MiseTask.TomlTable.resolveOrNull(it)
                }
    }
}
