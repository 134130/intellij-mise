package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.FileTestBase
import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.withMiseCommandLineExecutor
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language

/**
 * Base class for TOML completion tests.
 *
 * Note: This class shares similar structure with [com.github.l34130.mise.sh.lang.completion.MiseShCompletionTestBase]
 * in the sh module. Key differences:
 * - This class uses @Language("TOML") annotations and warms the task cache before tests
 * - Shell version uses @Language("Shell Script") and has no cache warming (shell scripts don't use task cache)
 *
 * If modifying this class, consider whether similar changes apply to the sh version.
 */
internal abstract class MiseTomlCompletionTestBase : FileTestBase() {
    protected fun testSingleCompletion(
        @Language("TOML") before: String,
        @Language("TOML") after: String,
    ) {
        check(hasCaretMarker(before)) { "Please, add `/*caret*/` or `<caret>` marker to\n$before" }
        check(hasCaretMarker(after)) { "Please, add `/*caret*/` or `<caret>` marker to\n$after" }
        checkByText(before.trimIndent(), after.trimIndent()) { executeSoloCompletion() }
    }

    protected fun testCompletion(
        lookupString: String,
        @Language("TOML") before: String,
        @Language("TOML") after: String,
        completionChar: Char,
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
        // Clear any stale cache from previous tests to ensure isolation
        project.service<MiseTaskResolver>().markCacheAsStale()

        inlineFile(code, "mise.toml")
        withMiseCommandLineExecutor {
            runBlocking { project.service<MiseTaskResolver>().computeTasksFromSource() }
        }
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
