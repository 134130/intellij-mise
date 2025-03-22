@file:Suppress("ktlint")

package com.github.l34130.mise.sh.lang.completion

internal class MiseFileTaskCompletionProviderTest : MiseShCompletionTestBase() {
    fun `test completion alias`() = testSingleCompletion(
        """
        #!/usr/bin/env bash
        #MISE al<caret>
        """.trimIndent(),
        """
        #!/usr/bin/env bash
        #MISE alias<caret>
        """.trimIndent()
    )

    fun `test completion depends`() = testCompletion("depends",
        """
        #!/usr/bin/env bash
        #MISE dep<caret>
        """.trimIndent(),
        """
        #!/usr/bin/env bash
        #MISE depends<caret>
        """.trimIndent()
    )

    fun `test completion wait_for`() = testSingleCompletion(
        """
        #!/usr/bin/env bash
        #MISE wai<caret>
        """.trimIndent(),
        """
        #!/usr/bin/env bash
        #MISE wait_for<caret>
        """.trimIndent()
    )
}
