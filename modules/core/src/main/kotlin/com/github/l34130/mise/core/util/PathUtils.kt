package com.github.l34130.mise.core.util

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.intellij.util.application
import java.io.File

@Deprecated("Use [com.intellij.ide.impl.ProjectUtil.getBaseDir] instead", ReplaceWith("com.intellij.ide.impl.ProjectUtil.getBaseDir()"))
fun Project.baseDirectory(): String {
    if (application.isUnitTestMode) {
        return getBaseDirectories().first().path
    }
    return this.presentableUrl ?: this.service<PathMacroManager>().collapsePath("\$PROJECT_DIR$")
}

fun presentablePath(
    project: Project?,
    path: String,
): String {
    val projectHomeUrl: String = PathUtil.toSystemDependentName(project?.basePath ?: ProjectUtil.getBaseDir())
    if (path.startsWith(projectHomeUrl)) {
        return StringUtil.ELLIPSIS + path.substring(projectHomeUrl.length)
    }

    return getPresentablePath(path)
}

fun getRelativePath(
    base: VirtualFile,
    file: VirtualFile,
): String? = getRelativePath(base.path, file.path)

fun getRelativePath(
    basePath: String,
    filePath: String,
): String? = FileUtil.getRelativePath(basePath, filePath, File.separatorChar)
