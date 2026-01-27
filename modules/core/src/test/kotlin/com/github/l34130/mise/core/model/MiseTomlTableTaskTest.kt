package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.FileTestBase
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlFile

class MiseTomlTableTaskTest : FileTestBase() {
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
        val actualDependency = tasks[1].depends?.get(0)?.get(0)
        assertEquals(
            """
            Expected 'echo' but got ${actualDependency}.
            The task should have been resolved to 'echo' and the args should have been removed.
            """.trimIndent(),
            "echo",
            actualDependency
        )
    }
}
