package com.github.l34130.mise.core.command

data class MiseDevTool(
    val version: String,
    val requestedVersion: String?,
    val installPath: String,
    val installed: Boolean,
    val active: Boolean,
    val source: MiseSource?,
) {
    fun shimsVersion(): String = requestedVersion ?: version

    fun shimsInstallPath(): String =
        if (requestedVersion == null) {
            installPath
        } else {
            // replace the version part of the install path with the requested version
            if (installPath.endsWith(version)) {
                installPath.dropLast(version.length) + requestedVersion
            } else {
                installPath
            }
        }
}
