package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.processOutput
import com.github.l34130.mise.core.util.markProjectCacheReady
import com.github.l34130.mise.core.withMiseCommandLineExecutor
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.concurrent.ConcurrentLinkedQueue

class MiseDevToolsCacheTest : BasePlatformTestCase() {
    override fun runInDispatchThread(): Boolean = false

    override fun setUp() {
        super.setUp()
        project.markProjectCacheReady()
        val cache = project.service<MiseCacheService>()
        cache.invalidateAllCommands()
        cache.invalidateAllExecutables()
        cache.getOrComputeExecutable(MiseExecutableManager.EXECUTABLE_KEY) {
            MiseExecutableInfo("mise", MiseVersion(2026, 1, 1))
        }
    }

    override fun tearDown() {
        try {
            project.service<MiseCacheService>().invalidateAllCommands()
            project.service<MiseCacheService>().invalidateAllExecutables()
        } finally {
            super.tearDown()
        }
    }

    fun `test cold lookup completes when former combined and local keys share a cache bucket`() {
        // These keys share a bucket in Caffeine's initially empty ConcurrentHashMap (#516).
        val workDir = "/tmp/" + "x".repeat(55) + "55"
        val calls = ConcurrentLinkedQueue<List<String>>()

        withMiseCommandLineExecutor(
            executor = { commandLine, _ ->
                calls.add(commandLine.parametersList.list)
                processOutput(stdout = "{}")
            },
        ) {
            assertTrue(MiseCommandLineHelper.getDevTools(project, workDir, "").getOrThrow().isEmpty())
        }

        assertEquals(listOf(listOf("ls", "--local", "--json"), listOf("ls", "--global", "--json")), calls.toList())
    }

    fun `test repeated lookup reuses scoped caches and invalidation refreshes results`() {
        val calls = ConcurrentLinkedQueue<String>()
        withMiseCommandLineExecutor(
            executor = { commandLine, _ ->
                val scope = commandLine.parametersList.list[1]
                calls.add(scope)
                val version = calls.count { it == scope }.toString()
                processOutput(stdout = toolJson(if (scope == "--local") "node" else "ruby", version))
            },
        ) {
            val first = MiseCommandLineHelper.getDevTools(project, "/project", "").getOrThrow()
            val second = MiseCommandLineHelper.getDevTools(project, "/project", "").getOrThrow()
            assertEquals(first, second)
            assertEquals(listOf("--local", "--global"), calls.toList())
            assertEquals(setOf(MiseDevToolName("node"), MiseDevToolName("ruby")), first.keys)
            assertTrue(first.values.all { it.single().version == "1" })

            project.service<MiseCacheService>().invalidateAllCommands()

            val refreshed = MiseCommandLineHelper.getDevTools(project, "/project", "").getOrThrow()
            assertEquals(listOf("--local", "--global", "--local", "--global"), calls.toList())
            assertEquals(first.keys, refreshed.keys)
            assertTrue(refreshed.values.all { it.single().version == "2" })
        }
    }

    fun `test failed scope is retried while successful scope stays cached`() {
        for (failedScope in listOf("--local", "--global")) {
            project.service<MiseCacheService>().invalidateAllCommands()
            val calls = ConcurrentLinkedQueue<String>()
            withMiseCommandLineExecutor(
                executor = { commandLine, _ ->
                    val scope = commandLine.parametersList.list[1]
                    calls.add(scope)
                    if (scope == failedScope && calls.count { it == scope } == 1) {
                        processOutput(stderr = "command failed", exitCode = 1)
                    } else {
                        processOutput(stdout = "{}")
                    }
                },
            ) {
                assertTrue(MiseCommandLineHelper.getDevTools(project, "/project", "").isFailure)
                assertTrue(MiseCommandLineHelper.getDevTools(project, "/project", "").isSuccess)
                assertEquals(listOf("--local", "--global", failedScope), calls.toList())
            }
        }
    }

    private fun toolJson(name: String, version: String): String =
        """{"$name":[{"version":"$version","install_path":"/tools/$name/$version","installed":true,"active":true}]}"""
}
