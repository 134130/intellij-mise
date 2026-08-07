package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.util.guessMiseProjectDir
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.testFramework.LightVirtualFile
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlFileType
import org.toml.lang.psi.TomlTable

class MiseTomlFile {
    class TaskConfig(
        origin: TomlTable,
    ) {
        val includes: List<String>? =
            (origin.getValueWithKey("includes") as? TomlArray)
                ?.elements
                ?.mapNotNull { it.stringValue }

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

        /**
         * Candidate locations for the main mise config file, checked without shelling out to `mise`.
         * Kept in sync with the well-known paths mise itself resolves by default.
         */
        private val DEFAULT_CONFIG_FILE_PATHS =
            listOf(
                "mise.toml", ".mise.toml", "mise.local.toml", ".mise.local.toml",
                "mise/config.toml", ".mise/config.toml",
                ".config/mise.toml", ".config/mise/config.toml",
            )

        /**
         * Whether [file] is a TOML task file explicitly listed in `[task_config].includes`.
         *
         * Mise documents `task_config.includes` as explicit paths (Tera-templated) for TOML task files
         * and file-task directories. We only treat direct TOML file includes as "task include files"
         * for the purpose of accepting bare task tables like:
         *
         * ```toml
         * [lint]
         * run = "echo hi"
         * ```
         *
         * We intentionally ignore:
         * - directory includes (file tasks live there, not TOML)
         * - `git::` includes
         * - any Tera template we can't trivially resolve
         */
        fun isTaskIncludeFile(
            project: Project,
            file: VirtualFile,
        ): Boolean {
            if (!file.isValid || file.fileType != TomlFileType) return false
            val baseDir = project.guessMiseProjectDir()
            val rel = VfsUtilCore.getRelativePath(file, baseDir, '/')?.replace('\\', '/') ?: return false
            return getIncludedTomlRelativePaths(project).contains(rel)
        }

        private fun getIncludedTomlRelativePaths(project: Project): Set<String> {
            val manager = CachedValuesManager.getManager(project)
            return manager.getCachedValue(project) {
                CachedValueProvider.Result.create(
                    computeIncludedTomlRelativePaths(project),
                    // Keep this cheap and safe: invalidate on any VFS structure/content change.
                    VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
                )
            }
        }

        private fun computeIncludedTomlRelativePaths(project: Project): Set<String> {
            val baseDir = project.guessMiseProjectDir()

            val configFiles =
                DEFAULT_CONFIG_FILE_PATHS
                    .mapNotNull { path -> baseDir.findFileOrDirectory(path)?.takeIf { it.isFile } }

            val included = LinkedHashSet<String>()
            for (configVf in configFiles) {
                val psiFile = configVf.findPsiFile(project) as? TomlFile ?: continue
                val includes = TaskConfig.resolveOrNull(psiFile)?.includes ?: continue
                for (rawInclude in includes) {
                    val include = normalizeIncludePath(rawInclude) ?: continue
                    if (!include.endsWith(".toml")) continue

                    val target = baseDir.findFileOrDirectory(include)
                    if (target?.isFile == true) {
                        VfsUtilCore.getRelativePath(target, baseDir, '/')
                            ?.replace('\\', '/')
                            ?.let { included.add(it) }
                    }
                }
            }

            return included
        }

        private fun normalizeIncludePath(rawInclude: String): String? {
            var s = rawInclude.trim().replace('\\', '/')
            if (s.startsWith("git::")) return null

            // Includes are Tera templates. We only support the common config_root variable.
            s = s.replace("\\{\\{\\s*config_root\\s*}}".toRegex(), "")
            if (s.contains("{{")) return null

            while (s.startsWith("./")) s = s.removePrefix("./")
            // mise includes are typically relative; treat absolute-leading slashes as relative to base.
            while (s.startsWith('/')) s = s.removePrefix("/")
            // A directory include may be written as `tasks/`; VirtualFile resolution expects no trailing slash.
            while (s.endsWith('/')) s = s.removeSuffix("/")
            return s.ifBlank { null }
        }
    }
}
