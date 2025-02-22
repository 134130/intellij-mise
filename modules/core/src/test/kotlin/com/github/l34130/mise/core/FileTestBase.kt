package com.github.l34130.mise.core

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import kotlin.math.min

abstract class FileTestBase : BasePlatformTestCase() {
    protected fun inlineFile(
        text: String,
        fileName: String,
    ): PsiFile {
        val file = myFixture.configureByText(fileName, text.trimIndent())
        runBlocking {
            project.service<MiseService>().refresh()
            file.virtualFile.refresh(false, false)
        }
        return file
    }

    protected inline fun <reified T : PsiElement> findElementInEditor(marker: String = "^"): T = findElementInEditor(T::class.java, marker)

    protected fun <T : PsiElement> findElementInEditor(
        psiClass: Class<T>,
        marker: String,
    ): T {
        val (element, data) = findElementWithDataAndOffsetInEditor(psiClass, marker)
        check(data.isEmpty()) { "Did not expect marker data" }
        return element
    }

    protected inline fun <reified T : PsiElement> findElementAndDataInEditor(marker: String = "^"): Pair<T, String> {
        val (element, data) = findElementWithDataAndOffsetInEditor<T>(marker)
        return element to data
    }

    protected inline fun <reified T : PsiElement> findElementWithDataAndOffsetInEditor(marker: String = "^"): Triple<T, String, Int> =
        findElementWithDataAndOffsetInEditor(T::class.java, marker)

    protected fun <T : PsiElement> findElementWithDataAndOffsetInEditor(
        psiClass: Class<T>,
        marker: String,
    ): Triple<T, String, Int> {
        val elementsWithDataAndOffset = findElementsWithDataAndOffsetInEditor(psiClass, marker)
        check(elementsWithDataAndOffset.isNotEmpty()) { "No `$marker` marker:\n${myFixture.file.text}" }
        check(elementsWithDataAndOffset.size <= 1) { "More than one `$marker` marker:\n${myFixture.file.text}" }
        return elementsWithDataAndOffset.first()
    }

    protected fun <T : PsiElement> findElementsWithDataAndOffsetInEditor(
        psiClass: Class<T>,
        marker: String,
    ): List<Triple<T, String, Int>> =
        findElementsWithDataAndOffsetInEditor(
            myFixture.file,
            myFixture.file.viewProvider.document!!,
            psiClass,
            marker,
        )

    protected fun <T : PsiElement> findElementsWithDataAndOffsetInEditor(
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
