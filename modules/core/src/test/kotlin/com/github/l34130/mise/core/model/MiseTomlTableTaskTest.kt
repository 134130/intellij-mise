package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.FileTestBase
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.toml.lang.psi.TomlFile

class MiseTomlTableTaskTest : FileTestBase() {
    @Test
    fun `test resolveAllFromTomlFile should remove args`() {
        @Language("TOML")
        val tomlText =
            """
            [tasks.echo]
            run = "echo"
            
            [tasks.hello-world]
            depends = ["echo 'Hello World'"]
            """.trimIndent()

        val tomlFile = inlineFile(tomlText, "mise.toml") as TomlFile
        val tasks = MiseTomlTableTask.resolveAllFromTomlFile(tomlFile)

        assertEquals(2, tasks.size)
        assertEquals("echo", tasks[0].name)
        assertEquals("hello-world", tasks[1].name)
        assertEquals("echo", tasks[1].depends!![0]) {
            """
            Expected 'echo' but got ${tasks[1].depends!![0]}.
            The task should have been resolved to 'echo' and the args should have been removed.
            """.trimIndent()
        }
    }
}
