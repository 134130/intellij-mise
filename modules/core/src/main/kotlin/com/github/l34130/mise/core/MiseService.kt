package com.github.l34130.mise.core

import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.rd.util.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class MiseService(
    val project: Project,
    private val cs: CoroutineScope,
) : Disposable {
    private val miseTomlFiles: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet<VirtualFile>()
    private val taskConfigDirectories: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet<VirtualFile>()

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

    suspend fun refresh() {
        miseTomlFiles.clear()

        loadMiseTomlFiles()
        loadFileTaskDirectories()
    }

    override fun dispose() {
    }

    private suspend fun loadMiseTomlFiles() {
        readAction {
            val result = mutableListOf<VirtualFile>()

            for (baseDir in project.getBaseDirectories()) {
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
            }

            miseTomlFiles.addAll(result)
        }
    }

    private suspend fun loadFileTaskDirectories() {
        readAction {
            val result = mutableListOf<VirtualFile>()
            for (baseDir in project.getBaseDirectories()) {
                result.addIfNotNull(baseDir.resolveFromRootOrRelative("mise-tasks")?.takeIf { it.isDirectory })
                result.addIfNotNull(baseDir.resolveFromRootOrRelative(".mise-tasks")?.takeIf { it.isDirectory })
                result.addIfNotNull(baseDir.resolveFromRootOrRelative("mise/tasks")?.takeIf { it.isDirectory })
                result.addIfNotNull(baseDir.resolveFromRootOrRelative(".mise/tasks")?.takeIf { it.isDirectory })
                result.addIfNotNull(baseDir.resolveFromRootOrRelative(".config/mise/tasks")?.takeIf { it.isDirectory })

                val miseTomlFiles = project.service<MiseService>().getMiseTomlFiles()
                for (miseToml in miseTomlFiles) {
                    val psiFile = miseToml.findPsiFile(project) as? MiseTomlFile ?: continue

                    val taskConfig = MiseTomlFile.TaskConfig.resolveOrNull(psiFile) ?: continue
                    val taskTomlOrDirs = taskConfig.includes ?: continue

                    for (taskTomlOrDir in taskTomlOrDirs) {
                        val file = baseDir.resolveFromRootOrRelative(taskTomlOrDir) ?: continue
                        if (file.isDirectory) {
                            result.addIfNotNull(file)
                        }
                    }
                }
            }

            taskConfigDirectories.addAll(result)
        }
    }

    companion object {
        private val MISE_TOML_NAME_REGEX = "^\\.?mise\\.(\\w+\\.)?toml$".toRegex()

        fun getInstance(project: Project): MiseService = project.getService(MiseService::class.java)
    }
}
