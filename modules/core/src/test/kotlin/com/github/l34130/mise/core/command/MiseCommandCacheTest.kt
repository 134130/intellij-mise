package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.util.markProjectCacheReady
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.concurrent.atomic.AtomicInteger

class MiseCommandCacheTest : BasePlatformTestCase() {
    fun `test failure results are not cached`() {
        project.markProjectCacheReady()

        val cache = project.service<MiseCommandCache>()
        val calls = AtomicInteger(0)
        val cacheKey = MiseCacheKey.EnvVars("test-workdir", null)
        val compute = {
            calls.incrementAndGet()
            Result.failure<Map<String, String>>(RuntimeException("boom"))
        }

        val first = cache.getCachedWithProgress(cacheKey, compute)
        val second = cache.getCachedWithProgress(cacheKey, compute)

        assertTrue(first.isFailure)
        assertTrue(second.isFailure)
        assertEquals(2, calls.get())
    }
}
