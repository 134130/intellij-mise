package com.github.l34130.mise.core

import com.github.l34130.mise.core.lang.MiseTomlFileType
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
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
import com.intellij.util.messages.Topic
import kotlinx.coroutines.Runnable
import java.util.concurrent.ConcurrentHashMap

class MiseTomlFileVfsListener private constructor(
    updater: MiseLocalIndexUpdater,
) : BulkVirtualFileListenerAdapter(
        object : VirtualFileContentsChangedAdapter() {
            override fun onFileChange(fileOrDirectory: VirtualFile) {
                updater.onFileChange(fileOrDirectory)
            }

            override fun onBeforeFileChange(fileOrDirectory: VirtualFile) {}
        },
    ) {
    companion object {
        val MISE_TOML_CHANGED = Topic.create("MISE_TOML_CHANGED", Runnable::class.java)

        fun startListening(
            project: Project,
            service: MiseService,
            connection: MessageBusConnection,
        ) {
            val updater = MiseLocalIndexUpdater(project, service)
            connection.subscribe(VirtualFileManager.VFS_CHANGES, MiseTomlFileVfsListener(updater))
            PsiManager.getInstance(project).addPsiTreeChangeListener(
                object : PsiTreeAnyChangeAbstractAdapter() {
                    override fun onChange(file: PsiFile?) {
                        if (file != null) {
                            updater.onFileChange(file.viewProvider.virtualFile)
                        }
                    }
                },
                service,
            )
        }
    }

    class MiseLocalIndexUpdater(
        val project: Project,
        val service: MiseService,
    ) {
        private val updater = ZipperUpdater(200, Alarm.ThreadToUse.POOLED_THREAD, service)
        private val dirtyTomlFiles: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet<VirtualFile>()
        private val runnable =
            Runnable {
                if (project.isDisposed) return@Runnable
                val scope = HashSet(dirtyTomlFiles)
                project.messageBus.syncPublisher(MISE_TOML_CHANGED).run()
                dirtyTomlFiles.removeAll(scope)

//                val analyzer = DaemonCodeAnalyzer.getInstance(project)
//                val psiManager = PsiManager.getInstance(project)
//                val editors = EditorFactory.getInstance().allEditors
//                for (editor in editors) {
//                    if (editor !is EditorEx || editor.project != project) continue
//
//                    val file = editor.virtualFile
//                    if (file == null || !file.isValid) continue

//                    val schemaFiles = service.getSchemasForFile(file, false, true)
//                    if (schemaFiles.contains { finalScope::contains }) {
//                        ReadAction
//                            .nonBlocking<Unit>(Callable { restartAnalyzer(analyzer, psiManager, file) })
//                            .expireWith(service)
//                            .submit(taskExecutor)
//                }
            }

        fun onFileChange(file: VirtualFile) {
            if (MiseTomlFileType.isMiseTomlFile(project, file)) {
                dirtyTomlFiles.add(file)
                updater.queue(runnable)
            }
        }

        private fun restartAnalyzer(
            analyzer: DaemonCodeAnalyzer,
            psiManager: PsiManager,
            file: VirtualFile,
        ) {
            if (psiManager.isDisposed) return
            if (!file.isValid) return
            val psiFile = psiManager.findFile(file) ?: return
            analyzer.restart(psiFile)
        }
    }
}
