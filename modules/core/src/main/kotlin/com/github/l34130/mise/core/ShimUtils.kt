package com.github.l34130.mise.core

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile

object ShimUtils {
    fun findExecutable(
        basePath: String,
        executableName: String,
    ): VirtualFile {
        val fs = LocalFileSystem.getInstance()

        // Refresh file system for UNC paths (WSL)
        if (basePath.startsWith("\\\\")) {
            fs.refreshAndFindFileByPath(basePath)
        }

        val baseFile =
            fs.findFileByPath(basePath)
                ?: throw IllegalStateException("Base path not found: $basePath")

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
            ?: throw IllegalStateException("Cannot find executable '$executableName' in path: $basePath")
    }
}
