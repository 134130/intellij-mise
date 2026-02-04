package com.github.l34130.mise.core.command

/**
 * Type-safe cache keys for mise commands.
 *
 * Each sealed class variant encodes both the cache key pattern and the result type,
 * providing compile-time guarantees that cache keys map to correct types.
 *
 * This eliminates the need for string-based key construction and provides:
 * - Compile-time type safety (impossible to use wrong type for a key)
 * - No string concatenation bugs
 * - Co-located progress titles with keys
 * - Better IDE support (autocomplete, refactoring)
 * - Exhaustive pattern matching capabilities
 *
 * Example usage:
 * ```kotlin
 * val cacheKey = MiseCacheKey.EnvVars(workDir, configEnvironment)
 * val result: Result<Map<String, String>> = cache.getCachedWithProgress(cacheKey) {
 *     // compute function
 * }
 * // Type is guaranteed by sealed class hierarchy - compiler enforces it!
 * ```
 */
sealed class MiseCacheKey<out T> {
    /**
     * The string key used for cache storage.
     * Format varies by command type.
     */
    abstract val key: String

    /**
     * The progress dialog title shown during cache miss.
     */
    abstract val progressTitle: String

    /**
     * Cache key for mise env command.
     *
     * Key format: `env:{workDir}:{configEnvironment}`
     * Result type: `Result<Map<String, String>>`
     *
     * Example: `env:/home/user/project:production`
     */
    data class EnvVars(
        val workDir: String,
        val configEnvironment: String?
    ) : MiseCacheKey<Result<Map<String, String>>>() {
        override val key = "env:$workDir:$configEnvironment"
        override val progressTitle = "Loading Mise Environment Variables"
    }

    /**
     * Cache key for mise env --json-extended command.
     *
     * Key format: `env-extended:{workDir}:{configEnvironment}`
     * Result type: `Result<Map<String, MiseEnvExtended>>`
     *
     * Example: `env-extended:/home/user/project:production`
     */
    data class EnvVarsExtended(
        val workDir: String,
        val configEnvironment: String?
    ) : MiseCacheKey<Result<Map<String, MiseEnvExtended>>>() {
        override val key = "env-extended:$workDir:$configEnvironment"
        override val progressTitle = "Loading Mise Environment Details"
    }

    /**
     * Cache key for mise ls command.
     *
     * Key format: `ls:{scope}:{workDir}:{configEnvironment}`
     * Result type: `Result<Map<MiseDevToolName, List<MiseDevTool>>>`
     *
     * Example: `ls:combined:/home/user/project:production`
     */
    data class DevTools(
        val workDir: String,
        val configEnvironment: String?,
        val scope: MiseDevToolsScope
    ) : MiseCacheKey<Result<Map<MiseDevToolName, List<MiseDevTool>>>>() {
        override val key = "ls:${scope.cacheKeySegment}:$workDir:$configEnvironment"
        override val progressTitle =
            when (scope) {
                MiseDevToolsScope.LOCAL -> "Loading Mise Dev Tools (Local)"
                MiseDevToolsScope.GLOBAL -> "Loading Mise Dev Tools (Global)"
                MiseDevToolsScope.COMBINED -> "Loading Mise Dev Tools"
            }
    }

    /**
     * Cache key for mise which command.
     *
     * Key format: `which:{workDir}:{configEnvironment}`
     * Result type: `Result<String>`
     *
     * Example: `ls:/home/user/project:production`
     */
    data class WhichBin(
        val commonBinName: String,
        val workDir: String,
        val configEnvironment: String?
    ) : MiseCacheKey<Result<String>>() {
        override val key = "which:$commonBinName:$workDir:$configEnvironment"
        override val progressTitle = "Finding $commonBinName"
    }
}
