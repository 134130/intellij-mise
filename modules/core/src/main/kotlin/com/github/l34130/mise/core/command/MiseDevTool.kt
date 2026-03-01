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
                val shimsPath = sanitizedPath.dropLast(version.length) + requestedVersion
                resolveShortcutFile(shimsPath)
            } else {
                // Silently returning the original path is a bug.
                // Throw an exception if the path format is unexpected to avoid silent misconfiguration.
                throw IllegalStateException("Could not determine version from install path: $installPath")
            }
        }

    companion object {
        /**
         * On Windows, mise creates shortcut files instead of symlinks for version aliases.
         * These files contain relative paths to the actual installation directories.
         * For example, a file named "22" might contain ".\22.22.0", pointing to the real directory.
         *
         * This method resolves such shortcut files to the actual directory path.
         * On Unix systems where symlinks are used, the path will already be a directory
         * and is returned unchanged.
         */
        internal fun resolveShortcutFile(path: String): String {
            try {
                val file = File(path)
                if (file.isFile) {
                    val content = file.readText().trim()
                    if (content.isNotEmpty()) {
                        val resolved = file.parentFile.resolve(content)
                        if (resolved.isDirectory) {
                            return resolved.canonicalPath
                        }
                    }
                }
            } catch (_: Exception) {
                // If resolution fails (e.g., file not accessible), fall back to the original path
            }
            return path
        }
    }
}
