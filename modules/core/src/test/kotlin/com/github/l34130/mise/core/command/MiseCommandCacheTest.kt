package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.util.markProjectCacheReady
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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

    fun `test in-flight cache prevents duplicate compute`() {
        project.markProjectCacheReady()

        val cacheService = project.service<MiseCacheService>()
        cacheService.invalidateAllCommands()

        val calls = AtomicInteger(0)
        val started = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val key = "in-flight-test"
            val first = executor.submit<String?> {
                cacheService.getCachedCommand(key) {
                    calls.incrementAndGet()
                    started.countDown()
                    assertTrue(finish.await(2, TimeUnit.SECONDS))
                    "value"
                }
            }

            assertTrue(started.await(2, TimeUnit.SECONDS))

            val second = executor.submit<String?> {
                cacheService.getCachedCommand(key) { "other" }
            }

            finish.countDown()

            assertEquals("value", first.get(2, TimeUnit.SECONDS))
            assertEquals("value", second.get(2, TimeUnit.SECONDS))
            assertEquals(1, calls.get())
        } finally {
            executor.shutdownNow()
        }
    }

    fun `test nested cache compute avoids recursive update`() {
        project.markProjectCacheReady()

        val cacheService = project.service<MiseCacheService>()
        cacheService.invalidateAllCommands()

        // "Aa" and "BB" intentionally collide in String.hashCode().
        val parentKey = "Aa"
        val childKey = "BB"

        val result = cacheService.getCachedCommand(parentKey) {
            val child = cacheService.getCachedCommand(childKey) {
                TimeUnit.MILLISECONDS.sleep(50)
                "child"
            }
            "parent:$child"
        }

        assertEquals("parent:child", result)
    }
}
