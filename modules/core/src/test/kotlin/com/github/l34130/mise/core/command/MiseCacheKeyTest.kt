package com.github.l34130.mise.core.command

import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiseCacheKeyTest : LightPlatformTestCase() {

    @Test
    fun `DevTools key includes scope segment`() {
        assertEquals("ls:local:/project:production", MiseCacheKey.DevTools("/project", "production", MiseDevToolsScope.LOCAL).key)
    }

    @Test
    fun `DevTools key uses global segment for GLOBAL scope`() {
        assertEquals("ls:global:/project:production", MiseCacheKey.DevTools("/project", "production", MiseDevToolsScope.GLOBAL).key)
    }

    @Test
    fun `DevTools key uses combined segment for null scope`() {
        assertEquals("ls:combined:/project:null", MiseCacheKey.DevTools("/project", null, null).key)
    }

    @Test
    fun `DevTools default scope is null (combined)`() {
        val key = MiseCacheKey.DevTools("/project", null)
        assertNull(key.scope)
        assertEquals("ls:combined:/project:null", key.key)
    }

    @Test
    fun `DevTools keys for all three scopes are distinct`() {
        val local = MiseCacheKey.DevTools("/project", null, MiseDevToolsScope.LOCAL).key
        val global = MiseCacheKey.DevTools("/project", null, MiseDevToolsScope.GLOBAL).key
        val combined = MiseCacheKey.DevTools("/project", null, null).key
        assertEquals(3, setOf(local, global, combined).size)
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
