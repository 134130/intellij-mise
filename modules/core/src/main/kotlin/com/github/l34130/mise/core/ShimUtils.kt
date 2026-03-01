package com.github.l34130.mise.core

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import java.io.File
import java.io.IOException

object ShimUtils {
    /**
     * On Windows, mise creates plain text files instead of symlinks for version aliases
     * (e.g. `installs/node/22` containing `.\22.22.0`).
     * This function resolves such a shortcut file to the actual installation directory path.
     * On Unix where symlinks are used, the path is already a directory and returned as-is.
     */
    fun resolveShortcutPath(path: String): String {
        val file = File(path)
        if (!file.isFile) return path
        return try {
            val content = file.readText().trim()
            if (content.isEmpty()) return path
            val resolved = file.parentFile.resolve(content)
            if (resolved.isDirectory) resolved.canonicalPath else path
        } catch (_: IOException) {
            path
        } catch (_: SecurityException) {
            path
        }
    }

    fun findExecutable(
        basePath: String,
        executableName: String,
    ): VirtualFile {
        val resolvedPath = resolveShortcutPath(basePath)
        val fs = LocalFileSystem.getInstance()

        // Refresh file system for UNC paths (WSL)
        if (resolvedPath.startsWith("\\\\")) {
            fs.refreshAndFindFileByPath(resolvedPath)
        }

        val baseFile =
            fs.findFileByPath(resolvedPath)
                ?: throw IllegalStateException("Base path not found: $resolvedPath")

        val searchRoot: VirtualFile = if (baseFile.isDirectory) baseFile else baseFile.parent

        val potentialRelativePaths =
            sequence {
                // For UNC/WSL paths, use Unix structure (no .exe extensions)
                if (basePath.startsWith("\\\\")) {
                    yield("bin/$executableName")
                    yield(executableName)
                } else if (SystemInfo.isWindows) {
                    yield("bin/$executableName.exe")
                    yield("bin/$executableName.cmd")
                    yield("bin/$executableName.bat")
                    yield("bin/$executableName")
                    yield("$executableName.exe")
                    yield("$executableName.cmd")
                    yield("$executableName.bat")
                    yield(executableName)
                } else {
                    yield("bin/$executableName")
                    yield(executableName)
                }
            }

        return potentialRelativePaths
            .mapNotNull { searchRoot.findFileByRelativePath(it) }
            .firstOrNull { it.isFile }
            ?: throw IllegalStateException("Cannot find executable '$executableName' in path: $resolvedPath")
    }
}
