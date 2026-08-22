package com.github.l34130.mise.core

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

internal fun Project.isExcludedFromMiseResolution(file: VirtualFile): Boolean {
    if (isDisposed || !file.isValid) return false
    return runCatching {
        runReadActionBlocking {
            if (isDisposed || !file.isValid) return@runReadActionBlocking false
            if (ProjectFileIndex.getInstance(this).isExcluded(file)) return@runReadActionBlocking true

            val fileUrl = file.url.trimEnd('/')
            ModuleManager.getInstance(this).modules.any { module ->
                ModuleRootManager.getInstance(module).excludeRootUrls.any { excludeRootUrl ->
                    val normalizedExcludeRootUrl = excludeRootUrl.trimEnd('/')
                    fileUrl == normalizedExcludeRootUrl || fileUrl.startsWith("$normalizedExcludeRootUrl/")
                }
            }
        }
    }.getOrDefault(false)
}
