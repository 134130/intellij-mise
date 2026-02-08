package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.model.MiseTomlFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Project-level service that manages the VFS listener for Mise TOML files and tracked config files.
 * This ensures that the listener is only registered once per project,
 * avoiding duplicate listener registrations.
 * 
 * Watches for changes to:
 * - Mise TOML configuration files (.mise.toml, mise.toml, etc.)
 * - External files tracked by mise (e.g., .env files loaded via env_file directive)
 */
@Service(Service.Level.PROJECT)
class MiseTomlFileListener(
    project: Project,
) : Disposable {
    init {
        // Ensure MiseTrackedConfigService is initialized early
        // This service will listen for events and update tracked configs
        project.service<MiseTrackedConfigService>()
        
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

            override fun onBeforeFileChange(fileOrDirectory: VirtualFile) {
                updater.onVfsChange(fileOrDirectory)
            }

            // Called BEFORE property change - file still has OLD value
            override fun beforePropertyChange(event: VirtualFilePropertyEvent) {
                updater.onVfsChange(event.file)
            }

            // Called AFTER property change - file now has NEW value
            override fun propertyChanged(event: VirtualFilePropertyEvent) {
                updater.onVfsChange(event.file)
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
                val isMiseToml = MiseTomlFile.isMiseTomlFile(project, file)
                // Check if this file is tracked by mise (e.g., .env file)
                val isTrackedConfig = !isMiseToml && project.service<MiseTrackedConfigService>().isTrackedConfig(file.path)
                
                if (isMiseToml || isTrackedConfig) {
                    dirtyTomlFiles.add(file)
                    vfsChanged.set(true)
                    updater.queue(runnable)
                }
            }

            fun onPsiChange(file: VirtualFile) {
                if (MiseTomlFile.isMiseTomlFile(project, file)) {
                    dirtyTomlFiles.add(file)
                    psiChanged.set(true)
                    updater.queue(runnable)
                }
            }
        }
    }
}
