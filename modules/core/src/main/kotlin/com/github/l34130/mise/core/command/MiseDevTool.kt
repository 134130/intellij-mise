package com.github.l34130.mise.core.command

import java.io.File

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

    /**
     * Returns the effective install path, resolving mise version alias files on Windows.
     *
     * On Windows, mise creates text files for short version aliases (e.g., "22") that
     * contain a relative path to the actual installation directory (e.g., ".\22.22.0").
     * This method resolves such alias files to return the actual installation directory.
     *
     * On other platforms, symlinks are resolved transparently by the OS, so this
     * simply returns [shimsInstallPath].
     */
    fun resolvedInstallPath(): String {
        val path = shimsInstallPath()
        val file = File(path)
        if (!file.isFile) return path
        // Read the alias file content (e.g., ".\22.22.0") and resolve against the parent directory
        val content = file.readText().trim()
        val resolved = File(content).takeIf { it.isAbsolute } ?: (file.parentFile?.resolve(content) ?: file)
        return resolved.canonicalPath
    }
}
