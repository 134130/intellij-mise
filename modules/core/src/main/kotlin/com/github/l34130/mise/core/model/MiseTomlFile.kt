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

            // Check project-level mise config files
            if (originalFile.name in listOf("mise.local.toml", ".mise.local.toml", "mise.toml", ".mise.toml") ||
                originalFile.name.matches("^mise\\.(\\w+\\.)?toml$".toRegex())
            ) {
                if (originalFile.parent.isProjectBaseDir(project)) return true
            }

            if (originalFile.name == "config.toml") {
                if (originalFile.isParentName("mise") || originalFile.isParentName(".mise")) {
                    if (originalFile.parentOf(2).isProjectBaseDir(project)) return true
                }
            }

            if (originalFile.name == "mise.toml" && originalFile.isParentName(".config")) {
                if (originalFile.parentOf(2).isProjectBaseDir(project)) return true
            }
            if (originalFile.name == "config.toml" && originalFile.isParentName("mise", ".config")) {
                if (originalFile.parentOf(3).isProjectBaseDir(project)) return true
            }
            if (originalFile.extension == "toml" && file.isParentName("conf.d", "mise", ".config")) {
                if (file.parent?.parent.isProjectBaseDir(project)) return true
            }

            return false
        }
        
        /**
         * Check if a file is a global mise configuration file.
         * Global configs are in the user's home directory (~/.config/mise/, etc.)
         */
        fun isGlobalMiseTomlFile(file: VirtualFile): Boolean {
            if (file.fileType != TomlFileType) return false
            
            val originalFile = if (file is LightVirtualFile) file.originalFile else file
            if (originalFile == null) return false
            
            val path = originalFile.path
            
            // Check for global mise config patterns
            // ~/.config/mise/config.toml
            // ~/.config/mise.toml
            // ~/.mise/config.toml
            // ~/.mise.toml
            // ~/.config/mise/conf.d/*.toml
            
            return when {
                // ~/.config/mise/config.toml - check that both .config and mise are consecutive parents
                originalFile.name == "config.toml" && 
                    originalFile.parent?.name == "mise" && 
                    originalFile.parent?.parent?.name == ".config" -> true
                // ~/.config/mise.toml
                originalFile.name == "mise.toml" && originalFile.parent?.name == ".config" -> true
                // ~/.mise/config.toml
                originalFile.name == "config.toml" && originalFile.parent?.name == ".mise" -> true
                // ~/.mise.toml - must be in root of some directory (likely home)
                originalFile.name == ".mise.toml" -> true
                // ~/.config/mise/conf.d/*.toml
                originalFile.extension == "toml" && 
                    originalFile.parent?.name == "conf.d" &&
                    originalFile.parent?.parent?.name == "mise" &&
                    originalFile.parent?.parent?.parent?.name == ".config" -> true
                else -> false
            }
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

        private fun VirtualFile?.isProjectBaseDir(project: Project): Boolean =
            if (this == null) {
                false
            } else {
                this == project.guessMiseProjectDir()
            }
    }
}
