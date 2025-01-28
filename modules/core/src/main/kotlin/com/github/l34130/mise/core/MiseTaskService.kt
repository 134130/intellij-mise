package com.github.l34130.mise.core

import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.util.concurrency.SequentialTaskExecutor
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.concurrency.CancellablePromise
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class MiseTaskService(
    val project: Project,
) : Disposable {
    private val taskConfigDirectories: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet<VirtualFile>()

    private val executor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("MiseTaskService")

    init {
        project.messageBus.connect(this).let {
            it.subscribe(
                MiseTomlFileVfsListener.MISE_TOML_CHANGED,
                Runnable {
                    taskConfigDirectories.clear()
                    loadFileTaskDirectories()
                },
            )
        }
    }

    fun getFileTaskDirectories(): Set<VirtualFile> = taskConfigDirectories

    fun refresh(): CancellablePromise<Unit> {
        taskConfigDirectories.clear()
        return loadFileTaskDirectories()
    }

    private fun loadFileTaskDirectories(): CancellablePromise<Unit> =
        ReadAction
            .nonBlocking<Unit> {
                val result = mutableListOf<VirtualFile>()
                for (baseDir in project.getBaseDirectories()) {
                    result.addIfNotNull(baseDir.resolveFromRootOrRelative("mise-tasks")?.takeIf { it.isDirectory })
                    result.addIfNotNull(baseDir.resolveFromRootOrRelative(".mise-tasks")?.takeIf { it.isDirectory })
                    result.addIfNotNull(baseDir.resolveFromRootOrRelative("mise/tasks")?.takeIf { it.isDirectory })
                    result.addIfNotNull(baseDir.resolveFromRootOrRelative(".mise/tasks")?.takeIf { it.isDirectory })
                    result.addIfNotNull(baseDir.resolveFromRootOrRelative(".config/mise/tasks")?.takeIf { it.isDirectory })

                    val miseTomlFiles = project.service<MiseTomlService>().getMiseTomlFiles()
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
            }.expireWith(this)
            .submit(executor)

    override fun dispose() {
    }
}
