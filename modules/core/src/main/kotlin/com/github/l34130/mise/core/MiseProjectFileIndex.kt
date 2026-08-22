package com.github.l34130.mise.core

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

internal fun Project.isExcludedFromMiseResolution(file: VirtualFile): Boolean {
    if (isDisposed || !file.isValid) return false
    return runReadActionBlocking {
        if (isDisposed || !file.isValid) {
            false
        } else {
            ProjectFileIndex.getInstance(this).isExcluded(file)
        }
    }
}
