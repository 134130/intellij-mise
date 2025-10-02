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
        val baseFile =
            fs.findFileByPath(basePath)
                ?: throw IllegalStateException("Base path not found: $basePath")

        val searchRoot: VirtualFile = if (baseFile.isDirectory) baseFile else baseFile.parent

        val potentialRelativePaths =
            sequence {
                if (SystemInfo.isWindows) {
                    yield("bin/$executableName.exe")
                    yield("bin/$executableName.cmd")
                    yield("bin/$executableName.bat")
                }
                yield("bin/$executableName")

                if (SystemInfo.isWindows) {
                    yield("$executableName.exe")
                    yield("$executableName.cmd")
                    yield("$executableName.bat")
                }
                yield(executableName)
            }

        return potentialRelativePaths
            .mapNotNull { searchRoot.findFileByRelativePath(it) }
            .firstOrNull { it.isFile }
            ?: throw IllegalStateException("Cannot find executable '$executableName' in path: $basePath")
    }
}
