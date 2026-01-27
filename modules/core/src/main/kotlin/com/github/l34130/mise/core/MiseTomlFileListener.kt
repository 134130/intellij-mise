package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.model.MiseTomlFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ZipperUpdater
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileContentsChangedAdapter
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFilePropertyEvent
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.util.Alarm
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.ConcurrentHashMap

/**
 * Project-level service that manages the VFS listener for Mise TOML files.
 * This ensures that the listener is only registered once per project,
 * avoiding duplicate listener registrations.
 */
@Service(Service.Level.PROJECT)
class MiseTomlFileListener(
    project: Project,
) : Disposable {
    init {
        // Register the VFS listener once for the entire project
        val connection = project.messageBus.connect(this)
        FileListener.startListening(project, this, connection)
    }

    override fun dispose() {
        // The MessageBusConnection created with connect(this) is automatically
        // disposed when this service is disposed, cleaning up all subscriptions
    }

    private class FileListener(
        updater: MiseLocalIndexUpdater,
    ) : BulkVirtualFileListenerAdapter(
        object : VirtualFileContentsChangedAdapter() {
            override fun onFileChange(fileOrDirectory: VirtualFile) {
                updater.onFileChange(fileOrDirectory)
            }

            override fun onBeforeFileChange(fileOrDirectory: VirtualFile) {
                updater.onFileChange(fileOrDirectory)
            }

            // Called BEFORE property change - file still has OLD value
            override fun beforePropertyChange(event: VirtualFilePropertyEvent) {
                updater.onFileChange(event.file)
            }

            // Called AFTER property change - file now has NEW value
            override fun propertyChanged(event: VirtualFilePropertyEvent) {
                updater.onFileChange(event.file)
            }
        }
    ) {
        companion object {
            fun startListening(project: Project, disposable: Disposable, connection: MessageBusConnection) {
                val updater = MiseLocalIndexUpdater(project, disposable)
                connection.subscribe(VirtualFileManager.VFS_CHANGES, FileListener(updater))
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
                    MiseProjectEventListener.broadcast(
                        project,
                        MiseProjectEvent(MiseProjectEvent.Kind.TOML_CHANGED, "mise toml changed")
                    )
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
}
