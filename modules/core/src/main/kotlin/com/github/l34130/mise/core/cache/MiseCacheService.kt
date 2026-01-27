package com.github.l34130.mise.core.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.l34130.mise.core.command.MiseExecutableInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking

/**
 * Centralized caching service for all mise-related data using Caffeine.
 *
 * Provides two specialized caches:
 * - Command cache: For mise command results (size-based eviction, 500 entries max)
 * - Executable cache: For resolved executable paths (small, rarely evicted)
 *
 * All mise caching should go through this service for consistent behavior and observability.
 */
@Service(Service.Level.PROJECT)
class MiseCacheService(private val project: Project) {
    private val logger = logger<MiseCacheService>()

    /**
     * Cache for mise command results (env, list, etc.)
     * Large capacity with LRU eviction for frequently used command results.
     */
    private val commandCache: Cache<String, Any?> = Caffeine.newBuilder()
        .maximumSize(500)
        .removalListener { key: String?, _: Any?, cause ->
            if (cause == RemovalCause.SIZE) {
                logger.debug("Command cache entry evicted due to size limit: $key")
            }
        }
        .build()

    /**
     * Cache for executable info (resolved path + version)
     * Small capacity - typically only 2-3 entries per project.
     */
    private val executableCache: Cache<String, MiseExecutableInfo> = Caffeine.newBuilder()
        .maximumSize(10)
        .removalListener { key: String?, _: MiseExecutableInfo?, cause ->
            if (cause == RemovalCause.SIZE) {
                logger.warn("Executable cache entry evicted (unexpected): $key")
            }
        }
        .build()

    // === Command Cache Operations ===

    /**
     * Get a cached command result or compute it.
     * Uses Caffeine's built-in stampede protection - only one computation per key.
     */
    fun <T> getCachedCommand(key: String, compute: () -> T?): T? {
        @Suppress("UNCHECKED_CAST")
        return commandCache.get(key) {
            logger.debug("Command cache miss: $key")
            val result = compute()
            if (result != null) {
                logger.debug("Command cached: $key")
            }
            result as Any?
        } as T?
    }

        /**
     * Get a cached command result if present, without computing.
     * Returns null if the key is not cached.
     * This is a fast, synchronous operation suitable for fast-path optimization.
     */
    fun <T> getIfCachedCommand(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return commandCache.getIfPresent(key) as? T
    }

    /**
     * Invalidate all command cache entries.
     */
    fun invalidateAllCommands() {
        commandCache.invalidateAll()
        logger.info("All command cache entries invalidated")
    }

    // === Executable Cache Operations ===

    /**
     * Get a cached executable path or compute it.
     * Uses Caffeine's built-in stampede protection - only one computation per key.
     *
     * EDT Safety: If called from EDT and cache is cold, runs computation on background thread
     * with modal progress dialog to avoid blocking the UI.
     */
    fun getOrComputeExecutable(key: String, compute: () -> MiseExecutableInfo): MiseExecutableInfo {
        return executableCache.get(key) {
            logger.debug("Executable cache miss: $key")

            val result = if (ApplicationManager.getApplication().isDispatchThread) {
                // On EDT - run with modal progress dialog on background thread
                logger.debug("Cache computation triggered from EDT, showing progress dialog")
                runWithModalProgressBlocking(project, "Detecting mise Executable") {
                    compute()
                }
            } else {
                // Not on EDT - run directly
                compute()
            }

            logger.debug("Executable cached: $key")
            result
        }
    }

    fun getCachedExecutable(key: String): MiseExecutableInfo? {
        return executableCache.getIfPresent(key)
    }

    /**
     * Invalidate all executable cache entries.
     */
    fun invalidateAllExecutables() {
        executableCache.invalidateAll()
        logger.info("All executable cache entries invalidated")
    }
}
