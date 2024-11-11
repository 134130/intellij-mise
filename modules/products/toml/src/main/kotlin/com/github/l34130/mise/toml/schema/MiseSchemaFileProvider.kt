package com.github.l34130.mise.toml.schema

import com.github.l34130.mise.toml.MiseFileType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType

@Service(Service.Level.PROJECT)
class MiseSchemaFileProvider(
    private val project: Project,
) : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean = file.fileType.name == MiseFileType.name

    override fun getName(): String = "mise"

    override fun getSchemaFile(): VirtualFile = MiseSchemaFileService.getInstance(project).getSchemaFile()

    override fun getSchemaType(): SchemaType = SchemaType.schema
}
