@file:Suppress("ktlint")

package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.FileTestBase
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns.inTaskDependsArray
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns.inTaskDependsString
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns.inToolsVersionValue
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns.inToolsTableKey
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

    fun `test inToolsVersionValue with string value`() = testPattern(inToolsVersionValue, """
        [tools]
        nodejs = "22"
                 #^
    """)

    fun `test inToolsVersionValue with empty string`() = testPattern(inToolsVersionValue, """
        [tools]
        nodejs = ""
                #^
    """)

    fun `test inToolsVersionValue with array element`() = testPattern(inToolsVersionValue, """
        [tools]
        python = ["3.11", ""]
                          #^
    """)

    fun `test inToolsVersionValue with inline table version`() = testPattern(inToolsVersionValue, """
        [tools]
        go = {version = "1"}
                        #^
    """)

    fun `test inToolsVersionValue with tool specific table`() = testPattern(inToolsVersionValue, """
        [tools.python]
        version = "3"
                  #^
    """)

    fun `test inToolsTableKey`() = testPattern(inToolsTableKey, """
        [tools]
        nodejs = "22.12.0"
        #^
    """)

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
