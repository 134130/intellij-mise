package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.ShimUtils
import com.github.l34130.mise.core.wsl.WslPathUtils

data class MiseDevTool(
    val version: String,
    val requestedVersion: String? = null,
    val installPath: String,
    val installed: Boolean,
    val active: Boolean,
    val source: MiseSource? = null,
    val wslDistributionMsId: String? = null
) {
    val resolvedVersion: String
        get() = version.takeIf { it.isNotBlank() } ?: requestedVersion?.takeIf { it.isNotBlank() }.orEmpty()

    val displayVersion: String
        get() = requestedVersion?.takeIf { it.isNotBlank() } ?: resolvedVersion

    val displayVersionWithResolved: String
        get() {
            val requested = requestedVersion?.takeIf { it.isNotBlank() }
            val resolved = version.takeIf { it.isNotBlank() }
            return when {
                requested == null -> resolved.orEmpty()
                resolved == null || resolved == requested -> requested
                else -> "$requested ($resolved)"
            }
        }

    /**
     * WSL-aware install path. When [wslDistributionMsId] is set, converts the Unix install path
     * to a Windows UNC path (e.g. `\\wsl.localhost\Ubuntu\home\user\.mise\...`).
     * Otherwise returns [installPath] unchanged.
     */
    val resolvedInstallPath: String
        get() = WslPathUtils.maybeConvertPathToWindowsUncPath(installPath, wslDistributionMsId)

    fun shimsVersion(): String = requestedVersion ?: version

    fun shimsInstallPath(): String =
        if (requestedVersion == null) {
            installPath
        } else {
            // replace the version part of the install path with the requested version
            val sanitizedPath = installPath.removeSuffix("/")
            if (sanitizedPath.endsWith(version)) {
                ShimUtils.resolveShortcutPath(sanitizedPath.dropLast(version.length) + requestedVersion)
            } else {
                // Silently returning the original path is a bug.
                // Throw an exception if the path format is unexpected to avoid silent misconfiguration.
                throw IllegalStateException("Could not determine version from install path: $installPath")
            }
        }
}
