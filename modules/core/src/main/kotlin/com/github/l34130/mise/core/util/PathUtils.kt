package com.github.l34130.mise.core.util

import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.application
import java.io.File

fun Project.baseDirectory(): String {
    if (application.isUnitTestMode) {
        return getBaseDirectories().first().path
    }
    return this.presentableUrl ?: this.service<PathMacroManager>().collapsePath("\$PROJECT_DIR$")
}

fun collapsePath(
    psiFile: PsiFile,
    project: Project,
): String {
    val virtualFile = psiFile.viewProvider.virtualFile
    return collapsePath(virtualFile, project)
}

fun collapsePath(
    virtualFile: VirtualFile,
    project: Project,
): String {
    val virtualFile = virtualFile
    return collapsePath(virtualFile.path, project)
}

fun collapsePath(
    path: String,
    project: Project,
): String {
    val result = path.removePrefix(project.baseDirectory())
    return result.removePrefix(File.separator)
}

fun getRelativePath(
    base: VirtualFile,
    file: VirtualFile,
): String? = getRelativePath(base.path, file.path)

fun getRelativePath(
    basePath: String,
    filePath: String,
): String? = FileUtil.getRelativePath(basePath, filePath, File.separatorChar)
