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
import com.intellij.util.Alarm
import com.intellij.util.application
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
        val debouncerFactory = project.service<MiseEventDebouncerFactory>()
        FileListener.startListening(project, this, connection, debouncerFactory)
    }

    override fun dispose() {
        // The MessageBusConnection created with connect(this) is automatically
        // disposed when this service is disposed, cleaning up all subscriptions
    }

    private class FileListener(
        updater: MiseLocalIndexUpdater,
    ) : BulkVirtualFileListenerAdapter(
            object : VirtualFileContentsChangedAdapter() {
                override fun onFileChange(fileOrDirectory: VirtualFile) = updater.onVfsChange(fileOrDirectory)

                override fun onBeforeFileChange(fileOrDirectory: VirtualFile) = updater.onVfsChange(fileOrDirectory)

                // Called BEFORE property change - file still has OLD value
                override fun beforePropertyChange(event: VirtualFilePropertyEvent) = updater.onVfsChange(event.file)

                // Called AFTER property change - file now has NEW value
                override fun propertyChanged(event: VirtualFilePropertyEvent) = updater.onVfsChange(event.file)
            },
        ) {
        companion object {
            fun startListening(
                project: Project,
                disposable: Disposable,
                connection: MessageBusConnection,
                debouncerFactory: MiseEventDebouncerFactory,
            ) {
                val debouncer = debouncerFactory.create(disposable)
                val updater = MiseLocalIndexUpdater(project, debouncer)
                connection.subscribe(VirtualFileManager.VFS_CHANGES, FileListener(updater))
                // PSI changes are intentionally ignored; we only refresh on VFS changes to avoid
                // frequent cache refresh while editing unsaved buffers.
            }
        }

        class MiseLocalIndexUpdater(
            val project: Project,
            private val debouncer: MiseEventDebouncer,
        ) {
            private val dirtyTomlFiles: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet()
            private val vfsChanged = AtomicBoolean(false)
            private val runnable =
                Runnable {
                    if (project.isDisposed) return@Runnable
                    val scope = HashSet(dirtyTomlFiles)
                    val shouldNotifyVfs = vfsChanged.getAndSet(false)
                    if (shouldNotifyVfs) {
                        MiseProjectEventListener.broadcast(
                            project,
                            MiseProjectEvent(MiseProjectEvent.Kind.TOML_CHANGED, "mise toml changed (vfs)"),
                        )
                    }
                    dirtyTomlFiles.removeAll(scope)
                }

            fun onVfsChange(file: VirtualFile) {
                if (isTrackedMiseInput(file)) {
                    dirtyTomlFiles.add(file)
                    vfsChanged.set(true)
                    debouncer.queue(runnable)
                }
            }

            private fun isTrackedMiseInput(file: VirtualFile): Boolean {
                if (project.service<MiseConfigFileResolver>().isTrackedPath(file)) return true
                if (MiseTomlFile.isMiseTomlFile(project, file)) return true
                return isLikelyMiseRelatedFile(file)
            }

            private fun isLikelyMiseRelatedFile(file: VirtualFile): Boolean {
                val name = file.name
                if (name == "config.toml" || name == "mise.toml" || name == ".mise.toml") return true
                if (name == "mise.local.toml" || name == ".mise.local.toml") return true
                if (name.startsWith(".env")) return true
                if (name.matches("^mise\\.[^/]+\\.toml$".toRegex())) return true
                if (name.matches("^\\.mise\\.[^/]+\\.toml$".toRegex())) return true
                return false
            }
        }
    }
}

/**
 * Debounces rapid-fire events into batched operations.
 *
 * Production: Uses 1000ms debounce to prevent refresh storms during bulk file changes
 * (e.g., git checkout, IDE project reloads, multi-file refactorings).
 *
 * Tests: Runs immediately for deterministic assertions without timing dependencies.
 */
fun interface MiseEventDebouncer {
    fun queue(runnable: Runnable)
}

@Service(Service.Level.PROJECT)
class MiseEventDebouncerFactory {
    fun create(disposable: Disposable): MiseEventDebouncer =
        if (application.isUnitTestMode) {
            ImmediateMiseEventDebouncer()
        } else {
            ZipperUpdaterDebouncer(1000, disposable)
        }
}

private class ZipperUpdaterDebouncer(
    delayMs: Int,
    disposable: Disposable,
) : MiseEventDebouncer {
    private val updater = ZipperUpdater(delayMs, Alarm.ThreadToUse.POOLED_THREAD, disposable)

    override fun queue(runnable: Runnable) {
        updater.queue(runnable)
    }
}

private class ImmediateMiseEventDebouncer : MiseEventDebouncer {
    override fun queue(runnable: Runnable) {
        runnable.run()
    }
}
