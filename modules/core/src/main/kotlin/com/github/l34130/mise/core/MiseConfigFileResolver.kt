package com.github.l34130.mise.core

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.isFile
import com.intellij.util.containers.addIfNotNull
import fleet.multiplatform.shims.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class MiseConfigFileResolver(
    val project: Project,
) {
    private val cache = ConcurrentHashMap<String, List<VirtualFile>>()

    suspend fun resolveConfigFiles(
        baseDirVf: VirtualFile,
        refresh: Boolean = false,
    ): List<VirtualFile> {
        if (!refresh) cache[baseDirVf.path]?.let { return it }

        val result =
            readAction {
                buildList {
                    addIfNotNull(baseDirVf.findFileOrDirectory("mise/config.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".mise/config.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".config/mise.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".config/mise/config.toml")?.takeIf { it.isFile })
                    // .config/mise/conf.d/*.toml
                    baseDirVf.findFileOrDirectory(".config/mise/conf.d")?.takeIf { it.isDirectory }?.let { dir ->
                        addAll(dir.children.filter { it.name.endsWith(".toml") && it.isFile })
                    }
                    addIfNotNull(baseDirVf.findFileOrDirectory("mise.local.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory("mise.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".mise.local.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".mise.toml")?.takeIf { it.isFile })
                }
            }

        cache[baseDirVf.path] = result
        return result
    }
}
