package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.guessMiseProjectDir
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
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

            // Authoritative check: only treat files that `mise config --tracked-configs` says are active.
            return isMiseTomlFileName(project, originalFile, originalFile.name)
        }

        fun looksLikeMiseTomlFile(
            project: Project,
            file: VirtualFile,
        ): Boolean {
            if (file.fileType != TomlFileType) return false

            val originalFile = if (file is LightVirtualFile) file.originalFile else file
            if (originalFile == null) return false

            // Heuristic for file listeners: broad match to know when to refresh `mise config` cache.
            return looksLikeMiseTomlFileName(project, originalFile, originalFile.name)
        }

        fun looksLikeMiseTomlFile(
            project: Project,
            file: VirtualFile,
            fileNameOverride: String,
            parentOverride: VirtualFile? = null,
        ): Boolean {
            // Used for rename/move events where name or parent changes but file type may not.
            val originalFile = if (file is LightVirtualFile) file.originalFile else file
            if (originalFile == null) return false

            // Heuristic for file listeners: broad match to know when to refresh `mise config` cache.
            return looksLikeMiseTomlFileName(project, originalFile, fileNameOverride, parentOverride)
        }

        private fun isMiseTomlFileName(
            project: Project,
            file: VirtualFile,
            fileName: String,
            parentOverride: VirtualFile? = null,
        ): Boolean {
            // Keep UI aligned with the tracked configs list, not just file names.
            if (!fileName.endsWith(".toml")) return false
            val parent = parentOverride ?: file.parent ?: return false
            val baseDir = project.guessMiseProjectDir()
            val configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment
            val configPaths = MiseCommandLineHelper.getTrackedConfigsIfCached(project, configEnvironment) ?: return false

            val candidatePath = FileUtil.toSystemIndependentName("${parent.path}/$fileName")
            val basePath = FileUtil.toSystemIndependentName(baseDir.path).trimEnd('/')
            val basePrefix = if (basePath.endsWith("/")) basePath else "$basePath/"

            return configPaths.any { configPath ->
                val normalized = FileUtil.toSystemIndependentName(configPath.trim())
                val absolutePath =
                    if (FileUtil.isAbsolute(normalized)) {
                        normalized
                    } else {
                        "$basePrefix$normalized"
                    }
                absolutePath == candidatePath
            }
        }

        private fun looksLikeMiseTomlFileName(
            project: Project,
            file: VirtualFile,
            fileName: String,
            parentOverride: VirtualFile? = null,
        ): Boolean {
            // Intentionally wider than `isMiseTomlFileName`; only used to trigger cache refresh.
            if (!fileName.endsWith(".toml")) return false
            val parent = parentOverride ?: file.parent ?: return false
            val baseDir = project.guessMiseProjectDir()

            val relativePath = relativePathFromProject(baseDir, parent, fileName) ?: return false
            return looksLikeConfigPath(relativePath)
        }

        private fun relativePathFromProject(
            baseDir: VirtualFile,
            parent: VirtualFile,
            fileName: String,
        ): String? {
            val basePath = FileUtil.toSystemIndependentName(baseDir.path).trimEnd('/')
            val basePrefix = if (basePath.endsWith("/")) basePath else "$basePath/"

            val parentPath = FileUtil.toSystemIndependentName(parent.path).trimEnd('/')
            val filePath = FileUtil.toSystemIndependentName("$parentPath/$fileName")
            if (!filePath.startsWith(basePrefix)) return null

            return filePath.removePrefix(basePrefix).takeIf { it.isNotBlank() }
        }

        private fun looksLikeConfigPath(relativePath: String): Boolean {
            if (relativePath == "mise.toml" || relativePath == ".mise.toml") return true
            if (relativePath == "mise.local.toml" || relativePath == ".mise.local.toml") return true
            if (relativePath.matches(Regex("mise\\..+\\.toml"))) return true
            if (relativePath.matches(Regex("\\.mise\\..+\\.toml"))) return true
            if (relativePath == "mise/config.toml" || relativePath == ".mise/config.toml") return true
            if (relativePath == ".config/mise.toml") return true
            if (relativePath.matches(Regex("\\.config/mise\\..+\\.toml"))) return true
            if (relativePath == ".config/mise/config.toml") return true
            if (relativePath.startsWith(".config/mise/conf.d/") && relativePath.endsWith(".toml")) return true
            return false
        }
    }
}
