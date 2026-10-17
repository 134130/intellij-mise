package com.github.l34130.mise.core.command

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiseCacheKeyTest : BasePlatformTestCase() {

    @Test
    fun `DevTools key includes scope segment`() {
        assertEquals("ls:local:/project:production", MiseCacheKey.DevTools("/project", "production", MiseDevToolsScope.LOCAL).key)
    }

    @Test
    fun `DevTools key uses global segment for GLOBAL scope`() {
        assertEquals("ls:global:/project:production", MiseCacheKey.DevTools("/project", "production", MiseDevToolsScope.GLOBAL).key)
    }

    @Test
    fun `DevTools key supports null environment`() {
        assertEquals("ls:local:/project:null", MiseCacheKey.DevTools("/project", null, MiseDevToolsScope.LOCAL).key)
    }

    @Test
    fun `DevTools keys for local and global scopes are distinct`() {
        val local = MiseCacheKey.DevTools("/project", null, MiseDevToolsScope.LOCAL).key
        val global = MiseCacheKey.DevTools("/project", null, MiseDevToolsScope.GLOBAL).key
        assertFalse(local == global)
    }

    @Test
    fun `WhichBin key includes binary name workDir and environment`() {
        assertEquals("which:python:/project:production", MiseCacheKey.WhichBin("python", "/project", "production").key)
    }

    @Test
    fun `WhichBin keys for different binary names are distinct`() {
        val python = MiseCacheKey.WhichBin("python", "/project", null).key
        val node = MiseCacheKey.WhichBin("node", "/project", null).key
        assertFalse("Keys for different binaries must differ", python == node)
    }
}
