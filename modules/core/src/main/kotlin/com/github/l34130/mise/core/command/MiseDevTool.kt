package com.github.l34130.mise.core.command

data class MiseDevTool(
    val version: String,
    val requestedVersion: String? = null,
    val installPath: String,
    val installed: Boolean,
    val active: Boolean,
    val source: MiseSource? = null,
) {
    fun shimsVersion(): String = requestedVersion ?: version

    fun shimsInstallPath(): String =
        if (requestedVersion == null) {
            installPath
        } else {
            // replace the version part of the install path with the requested version
            val sanitizedPath = installPath.removeSuffix("/")
            if (sanitizedPath.endsWith(version)) {
                sanitizedPath.dropLast(version.length) + requestedVersion
            } else {
                // Silently returning the original path is a bug.
                // Throw an exception if the path format is unexpected to avoid silent misconfiguration.
                throw IllegalStateException("Could not determine version from install path: $installPath")
            }
        }
}
