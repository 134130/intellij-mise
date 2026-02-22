package com.github.l34130.mise.core.lang.resolve

import com.github.l34130.mise.core.FileTestBase
import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.withMiseCommandLineExecutor
import com.intellij.openapi.components.service
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import kotlinx.coroutines.runBlocking
import org.toml.lang.psi.TomlLiteral

internal class MiseTomlTaskDependencyReferenceProviderTest : FileTestBase() {
    fun `test depends reference resolves after cache warm`() {
        myFixture.configureByText(
            "mise.toml",
            """
            [tasks.foo]
            run = "echo foo"

            [tasks.bar]
            depends = ["fo<caret>o"]
            """.trimIndent(),
        )

        withMiseCommandLineExecutor {
            runBlocking { project.service<MiseTaskResolver>().computeTasksFromSource() }
        }

        val elementAtCaret =
            myFixture.file.findElementAt(myFixture.caretOffset)
                ?: error("No element at caret")
        val literal =
            PsiTreeUtil.getParentOfType(elementAtCaret, TomlLiteral::class.java, false)
                ?: error("No TomlLiteral at caret")

        val provider = MiseTomlTaskDependencyReferenceProvider()
        val refs = provider.getReferencesByElement(literal, ProcessingContext())
        val reference = refs.single() as PsiPolyVariantReference
        val results = reference.multiResolve(false)

        assertEquals(1, results.size)
        assertEquals("foo", results.single().element?.text)
    }

    fun `test depends_post reference resolves after cache warm`() {
        myFixture.configureByText(
            "mise.toml",
            """
            [tasks.foo]
            run = "echo foo"

            [tasks.bar]
            depends_post = ["fo<caret>o"]
            """.trimIndent(),
        )

        withMiseCommandLineExecutor {
            runBlocking { project.service<MiseTaskResolver>().computeTasksFromSource() }
        }

        val elementAtCaret =
            myFixture.file.findElementAt(myFixture.caretOffset)
                ?: error("No element at caret")
        val literal =
            PsiTreeUtil.getParentOfType(elementAtCaret, TomlLiteral::class.java, false)
                ?: error("No TomlLiteral at caret")

        val provider = MiseTomlTaskDependencyReferenceProvider()
        val refs = provider.getReferencesByElement(literal, ProcessingContext())
        val reference = refs.single() as PsiPolyVariantReference
        val results = reference.multiResolve(false)

        assertEquals(1, results.size)
        assertEquals("foo", results.single().element?.text)
    }

    fun `test wait_for reference resolves after cache warm`() {
        myFixture.configureByText(
            "mise.toml",
            """
            [tasks.foo]
            run = "echo foo"

            [tasks.bar]
            wait_for = ["fo<caret>o"]
            """.trimIndent(),
        )

        withMiseCommandLineExecutor {
            runBlocking { project.service<MiseTaskResolver>().computeTasksFromSource() }
        }

        val elementAtCaret =
            myFixture.file.findElementAt(myFixture.caretOffset)
                ?: error("No element at caret")
        val literal =
            PsiTreeUtil.getParentOfType(elementAtCaret, TomlLiteral::class.java, false)
                ?: error("No TomlLiteral at caret")

        val provider = MiseTomlTaskDependencyReferenceProvider()
        val refs = provider.getReferencesByElement(literal, ProcessingContext())
        val reference = refs.single() as PsiPolyVariantReference
        val results = reference.multiResolve(false)

        assertEquals(1, results.size)
        assertEquals("foo", results.single().element?.text)
    }
}
