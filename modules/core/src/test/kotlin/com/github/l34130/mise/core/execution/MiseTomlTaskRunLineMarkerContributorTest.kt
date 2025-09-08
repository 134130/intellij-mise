@file:Suppress("ktlint")
package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.FileTestBase
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlFile

class MiseTomlTaskRunLineMarkerContributorTest : FileTestBase() {
    fun `test run line marker`()  {
        InlineTomlFile(
            """
            [tasks.foo]
            run = "echo foo"
            
            [tasks]
            "bar" = { run = "echo bar" }
            """.trimIndent(),
        )

        myFixture.doHighlighting()

        val lineMarkers = DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project)
        assertEquals(2, lineMarkers.size)
    }

    private fun InlineTomlFile(@Language("TOML") code: String, fileName: String = "mise.toml"): TomlFile =
        inlineFile(code, fileName) as TomlFile
}
