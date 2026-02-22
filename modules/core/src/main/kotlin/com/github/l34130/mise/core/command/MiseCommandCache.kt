package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.MiseTomlFileListener
import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.util.waitForProjectCache
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Smart cache for mise commands with broadcast-based invalidation and proactive warming.
 *
 * Cache is invalidated when:
 * - Any mise config file changes
 * - Mise executable changes
 * - Relevant settings changes
 *
 * After invalidation, commonly used commands (env, ls) are proactively re-warmed in background
 * to avoid EDT blocking on next access.
 *
 * Usage:
 * ```kotlin
 * cache.getCached(key = "env:$workDir:$configEnvironment") {
 *     // Computation to run on cache miss
 *     miseCommandLine.runCommandLine<Map<String, String>>(listOf("env", "--json"))
 * }
 * ```
 */
@Service(Service.Level.PROJECT)
class MiseCommandCache(
    private val project: Project,
    private val cs: CoroutineScope
) : Disposable {
    private val logger = logger<MiseCommandCache>()
    private val cacheService = project.service<MiseCacheService>()

    override fun dispose() {}

    private data class CacheEntry(
        val value: Any
    )

    private companion object {
        /**
         * mise info commands should normally complete very quickly; this is a last-resort bound to detect
         * stuck process execution (e.g., WSL plumbing stalled).
         */
        private const val STUCK_COMMAND_TIMEOUT_SECS: Long = 10L
    }

    init {
        // Ensure VFS listener is initialized so config changes trigger cache invalidation.
        project.service<MiseTomlFileListener>()

        MiseProjectEventListener.subscribe(project, this) { event ->
            when (event.kind) {
                MiseProjectEvent.Kind.STARTUP -> warmCommonCommands()

                MiseProjectEvent.Kind.SETTINGS_CHANGED,
                MiseProjectEvent.Kind.EXECUTABLE_CHANGED,
                MiseProjectEvent.Kind.TOML_CHANGED -> {
                    logger.info("Mise project event ${event.kind}, invalidating entire cache")
                    cacheService.invalidateAllCommands()
                    warmCommonCommands()
                }

                else -> Unit
            }
        }
    }

    /**
     * Proactively warm commonly used commands in background after cache invalidation.
     * This prevents EDT blocking when UI components (tool window, etc.) refresh.
     */
    fun warmCommonCommands() {
        if (application.isUnitTestMode) return
        cs.launch(Dispatchers.IO) {
            try {
                logger.debug("Warming command cache for commonly-used commands")
                val workDir = project.guessMiseProjectPath()
                val configEnvironment =
                    project.service<com.github.l34130.mise.core.setting.MiseProjectSettings>().state.miseConfigEnvironment

                // Warm env vars (used by env customizers, tool window, etc.)
                MiseCommandLineHelper.getEnvVars(project, workDir, configEnvironment)

                // Warm dev tools (used by tool window, SDK setup, etc.)
                MiseCommandLineHelper.getDevTools(project, workDir, configEnvironment)

                logger.debug("Command cache warmed successfully")
            } catch (e: Exception) {
                logger.warn("Failed to warm command cache after invalidation", e)
            }
        }
    }

    /**
     * Get the cached value or compute it.
     * Uses Caffeine's built-in stampede protection.
     * Internal method - callers should use getCachedWithProgress for threading protection.
     *
     * If the project cache isn't warm, this will wait for up to [com.github.l34130.mise.core.util.PROJECT_CACHE_WAIT_TIMEOUT] seconds
     * for it to be ready. If it isn't ready within the timeout, it just carries on and allows
     * whatever the compute is doing to succeed or fail without the project cache.
     */
    private fun <T> getCached(
        cacheKey: MiseCacheKey<T>,
        compute: () -> T
    ): T {
        getIfCached(cacheKey)?.let { return it }


        val waitForProjectCacheThenCompute: () -> T = {
            project.waitForProjectCache()
            compute()
        }

        var savedResult: T? = null
        val entry = cacheService.getCachedCommand(cacheKey.key) {
            val result = waitForProjectCacheThenCompute()
            savedResult = result
            if (result is Result<*> && result.isFailure) {
                null
            } else {
                CacheEntry(result as Any)
            }
        }

        if (entry != null) {
            @Suppress("UNCHECKED_CAST")
            return entry.value as T
        }

        return savedResult ?: throw ProcessCanceledException()
    }

    /**
     * Get the cached value if present, without computing.
     * Returns null if the key is not cached.
     * This is a fast, synchronous operation suitable for fast-path optimization.
     * Internal method - callers should use getCachedWithProgress for threading protection.
     */
    private fun <T> getIfCached(cacheKey: MiseCacheKey<T>): T? {
        val entry = cacheService.getIfCachedCommand<CacheEntry>(cacheKey.key)
        @Suppress("UNCHECKED_CAST")
        return entry?.value as? T
    }

    /**
     * Get cached value with automatic fast-path optimization and threading protection.
     *
     * - Fast path (cache hit): Returns cached result synchronously (instant, no thread switching)
     * - Slow path (cache miss): Computes via coroutine on IO dispatcher, blocking for up to [STUCK_COMMAND_TIMEOUT_SECS] seconds,
     *                              throws ProcessCanceledException if computation can't complete within the timeout.
     *
     * Thread-safe: Can be called from EDT, background thread, or read-action thread.
     * Type-safe: Uses sealed class MiseCacheKey to guarantee key→type mapping at compile time.
     *
     * @param cacheKey Type-safe cache key (contains key string + progress title + type information)
     * @param compute Function to compute value on cache miss (must be thread-safe)
     * @return Cached or computed value of type T
     * @throws ProcessCanceledException
     */
    fun <T> getCachedWithProgress(
        cacheKey: MiseCacheKey<T>,
        compute: () -> T
    ): T {
        // Fast path: check cache synchronously (instant, no threading overhead)
        getIfCached(cacheKey)?.let {
            logger.trace { "getCachedWithProgress hit for key: ${cacheKey.key}" }
            return it
        }

        logger.trace { "getCachedWithProgress miss for key: ${cacheKey.key}" }

        // Compute on IO dispatcher to avoid deadlocks/re-entrancy when called from env customization / read-action contexts.
        // EDT callers are allowed to block, but only to *wait* (with modal progress), never to *compute*.
        return if (application.isDispatchThread) {
            logger.trace { "getCachedWithProgress EDT detected, using modal progress to wait for pooled computation" }
            runWithModalProgressBlocking(project, cacheKey.progressTitle) {
                try {
                    withTimeout(STUCK_COMMAND_TIMEOUT_SECS * 1000L) {
                        withContext(Dispatchers.IO) {
                            getCached(cacheKey, compute)
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    logger.warn("getCachedWithProgress timed out in ${STUCK_COMMAND_TIMEOUT_SECS}s for key: ${cacheKey.key}")
                    throw ProcessCanceledException()
                }
            }
        } else {
            val deferred: Deferred<T> = cs.async(Dispatchers.IO) { getCached(cacheKey, compute) }
            awaitDeferredOrCancel(cacheKey, deferred)
        }
    }

    /**
     * Waits for a deferred computation and normalizes timeout/interrupt/cancellation to ProcessCanceledException.
     */
    private fun <T> awaitDeferredOrCancel(cacheKey: MiseCacheKey<T>, deferred: Deferred<T>): T {
        val latch = CountDownLatch(1)
        var completionResult: Result<T>? = null

        deferred.invokeOnCompletion { throwable ->
            completionResult = if (throwable == null) {
                Result.success(deferred.getCompleted())
            } else {
                Result.failure(throwable)
            }
            latch.countDown()
        }

        return try {
            if (!latch.await(STUCK_COMMAND_TIMEOUT_SECS, TimeUnit.SECONDS)) {
                deferred.cancel()
                logger.warn("getCachedWithProgress timed out in ${STUCK_COMMAND_TIMEOUT_SECS}s for key: ${cacheKey.key}")
                throw ProcessCanceledException()
            }
            completionResult!!.getOrThrow()
            // CancellationException (project closed, etc.) → rethrown as-is for MiseEnvCustomizer to handle
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            deferred.cancel()
            throw ProcessCanceledException()
        }
    }
}
