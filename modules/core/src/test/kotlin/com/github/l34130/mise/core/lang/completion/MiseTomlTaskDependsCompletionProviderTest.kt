package com.github.l34130.mise.core.lang.completion

internal class MiseTomlTaskDependsCompletionProviderTest : MiseTomlCompletionTestBase() {
    fun `test completion inTaskDependsArray`() = testSingleCompletion("""
        [tasks.foo]
        
        [tasks.bar]
        depends = ["<caret>"]
    """, """
        [tasks.foo]
        
        [tasks.bar]
        depends = ["foo<caret>"]
    """)

    fun `test completion inTaskDependsPostArray`() = testSingleCompletion("""
        [tasks.foo]
        
        [tasks.bar]
        depends_post = ["<caret>"]
    """, """
        [tasks.foo]
        
        [tasks.bar]
        depends_post = ["foo<caret>"]
    """)
}
