package com.github.l34130.mise.sh.lang.completion

import com.github.l34130.mise.sh.FileTestBase
import com.intellij.codeInsight.lookup.LookupElement
import org.intellij.lang.annotations.Language

/**
 * Base class for Shell Script completion tests.
 *
 * Note: This class shares similar structure with [com.github.l34130.mise.core.lang.completion.MiseTomlCompletionTestBase]
 * in the core module. Key differences:
 * - This class uses @Language("Shell Script") annotations and has no cache warming
 * - TOML version uses @Language("TOML") and warms the task cache before tests (TOML completion needs task data)
 * - This class provides a default completionChar parameter in testCompletion()
 *
 * If modifying this class, consider whether similar changes apply to the TOML version.
 */
internal abstract class MiseShCompletionTestBase : FileTestBase() {
    protected fun testSingleCompletion(
        @Language("Shell Script") before: String,
        @Language("Shell Script") after: String,
    ) {
        check(hasCaretMarker(before)) { "Please, add `/*caret*/` or `<caret>` marker to\n$before" }
        check(hasCaretMarker(after)) { "Please, add `/*caret*/` or `<caret>` marker to\n$after" }
        checkByText(before.trimIndent(), after.trimIndent()) { executeSoloCompletion() }
    }

    protected fun testCompletion(
        lookupString: String,
        @Language("Shell Script") before: String,
        @Language("Shell Script") after: String,
        completionChar: Char = '\n',
    ) {
        check(hasCaretMarker(before)) { "Please, add `/*caret*/` or `<caret>` marker to\n$before" }
        check(hasCaretMarker(after)) { "Please, add `/*caret*/` or `<caret>` marker to\n$after" }

        checkByText(before.trimIndent(), after.trimIndent()) {
            val items = myFixture.completeBasic() ?: return@checkByText
            val lookupItem =
                items.find { it.lookupString == lookupString }
                    ?: error("No lookup item found: $lookupString\nitems: ${items.joinToString { it.lookupString }}")
            myFixture.lookup.currentItem = lookupItem
            myFixture.type(completionChar)
        }
    }

    protected fun checkByText(
        code: String,
        after: String,
        action: () -> Unit,
    ) {
        inlineFile(code, "shellscript")
        action()
        myFixture.checkResult(replaceCaretMarker(after))
    }

    private fun executeSoloCompletion() {
        val lookups = myFixture.completeBasic()

        if (lookups != null) {
            if (lookups.size == 1) {
                // for cases like `frob/*caret*/nicate()`,
                // completion won't be selected automatically.
                myFixture.type('\n')
                return
            }

            fun LookupElement.debug(): String = "$lookupString ($psiElement)"
            error(
                "Expected a single completion, but got ${lookups.size}\n" +
                    lookups.joinToString("\n") { it.debug() },
            )
        }
    }

    private fun replaceCaretMarker(text: String): String = text.replace("/*caret*/", "<caret>")

    private fun hasCaretMarker(text: String): Boolean = "<caret>" in text || "/*caret*/" in text
}
