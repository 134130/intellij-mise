package com.github.l34130.mise.core

import com.intellij.openapi.components.service
import kotlinx.coroutines.runBlocking

internal class MiseTaskResolverStaleCacheTest : FileTestBase() {
    fun `test invalidation keeps cached tasks available`() {
        inlineFile(
            """
            [tasks.foo]
            run = "echo foo"
            """.trimIndent(),
            "mise.toml",
        )

        val resolver = project.service<MiseTaskResolver>()
        withMiseCommandLineExecutor {
            runBlocking { resolver.computeTasksFromSource() }
        }

        // Change file contents after cache warm
        inlineFile(
            """
            [tasks.bar]
            run = "echo bar"
            """.trimIndent(),
            "mise.toml",
        )

        resolver.markCacheAsStale()

        val cached = resolver.getCachedTasksOrEmptyList()
        assertNotEmpty(cached)
        val names = cached.map { it.name }
        assertTrue("foo" in names)
        assertTrue("bar" !in names)
    }

    fun `test refresh updates cached tasks`() {
        inlineFile(
            """
            [tasks.foo]
            run = "echo foo"
            """.trimIndent(),
            "mise.toml",
        )

        val resolver = project.service<MiseTaskResolver>()
        withMiseCommandLineExecutor {
            runBlocking { resolver.computeTasksFromSource() }
        }

        // Update file and invalidate
        inlineFile(
            """
            [tasks.bar]
            run = "echo bar"
            """.trimIndent(),
            "mise.toml",
        )
        resolver.markCacheAsStale()

        // Simulate async refresh (since queueTaskRefresh is disabled in unit tests)
        withMiseCommandLineExecutor {
            runBlocking { resolver.computeTasksFromSource() }
        }

        val cached = resolver.getCachedTasksOrEmptyList()
        assertNotEmpty(cached)
        val names = cached.map { it.name }
        assertTrue("bar" in names)
        assertTrue("foo" !in names)
    }
}
