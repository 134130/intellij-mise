package com.github.l34130.mise.core

import com.github.l34130.mise.core.model.MiseTomlFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ZipperUpdater
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileContentsChangedAdapter
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.util.Alarm
import com.intellij.util.messages.MessageBusConnection
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
        fun startListening(project: Project, disposable: Disposable, connection: MessageBusConnection) {
            val updater = MiseLocalIndexUpdater(project, disposable)
            connection.subscribe(VirtualFileManager.VFS_CHANGES, MiseTomlFileVfsListener(updater))
            PsiManager.getInstance(project).addPsiTreeChangeListener(
                object : PsiTreeAnyChangeAbstractAdapter() {
                    override fun onChange(file: PsiFile?) {
                        if (file != null) {
                            updater.onFileChange(file.viewProvider.virtualFile)
                        }
                    }
                },
                disposable,
            )
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
                project.messageBus.syncPublisher(MiseTomlFileListener.MISE_TOML_CHANGED).invoke()
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
