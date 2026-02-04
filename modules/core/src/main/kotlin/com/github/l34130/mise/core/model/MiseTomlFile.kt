package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.util.guessMiseProjectDir
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.childrenOfType
import com.intellij.testFramework.LightVirtualFile
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlFileType
import org.toml.lang.psi.TomlTable

class MiseTomlFile {
    class TaskConfig(
        origin: TomlTable,
    ) : TomlTable by origin {
        val includes: List<String>? = (getValueWithKey("includes") as? TomlArray)?.elements?.mapNotNull { it.stringValue }

        companion object {
            fun resolveOrNull(file: TomlFile): TaskConfig? {
                val table = file.childrenOfType<TomlTable>().firstOrNull { it.header.key?.textMatches("task_config") == true }
                return table?.let { TaskConfig(it) }
            }
        }
    }

    companion object {
        fun isMiseTomlFile(
            project: Project,
            file: VirtualFile,
        ): Boolean {
            if (file.fileType != TomlFileType) return false

            val originalFile = if (file is LightVirtualFile) file.originalFile else file
            if (originalFile == null) return false

            return isMiseTomlFileName(project, originalFile, originalFile.name)
        }

        fun isMiseTomlFile(
            project: Project,
            file: VirtualFile,
            fileNameOverride: String,
            parentOverride: VirtualFile? = null,
        ): Boolean {
            // Used for rename/move events where name or parent changes but file type may not.
            val originalFile = if (file is LightVirtualFile) file.originalFile else file
            if (originalFile == null) return false

            return isMiseTomlFileName(project, originalFile, fileNameOverride, parentOverride)
        }

        private fun isMiseTomlFileName(
            project: Project,
            file: VirtualFile,
            fileName: String,
            parentOverride: VirtualFile? = null,
        ): Boolean {
            if (!fileName.endsWith(".toml")) return false
            val parent = parentOverride ?: file.parent

            if (fileName in listOf("mise.local.toml", ".mise.local.toml", "mise.toml", ".mise.toml") ||
                fileName.matches("^mise\\.(\\w+\\.)?toml$".toRegex())
            ) {
                if (parent.isProjectBaseDir(project)) return true
            }

            if (fileName == "config.toml") {
                if (parentNameMatches(parent, "mise") || parentNameMatches(parent, ".mise")) {
                    if (parentOf(parent, 2).isProjectBaseDir(project)) return true
                }
            }

            if (fileName == "mise.toml" && parentNameMatches(parent, ".config")) {
                if (parentOf(parent, 2).isProjectBaseDir(project)) return true
            }
            if (fileName == "config.toml" && parentNameMatches(parent, "mise", ".config")) {
                if (parentOf(parent, 3).isProjectBaseDir(project)) return true
            }
            if (parentNameMatches(parent, "conf.d", "mise", ".config")) {
                if (parent?.parent.isProjectBaseDir(project)) return true
            }

            return false
        }

        private fun parentNameMatches(parent: VirtualFile?, vararg names: String): Boolean {
            var current = parent
            for (i in names.indices.reversed()) {
                current ?: return false
                if (current.name != names[i]) return false
                current = current.parent
            }
            return true
        }

        private fun parentOf(
            parent: VirtualFile?,
            depthFromFile: Int,
        ): VirtualFile? {
            var current = parent
            repeat(depthFromFile - 1) {
                current = current?.parent ?: return null
            }
            return current
        }

        private fun VirtualFile?.isProjectBaseDir(project: Project): Boolean =
            if (this == null) {
                false
            } else {
                this == project.guessMiseProjectDir()
            }
    }
}
