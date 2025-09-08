@file:Suppress("ktlint")

package com.github.l34130.mise.core.lang.completion

internal class MiseTomlTaskDependsCompletionProviderTest : MiseTomlCompletionTestBase() {
    fun `test completion inTaskDependsArray`() = testSingleCompletion(
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

    fun `test completion inTaskDependsPostArray`() = testSingleCompletion(
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

    fun `test completion inTaskWaitForArray`() = testSingleCompletion(
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

    fun `test completion inTaskDependsArray with items`() = testSingleCompletion(
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

    fun `test completion inTaskDependsString`() = testSingleCompletion(
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

    fun `test completion inTaskDependsArray with tasks table`() = testSingleCompletion(
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

    fun `test completion inTaskDependsArray with items with tasks table`() = testSingleCompletion(
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

    fun `test completion inTaskDependsString with tasks table`() = testSingleCompletion(
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
}
