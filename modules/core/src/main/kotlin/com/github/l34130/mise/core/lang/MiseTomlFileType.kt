package com.github.l34130.mise.core.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.toml.lang.TomlLanguage
import org.toml.lang.psi.TomlFileType
import javax.swing.Icon

object MiseTomlFileType : LanguageFileType(TomlLanguage) {
    override fun getName(): String = "mise"

    override fun getDescription(): String = "Mise Configuration file"

    override fun getDefaultExtension(): String = "toml"

    override fun getIcon(): Icon = TomlFileType.icon

    fun isMiseTomlFile(
        project: Project,
        file: VirtualFile,
    ): Boolean {
        if (file.fileType != this) return false

        if (file.name in listOf("mise.local.toml", ".mise.local.toml", "mise.toml", ".mise.toml") ||
            file.name.matches("^mise\\.(\\w+\\.)?toml$".toRegex())
        ) {
            return file.parent?.isProjectBaseDir(project) == true
        }

        if (file.name == "config.toml") {
            if (file.isParentName("mise") || file.isParentName(".mise")) {
                return file.parentOf(2)?.isProjectBaseDir(project) == true
            }
        }

        if (file.name == "mise.toml" && file.isParentName(".config")) {
            return file.parentOf(2)?.isProjectBaseDir(project) == true
        }
        if (file.name == "config.toml" && file.isParentName("mise", ".config")) {
            return file.parentOf(3)?.isProjectBaseDir(project) == true
        }
        if (file.extension == "toml" && file.isParentName("conf.d", "mise", ".config")) {
            if (project.getBaseDirectories().contains(file.parent?.parent)) return true
        }

        return false
    }

    private fun VirtualFile.isParentName(vararg names: String): Boolean {
        var parent = this
        for (i in names.indices.reversed()) {
            parent = parent.parent ?: return false
            if (parent.name != names[i]) return false
        }
        return true
    }

    private fun VirtualFile.parentOf(depth: Int = 1): VirtualFile? {
        var parent = this
        repeat(depth) {
            parent = parent.parent ?: return null
        }
        return parent
    }

    private fun VirtualFile.isProjectBaseDir(project: Project): Boolean = project.getBaseDirectories().contains(this)
}
