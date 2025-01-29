package com.github.l34130.mise.core

import ai.grazie.utils.dropPrefix
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

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
    return collapsePath(virtualFile.path, project)
}
