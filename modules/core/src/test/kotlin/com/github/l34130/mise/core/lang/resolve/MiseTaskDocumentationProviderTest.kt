package com.github.l34130.mise.core.lang.resolve

import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.withMiseCommandLineExecutor
import com.intellij.openapi.components.service
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.toml.lang.psi.TomlKeySegment

internal class MiseTaskDocumentationProviderTest : BasePlatformTestCase() {
    fun `test documentation for toml task`() {
        myFixture.configureByText(
            "mise.toml",
            """
            [tasks.fo<caret>o]
            description = "Foo description"
            run = "echo foo"
            """.trimIndent(),
        )

        // Warm cache before testing documentation
        withMiseCommandLineExecutor {
            runBlocking { project.service<MiseTaskResolver>().computeTasksFromSource() }
        }

        val elementAtCaret = myFixture.elementAtCaret
        val keySegment =
            PsiTreeUtil.getParentOfType(elementAtCaret, TomlKeySegment::class.java, false)
                ?: error("No TomlKeySegment at caret")
        val provider = MiseTaskDocumentationProvider()
        val doc = provider.generateDoc(keySegment, keySegment)

        assertNotNull(doc)
        assertTrue(doc!!.contains("foo"))
        assertTrue(doc.contains("Foo description"))
    }

    fun `test shell script documentation returns null when cache is cold`() {
        // Create a shell script file that would be a mise task
        val shellFile =
            myFixture.addFileToProject(
                "xtasks/test-task",
                """
                #!/usr/bin/env bash
                # This is a test task
                echo "test"
                """.trimIndent(),
            )

        // Ensure cache is cold by invalidating it
        project.service<MiseTaskResolver>().markCacheAsStale()

        val provider = MiseTaskDocumentationProvider()
        val doc = provider.generateDoc(shellFile, shellFile)

        // With cold cache, shell script documentation should return null
        // (unlike TOML tasks which resolve from PSI and don't need cache)
        assertNull(doc)
    }
}
