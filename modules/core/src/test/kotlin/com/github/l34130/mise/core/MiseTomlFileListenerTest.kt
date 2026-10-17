package com.github.l34130.mise.core

import junit.framework.TestCase

class MiseTomlFileListenerTest : TestCase() {
    fun `test package manager inputs invalidate mise commands`() {
        assertTrue(isLikelyMiseRelatedFileName("package.json"))
        assertTrue(isLikelyMiseRelatedFileName("pnpm-lock.yaml"))
        assertFalse(isLikelyMiseRelatedFileName("package-lock.json"))
        assertFalse(isLikelyMiseRelatedFileName("package.json.bak"))
    }

    fun `test likely mise-related file names accept tool-versions files and reject near misses`() {
        assertTrue(isLikelyMiseRelatedFileName(".tool-versions"))
        assertTrue(isLikelyMiseRelatedFileName(".tool-versions.local"))
        assertFalse(isLikelyMiseRelatedFileName(".tool-version"))
    }

    fun `test likely mise-related file names accept environment configs and reject near misses`() {
        assertTrue(isLikelyMiseRelatedFileName("mise.dev.toml"))
        assertTrue(isLikelyMiseRelatedFileName(".mise.dev.local.toml"))
        assertFalse(isLikelyMiseRelatedFileName("mise.toml.bak"))
        assertFalse(isLikelyMiseRelatedFileName("notmise.dev.toml"))
    }
}
