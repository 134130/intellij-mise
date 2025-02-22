package com.github.l34130.mise.core

import com.github.l34130.mise.core.model.MiseShellScriptTask
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.model.MiseTomlFile
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.github.l34130.mise.core.util.baseDirectory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.util.childrenOfType
import com.intellij.util.application
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.rd.util.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.isExecutable

@Service(Service.Level.PROJECT)
class MiseService(
    val project: Project,
    private val cs: CoroutineScope,
) : Disposable {
    var isInitialized: AtomicBoolean = AtomicBoolean(false)
        private set

    private val miseTomlFiles: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet<VirtualFile>()
    private val taskConfigDirectories: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet<VirtualFile>()
    private val tasks: MutableSet<MiseTask> = ConcurrentHashMap.newKeySet<MiseTask>()

    init {
        project.messageBus.connect(this).let {
            it.subscribe(
                MiseTomlFileVfsListener.MISE_TOML_CHANGED,
                Runnable {
                    cs.launch(Dispatchers.IO) {
                        refresh()
                    }
                },
            )
            MiseTomlFileVfsListener.startListening(project, this, it)
        }
    }

    fun getMiseTomlFiles(): Set<VirtualFile> = miseTomlFiles

    fun getFileTaskDirectories(): Set<VirtualFile> = taskConfigDirectories

    fun getTasks(): Set<MiseTask> = tasks

    suspend fun refresh() {
        miseTomlFiles.clear()
        taskConfigDirectories.clear()
        tasks.clear()

        loadMiseTomlFiles()
        loadFileTaskDirectories()
        loadTasks()

        isInitialized.set(true)
    }

    override fun dispose() {
    }

    private suspend fun loadMiseTomlFiles() {
        readAction {
            val result = mutableListOf<VirtualFile>()

            val baseDir =
                if (application.isUnitTestMode) {
                    VirtualFileManager.getInstance().findFileByUrl("temp:///src") ?: return@readAction
                } else {
                    LocalFileSystem.getInstance().findFileByPath(project.baseDirectory()) ?: return@readAction
                }

            for (child in baseDir.children) {
                if (child.isDirectory) {
                    // mise/config.toml or .mise/config.toml
                    if (child.name == "mise" || child.name == ".mise") {
                        result.addIfNotNull(child.resolveFromRootOrRelative("config.toml"))
                    }

                    if (child.name == ".config") {
                        // .config/mise.toml
                        result.addIfNotNull(child.resolveFromRootOrRelative("mise.toml"))
                        // .config/mise/config.toml
                        result.addIfNotNull(child.resolveFromRootOrRelative("config.toml"))
                        // .config/mise/conf.d/*.toml
                        result.addAll(child.resolveFromRootOrRelative("conf.d")?.children.orEmpty())
                    }
                } else {
                    // mise.local.toml, mise.toml, .mise.local.toml, .mise.toml
                    if (child.name.matches(MISE_TOML_NAME_REGEX)) {
                        result.add(child)
                    }
                }
            }

            miseTomlFiles.addAll(result)
        }
    }

    private suspend fun loadFileTaskDirectories() {
        readAction {
            val result = mutableListOf<VirtualFile>()

            val baseDir =
                if (application.isUnitTestMode) {
                    VirtualFileManager.getInstance().findFileByUrl("temp:///src") ?: return@readAction
                } else {
                    LocalFileSystem.getInstance().findFileByPath(project.baseDirectory()) ?: return@readAction
                }

            result.addIfNotNull(baseDir.resolveFromRootOrRelative("mise-tasks")?.takeIf { it.isDirectory })
            result.addIfNotNull(baseDir.resolveFromRootOrRelative(".mise-tasks")?.takeIf { it.isDirectory })
            result.addIfNotNull(baseDir.resolveFromRootOrRelative("mise/tasks")?.takeIf { it.isDirectory })
            result.addIfNotNull(baseDir.resolveFromRootOrRelative(".mise/tasks")?.takeIf { it.isDirectory })
            result.addIfNotNull(baseDir.resolveFromRootOrRelative(".config/mise/tasks")?.takeIf { it.isDirectory })

            for (miseToml in getMiseTomlFiles()) {
                val psiFile = miseToml.findPsiFile(project) as? TomlFile ?: continue

                val taskConfig = MiseTomlFile.TaskConfig.resolveOrNull(psiFile) ?: continue
                val taskTomlOrDirs = taskConfig.includes ?: continue

                for (taskTomlOrDir in taskTomlOrDirs) {
                    val file = baseDir.resolveFromRootOrRelative(taskTomlOrDir) ?: continue
                    if (file.isDirectory) {
                        result.addIfNotNull(file)
                    }
                }
            }

            taskConfigDirectories.addAll(result)
        }
    }

    private suspend fun loadTasks() {
        readAction {
            val result = mutableListOf<MiseTask>()

            val baseDir =
                if (application.isUnitTestMode) {
                    VirtualFileManager.getInstance().findFileByUrl("temp:///src") ?: return@readAction
                } else {
                    LocalFileSystem.getInstance().findFileByPath(project.baseDirectory()) ?: return@readAction
                }

            // get all tasks from all toml files in the project
            for (virtualFile in getMiseTomlFiles()) {
                val psiFile = virtualFile.findPsiFile(project) as? TomlFile ?: continue
                result.addAll(MiseTomlTableTask.resolveAllFromTomlFile(psiFile))
            }

            // get all tasks from the task directories
            for (directory in getFileTaskDirectories()) {
                directory
                    .leafChildren()
                    .filter {
                        // if it's not a real file, consider it as executable
                        val nioPath = it.toNioPathOrNull() ?: return@filter true
                        nioPath.isExecutable()
                    }.mapNotNullTo(result) {
                        MiseShellScriptTask.resolveOrNull(project, directory, it)
                    }
            }

            // get all tasks from the task config includes
            for (miseToml in getMiseTomlFiles()) {
                val psiFile = miseToml.findPsiFile(project) as? TomlFile ?: continue

                val taskConfig = MiseTomlFile.TaskConfig.resolveOrNull(psiFile) ?: continue
                val taskTomlOrDirs = taskConfig.includes ?: continue

                for (taskTomlOrDir in taskTomlOrDirs) {
                    val virtualFile = baseDir.resolveFromRootOrRelative(taskTomlOrDir) ?: continue
                    if (!virtualFile.isDirectory) {
                        val psiFile = virtualFile.findPsiFile(project) as? TomlFile ?: continue
                        val tables = psiFile.childrenOfType<TomlTable>()
                        for (table in tables) {
                            val keySegments = table.header.key?.segments ?: continue
                            val keySegment = keySegments.singleOrNull() ?: continue
                            result.addIfNotNull(MiseTomlTableTask.resolveOrNull(keySegment))
                        }
                    }
                }
            }

            tasks.addAll(result)
        }
    }

    private fun VirtualFile.leafChildren(): Sequence<VirtualFile> =
        children.asSequence().flatMap {
            if (it.isDirectory) {
                it.leafChildren()
            } else {
                sequenceOf(it)
            }
        }

    companion object {
        private val MISE_TOML_NAME_REGEX = "^\\.?mise\\.(\\w+\\.)?toml$".toRegex()

        fun getInstance(project: Project): MiseService = project.getService(MiseService::class.java)
    }
}
