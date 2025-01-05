package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.MiseTomlTestBase
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns.inTaskDependsArray
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns.inTaskDependsString
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Document
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.lang.annotations.Language
import kotlin.math.min

class MiseTomlPsiPatternsTest : MiseTomlTestBase() {
    fun `test inTaskDependsArray`() = testPattern(inTaskDependsArray, """
        [tasks.foo]
        depends = ["bar", ""]
                         #^
    """)

    fun `test inTaskDependsArray with empty array`() = testPattern(inTaskDependsArray, """
        [tasks.foo]
        depends = []
                 #^
    """)

    fun `test inTaskDependsString with`() = testPattern(inTaskDependsString, """
        [tasks.foo]
        depends = "b"
                  #^
    """)

    fun `test inTaskDependsString with empty string`() = testPattern(inTaskDependsString, """
        [tasks.foo]
        depends = ""
                 #^
    """)

    private inline fun <reified T : PsiElement> testPattern(
        pattern: ElementPattern<T>,
        @Language("TOML") code: String,
        fileName: String = "mise.toml",
    ) {
        InlineFile(code, fileName)
        val element = findElementInEditor<T>()
        assertTrue(pattern.accepts(element))
    }

    private inline fun <reified T : PsiElement> findElementInEditor(marker: String = "^"): T = findElementInEditor(T::class.java, marker)

    private fun <T : PsiElement> findElementInEditor(
        psiClass: Class<T>,
        marker: String,
    ): T {
        val (element, data) = findElementWithDataAndOffsetInEditor(psiClass, marker)
        check(data.isEmpty()) { "Did not expect marker data" }
        return element
    }

    private inline fun <reified T : PsiElement> findElementAndDataInEditor(marker: String = "^"): Pair<T, String> {
        val (element, data) = findElementWithDataAndOffsetInEditor<T>(marker)
        return element to data
    }

    private inline fun <reified T : PsiElement> findElementWithDataAndOffsetInEditor(marker: String = "^"): Triple<T, String, Int> =
        findElementWithDataAndOffsetInEditor(T::class.java, marker)

    private fun <T : PsiElement> findElementWithDataAndOffsetInEditor(
        psiClass: Class<T>,
        marker: String,
    ): Triple<T, String, Int> {
        val elementsWithDataAndOffset = findElementsWithDataAndOffsetInEditor(psiClass, marker)
        check(elementsWithDataAndOffset.isNotEmpty()) { "No `$marker` marker:\n${myFixture.file.text}" }
        check(elementsWithDataAndOffset.size <= 1) { "More than one `$marker` marker:\n${myFixture.file.text}" }
        return elementsWithDataAndOffset.first()
    }

    private fun <T : PsiElement> findElementsWithDataAndOffsetInEditor(
        psiClass: Class<T>,
        marker: String,
    ): List<Triple<T, String, Int>> =
        findElementsWithDataAndOffsetInEditor(
            myFixture.file,
            myFixture.file.viewProvider.document!!,
            psiClass,
            marker,
        )

    private fun <T : PsiElement> findElementsWithDataAndOffsetInEditor(
        file: PsiFile,
        doc: Document,
        psiClass: Class<T>,
        marker: String,
    ): List<Triple<T, String, Int>> {
        val commentPrefix = LanguageCommenters.INSTANCE.forLanguage(file.language).lineCommentPrefix ?: "//"
        val caretMarker = "$commentPrefix$marker"
        val text = file.text
        val result = mutableListOf<Triple<T, String, Int>>()
        var markerOffset = -caretMarker.length
        while (true) {
            markerOffset = text.indexOf(caretMarker, markerOffset + caretMarker.length)
            if (markerOffset == -1) break
            val data =
                text
                    .drop(markerOffset)
                    .removePrefix(caretMarker)
                    .takeWhile { it != '\n' }
                    .trim()
            val markerEndOffset = markerOffset + caretMarker.length - 1
            val markerLine = doc.getLineNumber(markerEndOffset)
            val makerColumn = markerEndOffset - doc.getLineStartOffset(markerLine)
            val elementOffset = min(doc.getLineStartOffset(markerLine - 1) + makerColumn, doc.getLineEndOffset(markerLine - 1))
            val elementAtMarker = file.findElementAt(elementOffset)!!

            val element = PsiTreeUtil.getParentOfType(elementAtMarker, psiClass, false)
            if (element != null) {
                result.add(Triple(element, data, elementOffset))
            } else {
                val injectionElement =
                    InjectedLanguageManager
                        .getInstance(file.project)
                        .findInjectedElementAt(file, elementOffset)
                        ?.let { PsiTreeUtil.getParentOfType(it, psiClass, false) }
                        ?: error("No ${psiClass.simpleName} at ${elementAtMarker.text}")
                val injectionOffset =
                    (injectionElement.containingFile.virtualFile as VirtualFileWindow)
                        .documentWindow
                        .hostToInjected(elementOffset)
                result.add(Triple(injectionElement, data, injectionOffset))
            }
        }
        return result
    }
}
