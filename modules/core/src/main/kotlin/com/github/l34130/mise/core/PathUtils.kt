package com.github.l34130.mise.core

import ai.grazie.utils.dropPrefix
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

fun Project.baseDirectory(): String = this.presentableUrl ?: this.service<PathMacroManager>().collapsePath("\$PROJECT_DIR$")

fun collapsePath(
    path: String,
    project: Project,
): String {
    val pathMacroManager = project.service<PathMacroManager>()
    return pathMacroManager.collapsePath(path).dropPrefix("\$PROJECT_DIR$/")
}

fun collapsePath(
    psiFile: PsiFile,
    project: Project,
): String {
    val virtualFile = psiFile.virtualFile ?: psiFile.viewProvider.virtualFile
    return collapsePath(virtualFile, project)
}

fun collapsePath(
    virtualFile: VirtualFile,
    project: Project,
): String {
    val virtualFile = virtualFile
    return collapsePath(virtualFile.path, project)
}
