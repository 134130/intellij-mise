package com.github.l34130.mise.core.command

import com.intellij.openapi.components.Service
import java.security.SecureRandom
import java.util.Base64

/**
 * Provides a stable cache session key for all plugin-owned mise invocations within a project session.
 *
 * Rationale:
 * - The plugin launches mise as separate subprocesses, not a persistent shell.
 * - mise env caching is session-keyed via `__MISE_ENV_CACHE_KEY`.
 * - Reusing one key across plugin subprocesses allows cache hits for repeated env resolution
 *   (including fnox-backed env plugins), while still rotating on IDE/project session restart.
 *
 * References:
 * - fnox mise integration: https://fnox.jdx.dev/guide/mise-integration.html
 * - mise env cache settings: https://mise.jdx.dev/configuration/settings.html#env_cache
 */
@Service(Service.Level.PROJECT)
class MiseEnvCacheKeyService {
    companion object {
        const val MISE_ENV_CACHE_KEY = "__MISE_ENV_CACHE_KEY"
    }

    private val rng = SecureRandom()

    val sessionKey: String = generate()

    private fun generate(): String {
        val key = ByteArray(32) // 256-bit
        rng.nextBytes(key)
        return Base64.getEncoder().encodeToString(key)
    }
}
