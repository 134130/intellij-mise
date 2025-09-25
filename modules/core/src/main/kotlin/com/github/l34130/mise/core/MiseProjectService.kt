package com.github.l34130.mise.core

import com.github.l34130.mise.core.model.MiseTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.application
import com.jetbrains.rd.util.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class MiseProjectService(
    val project: Project,
    private val cs: CoroutineScope,
) : Disposable {
    var isInitialized: AtomicBoolean = AtomicBoolean(false)
        private set

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

    fun getTasks(): Set<MiseTask> = tasks

    suspend fun refresh() {
        tasks.clear()

        loadTasks()

        isInitialized.set(true)
    }

    override fun dispose() {
    }

    private suspend fun loadTasks() {
        val baseDir: VirtualFile =
            readAction {
                if (application.isUnitTestMode) {
                    VirtualFileManager.getInstance().findFileByUrl("temp:///src")
                } else {
                    LocalFileSystem.getInstance().findFileByPath(project.basePath ?: ProjectUtil.getBaseDir())
                }
            } ?: return

        val miseTasks = project.service<MiseTaskResolver>().getMiseTasks(baseDir, true)
        tasks.addAll(miseTasks)
    }
}
