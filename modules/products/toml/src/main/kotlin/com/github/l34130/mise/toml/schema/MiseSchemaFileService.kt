package com.github.l34130.mise.toml.schema

import com.intellij.json.JsonLanguage
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.util.io.HttpRequests

@Service(Service.Level.PROJECT)
class MiseSchemaFileService(
    private val project: Project,
) {
    private val schemaFile: PsiFile by lazy {
        val chars =
            HttpRequests
                .request("https://mise.jdx.dev/schema/mise.json")
                .readChars()

        PsiFileFactory.getInstance(project).createFileFromText(JsonLanguage.INSTANCE, chars)
    }

    fun getSchemaFile(): VirtualFile = schemaFile.virtualFile

    companion object {
        fun getInstance(project: Project): MiseSchemaFileService = project.service<MiseSchemaFileService>()
    }
}
