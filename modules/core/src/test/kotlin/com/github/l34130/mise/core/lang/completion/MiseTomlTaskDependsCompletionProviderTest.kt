package com.github.l34130.mise.core.lang.completion

import com.github.l34130.mise.core.MiseTaskResolver
import com.intellij.openapi.components.service

internal class MiseTomlTaskDependsCompletionProviderTest : MiseTomlCompletionTestBase() {
    override fun setUp() {
        super.setUp()
        // Ensure clean cache state before each test
        project.service<MiseTaskResolver>().markCacheAsStale()
    }

    fun `test completion inTaskDependsArray`() =
        testSingleCompletion(
            """
            [tasks.foo]
            
            [tasks.bar]
            depends = ["<caret>"]
            """.trimIndent(),
            """
            [tasks.foo]
            
            [tasks.bar]
            depends = ["foo<caret>"]
            """.trimIndent(),
        )

    fun `test completion inTaskDependsPostArray`() =
        testSingleCompletion(
            """
            [tasks.foo]
            
            [tasks.bar]
            depends_post = ["<caret>"]
            """.trimIndent(),
            """
            [tasks.foo]
            
            [tasks.bar]
            depends_post = ["foo<caret>"]
            """.trimIndent(),
        )

    fun `test completion inTaskWaitForArray`() =
        testSingleCompletion(
            """
            [tasks.foo]
            
            [tasks.bar]
            wait_for = ["<caret>"]
            """.trimIndent(),
            """
            [tasks.foo]
            
            [tasks.bar]
            wait_for = ["foo<caret>"]
            """.trimIndent(),
        )

    fun `test completion inTaskDependsArray with items`() =
        testSingleCompletion(
            """
            [tasks.foo]
            
            [tasks.bar]
            depends = ["another", "<caret>"]
            """.trimIndent(),
            """
            [tasks.foo]
            
            [tasks.bar]
            depends = ["another", "foo<caret>"]
            """.trimIndent(),
        )

    fun `test completion inTaskDependsString`() =
        testSingleCompletion(
            """
            [tasks.foo]
            
            [tasks.bar]
            depends = "<caret>"
            """.trimIndent(),
            """
            [tasks.foo]
            
            [tasks.bar]
            depends = "foo<caret>"
            """.trimIndent(),
        )

    fun `test completion inTaskDependsArray with tasks table`() =
        testSingleCompletion(
            """
            [tasks]
            foo = {}
            bar = { depends = ["<caret>"] }
            """.trimIndent(),
            """
            [tasks]
            foo = {}
            bar = { depends = ["foo<caret>"] }
            """.trimIndent(),
        )

    fun `test completion inTaskDependsArray with items with tasks table`() =
        testSingleCompletion(
            """
            [tasks]
            foo = {}
            bar = { depends = ["another", "<caret>"] }
            """.trimIndent(),
            """
            [tasks]
            foo = {}
            bar = { depends = ["another", "foo<caret>"] }
            """.trimIndent(),
        )

    fun `test completion inTaskDependsString with tasks table`() =
        testSingleCompletion(
            """
            [tasks]
            foo = {}
            bar = { depends = "<caret>" }
            """.trimIndent(),
            """
            [tasks]
            foo = {}
            bar = { depends = "foo<caret>" }
            """.trimIndent(),
        )

    fun `test completion with multiple tasks`() =
        testCompletion(
            "bar",
            """
            [tasks.foo]
            run = "echo foo"

            [tasks.bar]
            run = "echo bar"

            [tasks.baz]
            depends = ["<caret>"]
            """.trimIndent(),
            """
            [tasks.foo]
            run = "echo foo"

            [tasks.bar]
            run = "echo bar"

            [tasks.baz]
            depends = ["bar<caret>"]
            """.trimIndent(),
            '\n',
        )

    fun `test completion returns empty when cache is cold`() {
        inlineFile(
            """
            [tasks.foo]
            run = "echo foo"

            [tasks.bar]
            depends = ["<caret>"]
            """.trimIndent(),
            "mise.toml",
        )

        // Ensure cache is cold by invalidating it
        project.service<MiseTaskResolver>().markCacheAsStale()

        // Trigger completion
        myFixture.completeBasic()

        // With cold cache, should return no completions
        val completions = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(completions.isEmpty())
    }
}
