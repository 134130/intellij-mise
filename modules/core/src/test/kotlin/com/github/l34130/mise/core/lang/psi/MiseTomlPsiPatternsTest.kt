@file:Suppress("ktlint")

package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.FileTestBase
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns.inTaskDependsArray
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns.inTaskDependsString
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.jetbrains.rd.util.assert
import org.intellij.lang.annotations.Language

class MiseTomlPsiPatternsTest : FileTestBase() {
    fun `test onTaskProperty(name) on task specific table`() = testPattern(MiseTomlPsiPatterns.onTaskProperty("name"), """
        [tasks.foo]
        name = []
        #^
    """)

    fun `test onTaskProperty(name) on task table`() = testPattern(MiseTomlPsiPatterns.onTaskProperty("name"), """
        [tasks]
        foo = { name = [] }
                #^
    """)

    fun `test inTaskDependsArray`() = testPattern(inTaskDependsArray, """
        [tasks.foo]
        depends = ["bar", ""]
                         #^
    """)

    fun `test inTaskDependsArray with empty array`() = testPattern(inTaskDependsArray, """
        [tasks.foo]
        depends = [""]
                  #^
    """)

    fun `test inTaskDependsString with`() = testPattern(inTaskDependsString, """
        [tasks.foo]
        depends = "f"
                  #^
    """)

    fun `test inTaskDependsString with empty string`() = testPattern(inTaskDependsString, """
        [tasks.foo]
        depends = ""
                 #^
    """)

    fun `test inTaskRunStringOrArray on bare task table in include file`() {
        myFixture.addFileToProject("mise.toml", """
            [task_config]
            includes = ["buf.toml"]
        """.trimIndent())

        val bufToml = myFixture.addFileToProject("buf.toml", """
            ["buf:lint"]
            run = '''
            echo hi
            #^
            '''
        """.trimIndent())

        myFixture.configureFromExistingVirtualFile(bufToml.virtualFile)

        val element = findElementInEditor<PsiElement>()
        assert(MiseTomlPsiPatterns.inTaskRunStringOrArray.accepts(element)) {
            "Pattern does not accept element at caret:\n${myFixture.file.text}"
        }
    }

    fun `test inTaskRunStringOrArray on bare task table in include file with trailing slash dir include`() {
        myFixture.addFileToProject("mise.toml", """
            [task_config]
            includes = ["tasks/"]
        """.trimIndent())

        val bufToml = myFixture.addFileToProject("tasks/buf.toml", """
            [lint]
            run = '''
            echo hi
            #^
            '''
        """.trimIndent())

        myFixture.configureFromExistingVirtualFile(bufToml.virtualFile)

        val element = findElementInEditor<PsiElement>()
        assert(!MiseTomlPsiPatterns.inTaskRunStringOrArray.accepts(element)) {
            "Pattern unexpectedly accepts element at caret (directory include should not mark TOML as included):\n${myFixture.file.text}"
        }
    }

    fun `test inTaskRunStringOrArray on bare task table in tasks dir without includes`() {
        myFixture.addFileToProject("mise.toml", """
            [tools]
            node = "20"
        """.trimIndent())

        val bufToml = myFixture.addFileToProject("tasks/buf.toml", """
            [lint]
            run = '''
            echo hi
            #^
            '''
        """.trimIndent())

        myFixture.configureFromExistingVirtualFile(bufToml.virtualFile)

        val element = findElementInEditor<PsiElement>()
        assert(!MiseTomlPsiPatterns.inTaskRunStringOrArray.accepts(element)) {
            "Pattern unexpectedly accepts element at caret (no [task_config].includes):\n${myFixture.file.text}"
        }
    }

    private inline fun <reified T : PsiElement> testPattern(
        pattern: ElementPattern<T>,
        @Language("TOML") code: String,
        fileName: String = "mise.toml",
    ) {
        inlineFile(code, fileName)
        val element = findElementInEditor<T>()
        assert(pattern.accepts(element)) {
            """
                Pattern does not accept element at caret:
                pattern: $pattern
                $code
            """.trimIndent()
        }
    }
}
