package com.github.l34130.mise.core.command

import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiseDevToolsScopeTest : LightPlatformTestCase() {

    @Test
    fun `LOCAL commandFlag is --local`() {
        assertEquals("--local", MiseDevToolsScope.LOCAL.commandFlag)
    }

    @Test
    fun `GLOBAL commandFlag is --global`() {
        assertEquals("--global", MiseDevToolsScope.GLOBAL.commandFlag)
    }

    @Test
    fun `cacheKeySegments are distinct`() {
        val scopes = listOf(MiseDevToolsScope.LOCAL, MiseDevToolsScope.GLOBAL)
        val segments = scopes.map { it.cacheKeySegment }.toSet()
        assertEquals(scopes.size, segments.size)
    }
}
