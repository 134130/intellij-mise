package com.github.l34130.mise.core.lang.completion

internal class MiseFileTaskCompletionProviderTest : MiseShCompletionTestBase() {
    fun `test completion alias`() = testSingleCompletion("""
        #!/usr/bin/env bash
        #MISE al<caret>
    """, """
        #!/usr/bin/env bash
        #MISE alias<caret>
    """)

    fun `test completion depends`() = testCompletion("depends", """
        #!/usr/bin/env bash
        #MISE dep<caret>
    """, """
        #!/usr/bin/env bash
        #MISE depends<caret>
    """)

    fun `test completion wait_for`() = testSingleCompletion("""
        #!/usr/bin/env bash
        #MISE wai<caret>
    """, """
        #!/usr/bin/env bash
        #MISE wait_for<caret>
    """)
}
