@file:Suppress("ktlint")

package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.FileTestBase
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTableHeader

class MiseTomlPsiUtilsTest : FileTestBase() {
    fun `test TomlFile_allTasks`() {
        val file = InlineTomlFile(
            """
            [tasks]
            foo = {  }
            bar = {  }
            
            [tasks.baz]
            """.trimIndent(),
        )
        val tasks = file.allTasks().toList()
        assert(tasks.size == 3)
        assert(tasks[0].name == "foo")
        assert(tasks[1].name == "bar")
        assert(tasks[2].name == "baz")
    }

    fun `test TomlTableHeader_isSpecificTaskTableHeader`() {
        InlineTomlFile(
            """
            [tasks.foo]
                    #^
            """.trimIndent(),
        )
        val element = findElementInEditor<TomlTableHeader>()
        assert(element.isSpecificTaskTableHeader)
    }

    private fun InlineTomlFile(@Language("TOML") code: String, fileName: String = "mise.toml"): TomlFile =
        InlineFile(code, fileName) as TomlFile
}
