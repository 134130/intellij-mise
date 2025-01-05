package com.github.l34130.mise.core.lang.json

import com.github.l34130.mise.core.lang.MiseTomlFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class MiseTomlJsonSchemaFileProviderFactory :
    JsonSchemaProviderFactory,
    DumbAware {
    override fun getProviders(project: Project): List<JsonSchemaFileProvider> = listOf(MiseTomlJsonSchemaFileProvider())

    class MiseTomlJsonSchemaFileProvider : JsonSchemaFileProvider {
        override fun isAvailable(file: VirtualFile): Boolean = file.fileType is MiseTomlFileType

        override fun getName(): String = "mise"

        override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

        override fun isUserVisible(): Boolean = false

        override fun getSchemaFile(): VirtualFile? = JsonSchemaProviderFactory.getResourceFile(javaClass, "/schemas/mise.json")
    }
}
