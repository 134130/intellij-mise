package com.github.l34130.mise.core.command

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
}
