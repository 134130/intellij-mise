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
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.openapi.vfs.VirtualFilePropertyEvent
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.util.Alarm
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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
                updater.onVfsChange(fileOrDirectory)
            }

            override fun onBeforeFileChange(fileOrDirectory: VirtualFile) = Unit

            // Called AFTER property change - file now has NEW value
            override fun propertyChanged(event: VirtualFilePropertyEvent) {
                if (event.propertyName == VirtualFile.PROP_NAME) {
                    val oldName = event.oldValue as? String ?: return
                    val newName = event.newValue as? String ?: return
                    updater.onVfsRename(event.file, oldName, newName)
                }
            }

            override fun fileMoved(event: VirtualFileMoveEvent) {
                updater.onVfsMove(event.file, event.oldParent, event.newParent)
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
                                updater.onPsiChange(file.viewProvider.virtualFile)
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
            private val vfsChanged = AtomicBoolean(false)
            private val psiChanged = AtomicBoolean(false)
            private val runnable =
                Runnable {
                    if (project.isDisposed) return@Runnable
                    val scope = HashSet(dirtyTomlFiles)
                    val shouldNotifyVfs = vfsChanged.getAndSet(false)
                    val shouldNotifyPsi = psiChanged.getAndSet(false)
                    if (shouldNotifyVfs) {
                        MiseProjectEventListener.broadcast(
                            project,
                            MiseProjectEvent(MiseProjectEvent.Kind.TOML_CHANGED, "mise toml changed (vfs)")
                        )
                    }
                    if (shouldNotifyPsi) {
                        MiseProjectEventListener.broadcast(
                            project,
                            MiseProjectEvent(MiseProjectEvent.Kind.TOML_PSI_CHANGED, "mise toml changed (psi)")
                        )
                    }
                    dirtyTomlFiles.removeAll(scope)
                }

            fun onVfsChange(file: VirtualFile) {
                if (MiseTomlFile.isMiseTomlFile(project, file)) {
                    dirtyTomlFiles.add(file)
                    vfsChanged.set(true)
                    updater.queue(runnable)
                }
            }

            fun onVfsRename(
                file: VirtualFile,
                oldName: String,
                newName: String,
            ) {
                // Rename events have the new name already applied; check both sides to detect enter/exit.
                val wasMiseToml = MiseTomlFile.isMiseTomlFile(project, file, oldName)
                val isMiseToml = MiseTomlFile.isMiseTomlFile(project, file, newName)
                if (wasMiseToml || isMiseToml) {
                    dirtyTomlFiles.add(file)
                    vfsChanged.set(true)
                    updater.queue(runnable)
                }
            }

            fun onVfsMove(
                file: VirtualFile,
                oldParent: VirtualFile,
                newParent: VirtualFile,
            ) {
                // Moves can change whether a config is in-scope without changing its name.
                val fileName = file.name
                val wasMiseToml = MiseTomlFile.isMiseTomlFile(project, file, fileName, oldParent)
                val isMiseToml = MiseTomlFile.isMiseTomlFile(project, file, fileName, newParent)
                if (wasMiseToml || isMiseToml) {
                    dirtyTomlFiles.add(file)
                    vfsChanged.set(true)
                    updater.queue(runnable)
                }
            }

            fun onPsiChange(file: VirtualFile) {
                // PSI updates are used for in-editor parsing only; CLI work listens to VFS events.
                if (MiseTomlFile.isMiseTomlFile(project, file)) {
                    dirtyTomlFiles.add(file)
                    psiChanged.set(true)
                    updater.queue(runnable)
                }
            }
        }
    }
}
