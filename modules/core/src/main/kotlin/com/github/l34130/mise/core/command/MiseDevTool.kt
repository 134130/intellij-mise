package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.wsl.WslPathUtils

data class MiseDevTool(
    val version: String,
    val requestedVersion: String? = null,
    val installPath: String,
    val installed: Boolean,
    val active: Boolean,
    val source: MiseSource? = null,
) {
    // Populated by command helpers to preserve WSL context for path conversion.
    var miseExecutablePath: String? = null

    val resolvedVersion: String
        get() = version.takeIf { it.isNotBlank() } ?: requestedVersionDisplay.orEmpty()

    val displayVersion: String
        get() = requestedVersionDisplay ?: resolvedVersion

    val displayVersionWithResolved: String
        get() {
            val requested = requestedVersionDisplay
            val resolved = version.takeIf { it.isNotBlank() }
            return when {
                requested == null -> resolved.orEmpty()
                resolved == null || resolved == requested -> requested
                else -> "$requested ($resolved)"
            }
        }

    private val requestedVersionDisplay: String?
        get() = requestedVersion?.takeIf { it.isNotBlank() }

    private var resolvedInstallPathCache: String? = null
    private var resolvedInstallPathCacheKey: String? = null

    val resolvedInstallPath: String
        get() {
            val key = miseExecutablePath
            val cached = resolvedInstallPathCache
            if (cached != null && resolvedInstallPathCacheKey == key) return cached

            val resolved = WslPathUtils.maybeConvertUnixPathToWsl(installPath, key)
            resolvedInstallPathCacheKey = key
            resolvedInstallPathCache = resolved
            return resolved
        }
}
