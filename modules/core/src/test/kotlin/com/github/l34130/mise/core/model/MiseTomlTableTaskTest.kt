package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.FileTestBase
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlFile

class MiseTomlTableTaskTest : FileTestBase() {
    fun `test resolveAllFromTomlFile should resolve string value tasks`() {
        @Language("TOML")
        val tomlText =
            """
            [tasks]
            codegen = "bun run openapi-ts"
            test = "bun test"
            compile = "bun build --compile --outfile out/blah ./src/index.ts"
            """.trimIndent()

        val tomlFile = inlineFile(tomlText, "mise.toml") as TomlFile
        val tasks = MiseTomlTableTask.resolveAllFromTomlFile(tomlFile)

        assertEquals(3, tasks.size)
        assertEquals("codegen", tasks[0].name)
        assertEquals("test", tasks[1].name)
        assertEquals("compile", tasks[2].name)
    }

    fun `test resolveAllFromTomlFile should resolve mixed task formats`() {
        @Language("TOML")
        val tomlText =
            """
            [tasks]
            simple = "echo hello"
            complex = { run = "echo world", description = "A complex task" }
            
            [tasks.chained]
            run = "echo chained"
            """.trimIndent()

        val tomlFile = inlineFile(tomlText, "mise.toml") as TomlFile
        val tasks = MiseTomlTableTask.resolveAllFromTomlFile(tomlFile)

        assertEquals(3, tasks.size)
        assertEquals("simple", tasks[0].name)
        assertNull(tasks[0].description)
        assertEquals("complex", tasks[1].name)
        assertEquals("A complex task", tasks[1].description)
        assertEquals("chained", tasks[2].name)
    }

    fun `test resolveAllFromTomlFile should resolve array value tasks`() {
        @Language("TOML")
        val tomlText =
            """
            [tasks]
            multi = ["echo one", "echo two"]
            """.trimIndent()

        val tomlFile = inlineFile(tomlText, "mise.toml") as TomlFile
        val tasks = MiseTomlTableTask.resolveAllFromTomlFile(tomlFile)

        assertEquals(1, tasks.size)
        assertEquals("multi", tasks[0].name)
    }

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
