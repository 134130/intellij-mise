package com.github.l34130.mise.core

import junit.framework.TestCase

class MiseTomlFileListenerTest : TestCase() {
    fun `test likely mise related file names include tool versions files`() {
        assertTrue(isLikelyMiseRelatedFileName(".tool-versions"))
        assertTrue(isLikelyMiseRelatedFileName(".tool-versions.local"))
        assertFalse(isLikelyMiseRelatedFileName(".tool-version"))
    }
}
