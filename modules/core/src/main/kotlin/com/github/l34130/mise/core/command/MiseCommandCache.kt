package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.MiseTomlFileListener
import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.util.canSafelyInvokeAndWait
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.util.waitForProjectCache
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.application
import kotlinx.coroutines.*


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
        if (application.isUnitTestMode) {
            return
        }
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
     */
    private fun <T> getCached(
        cacheKey: MiseCacheKey<T>,
        compute: () -> T
    ): T {
        getIfCached(cacheKey)?.let { return it }

        // If the project cache isn't warm, this will wait for up to 10 seconds
        // for it to be ready. Even if it isn't ready, it just carries on and allows
        // what ever the compute is doing to succeed or fail without the project cache.
        val waitOnCache: () -> T = {
            project.waitForProjectCache()
            compute()
        }

        var savedResult: T? = null
        val entry = cacheService.getCachedCommand(cacheKey.key) {
            val result = waitOnCache()
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
     * - Fast path: Returns cached result synchronously (instant, no thread switching)
     * - Slow path: Executes compute() on appropriate thread with progress indicator
     *
     * Thread-safe: Can be called from EDT, background thread, or read-action thread.
     * The cache automatically handles threading based on calling context.
     *
     * Type-safe: Uses sealed class MiseCacheKey to guarantee key→type mapping at compile time.
     *
     * @param cacheKey Type-safe cache key (contains key string + progress title + type information)
     * @param compute Function to compute value on cache miss (must be thread-safe)
     * @return Cached or computed value of type T
     */
    fun <T> getCachedWithProgress(
        cacheKey: MiseCacheKey<T>,
        compute: () -> T
    ): T {
        // Fast path: check cache synchronously (instant, no threading overhead)
        getIfCached(cacheKey)?.let {
            logger.trace("getCachedWithProgress hit for key: ${cacheKey.key}")
            return it
        }

        logger.debug("getCachedWithProgress miss for key: ${cacheKey.key}")

        // Slow path: execute with appropriate threading strategy
        return when {
            application.isDispatchThread -> {
                logger.trace("getCachedWithProgress EDT detected, using modal progress")
                runWithModalProgressBlocking(project, cacheKey.progressTitle) {
                    getCached(cacheKey, compute)
                }
            }
            application.isReadAccessAllowed -> {
                // Background thread with read lock - use background progress
                logger.debug("getCachedWithProgress Background thread with read lock, using background progress")
                runBlocking {
                    withBackgroundProgress(project, cacheKey.progressTitle) {
                        withContext(Dispatchers.IO) {
                            getCached(cacheKey, compute)
                        }
                    }
                }
            }
            canSafelyInvokeAndWait() -> {
                // Background thread without read lock, but safe to dispatch to EDT
                logger.trace("getCachedWithProgress Background thread without read lock, safe to dispatch to EDT")
                var result: T? = null
                application.invokeAndWait {
                    runWithModalProgressBlocking(project, cacheKey.progressTitle) {
                        result = getCached(cacheKey, compute)
                    }
                }
                result ?: throw ProcessCanceledException()
            }

            else -> {
                // Background thread without read lock, UNSAFE to dispatch to EDT
                // Fall back to silent background execution without UI progress
                logger.trace("getCachedWithProgress Background thread without read lock in unsafe context (thread: ${Thread.currentThread().name}), executing without UI")
                getCached(cacheKey, compute)
            }
        }
    }

    // === Data Classes ===

    private data class CacheEntry(
        val value: Any
    )

    override fun dispose() {
    }
}
