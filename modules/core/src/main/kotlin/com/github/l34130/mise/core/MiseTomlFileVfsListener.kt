package com.github.l34130.mise.core

import com.github.l34130.mise.core.model.MiseTomlFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ZipperUpdater
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileContentsChangedAdapter
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.util.Alarm
import com.intellij.util.messages.Topic
import java.util.concurrent.ConcurrentHashMap

class MiseTomlFileVfsListener(
    updater: MiseLocalIndexUpdater,
) : BulkVirtualFileListenerAdapter(
    object : VirtualFileContentsChangedAdapter() {
        override fun onFileChange(fileOrDirectory: VirtualFile) {
            updater.onFileChange(fileOrDirectory)
        }

        override fun onBeforeFileChange(fileOrDirectory: VirtualFile) {
            updater.onFileChange(fileOrDirectory)
        }
    }
) {
    companion object {
        val MISE_TOML_CHANGED = Topic.create("MISE_TOML_CHANGED", Function0::class.java)

        fun startListening(project: Project, disposable: Disposable) {
            MiseLocalIndexUpdater(project, disposable)
        }
    }

    class MiseLocalIndexUpdater(
        val project: Project,
        disposable: Disposable,
    ) {
        private val updater = ZipperUpdater(200, Alarm.ThreadToUse.POOLED_THREAD, disposable)
        private val dirtyTomlFiles: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet()
        private val runnable =
            Runnable {
                if (project.isDisposed) return@Runnable
                val scope = HashSet(dirtyTomlFiles)
                project.messageBus.syncPublisher(MISE_TOML_CHANGED).invoke()
                dirtyTomlFiles.removeAll(scope)
            }

        fun onFileChange(file: VirtualFile) {
            if (MiseTomlFile.isMiseTomlFile(project, file)) {
                dirtyTomlFiles.add(file)
                updater.queue(runnable)
            }
        }
    }
}
