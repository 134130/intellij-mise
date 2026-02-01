package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.model.MiseShellScriptTask
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.model.MiseTomlFile
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.baseDirectory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.util.childrenOfType
import com.intellij.util.application
import fleet.multiplatform.shims.ConcurrentHashMap
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTable
import kotlin.io.path.isExecutable

@Service(Service.Level.PROJECT)
class MiseTaskResolver(
    val project: Project,
) : Disposable {
    private val cache = ConcurrentHashMap<String, List<MiseTask>>()

    init {
        // Ensure the VFS listener service is initialized
        project.service<MiseTomlFileListener>()

        // Subscribe to cache invalidation events
        MiseProjectEventListener.subscribe(project, this) { event ->
            if (event.kind == MiseProjectEvent.Kind.TOML_CHANGED ||
                event.kind == MiseProjectEvent.Kind.TOML_PSI_CHANGED) {
                invalidateCache()
            }
        }
    }

    /**
     * Invalidate the internal task cache.
     * Should be called when mise configuration files change.
     */
    fun invalidateCache() {
        cache.clear()
    }


    suspend fun getMiseTasks(
        refresh: Boolean = false,
        configEnvironment: String? = null,
    ): List<MiseTask> {
        val baseDirVf: VirtualFile =
            readAction {
                if (application.isUnitTestMode) {
                    VirtualFileManager.getInstance().findFileByUrl("temp:///src")
                        ?: VirtualFileManager.getInstance().findFileByUrl("file://${project.baseDirectory()}")
                } else {
                    VirtualFileManager.getInstance().findFileByUrl("file://${project.baseDirectory()}")
                }
            } ?: return emptyList()

        val configEnvironment = configEnvironment ?: project.service<MiseProjectSettings>().state.miseConfigEnvironment

        val cacheKey = configEnvironment
        if (!refresh && !application.isUnitTestMode) {
            cache[cacheKey]?.let { return it }
        }

        val result = mutableListOf<MiseTask>()
        val configVfs = project.service<MiseConfigFileResolver>().resolveConfigFiles(baseDirVf, refresh, configEnvironment)

        // Resolve tasks from the config file
        smartReadAction(project) {
            for (configVf in configVfs) {
                val psiFile = configVf.findPsiFile(project) as? TomlFile ?: continue
                val tomlTableTasks: List<MiseTomlTableTask> = MiseTomlTableTask.resolveAllFromTomlFile(psiFile)
                result.addAll(tomlTableTasks)
            }
        }

        // Resolve tasks from the task config files
        smartReadAction(project) {
            for (configVf in configVfs) {
                val configPsiFile = configVf.findPsiFile(project) as? TomlFile ?: continue
                val taskIncludes = MiseTomlFile.TaskConfig.resolveOrNull(configPsiFile)?.includes ?: continue
                val taskTomlPsiFiles =
                    taskIncludes
                        .mapNotNull { baseDirVf.findFileOrDirectory(it) }
                        .filter { it.isFile }
                        .mapNotNull { it.findPsiFile(project) as? TomlFile }
                for (taskTomlPsiFile in taskTomlPsiFiles) {
                    val tomlTables = taskTomlPsiFile.childrenOfType<TomlTable>()
                    for (tomlTable in tomlTables) {
                        val keySegments = tomlTable.header.key?.segments ?: continue
                        val keySegment = keySegments.singleOrNull() ?: continue
                        val task = MiseTomlTableTask.resolveOrNull(keySegment) ?: continue
                        result.add(task)
                    }
                }
            }
        }

        // Resolve tasks from the task directories (Shell Script)
        val fileTaskDirectories = resolveFileTaskDirectories(project, baseDirVf, configEnvironment)
        for (fileTaskDir in fileTaskDirectories) {
            for (file in fileTaskDir.leafChildren()) {
                val isExecutable =
                    file.toNioPathOrNull()?.isExecutable()
                        ?: true // assume file is executable if we can't get its path
                if (!isExecutable) continue

                val task: MiseShellScriptTask = MiseShellScriptTask.resolveOrNull(fileTaskDir, file) ?: continue
                result.add(task)
            }
        }

        if (!application.isUnitTestMode) {
            cache[cacheKey] = result
        }
        return result
    }

    override fun dispose() {}

    companion object {
        private val DEFAULT_TASK_DIRECTORIES =
            listOf(
                "mise-tasks",
                ".mise-tasks",
                "mise/tasks",
                ".mise/tasks",
                ".config/mise/tasks",
            )

        private suspend fun resolveFileTaskDirectories(
            project: Project,
            baseDirVf: VirtualFile,
            configEnvironment: String? = null,
        ): List<VirtualFile> {
            val result = mutableListOf<VirtualFile>()

            // Resolve default task directories
            smartReadAction(project) {
                for (dir in DEFAULT_TASK_DIRECTORIES) {
                    val vf = baseDirVf.resolveFromRootOrRelative(dir) ?: continue
                    if (vf.isDirectory) {
                        result.add(vf)
                    }
                }
            }

            // Resolve task directories defined in the config file
            val configVfs = project.service<MiseConfigFileResolver>().resolveConfigFiles(baseDirVf, false, configEnvironment)
            smartReadAction(project) {
                for (configVf in configVfs) {
                    val psiFile = configVf.findPsiFile(project) as? TomlFile ?: continue
                    val taskTomlOrDirs = MiseTomlFile.TaskConfig.resolveOrNull(psiFile)?.includes ?: emptyList()

                    for (taskTomlOrDir in taskTomlOrDirs) {
                        val directory = baseDirVf.findFileOrDirectory(taskTomlOrDir)?.takeIf { it.isDirectory } ?: continue
                        result.add(directory)
                    }
                }
            }

            return result
        }

        private fun VirtualFile.leafChildren(): Sequence<VirtualFile> =
            children.asSequence().flatMap {
                if (it.isDirectory) {
                    it.leafChildren()
                } else {
                    sequenceOf(it)
                }
            }
    }
}
