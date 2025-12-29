package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.application
import com.jetbrains.rd.util.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class MiseProjectService(
    val project: Project,
    private val cs: CoroutineScope,
) : Disposable {
    var isInitialized: AtomicBoolean = AtomicBoolean(false)
        private set
    
    private val isCheckingInstall: AtomicBoolean = AtomicBoolean(false)

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
        
        // Check and run mise install if configured (non-blocking)
        cs.launch(Dispatchers.IO) {
            checkAndRunMiseInstall()
        }

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

        val settings = project.service<MiseProjectSettings>()
        val configEnvironment = settings.state.miseConfigEnvironment
        val miseTasks = project.service<MiseTaskResolver>().getMiseTasks(baseDir, true, configEnvironment)
        tasks.addAll(miseTasks)
    }
    
    private suspend fun checkAndRunMiseInstall() {
        // Prevent concurrent checks
        if (!isCheckingInstall.compareAndSet(false, true)) {
            logger.debug { "Install check already in progress, skipping" }
            return
        }
        
        try {
            val settings = project.service<MiseProjectSettings>()
            
            // Only run if the setting is enabled
            if (!settings.state.runMiseInstallBeforeRun) {
                return
            }
            
            val configEnvironment = settings.state.miseConfigEnvironment
            val workDir = project.basePath
            
            logger.debug { "Checking tools installation status..." }
            
            // Get the list of dev tools to check if any are not installed
            val toolsResult = MiseCommandLineHelper.getDevTools(
                workDir = workDir,
                configEnvironment = configEnvironment
            )
            
            toolsResult.fold(
                onSuccess = { devTools ->
                    val notInstalledTools = devTools.values.flatten().filter { !it.installed }
                    
                    if (notInstalledTools.isNotEmpty()) {
                        logger.debug { "Found ${notInstalledTools.size} tools not installed. Running mise install..." }
                        
                        // Run mise install
                        val installResult = runMiseInstallBlocking(workDir, configEnvironment)
                        
                        installResult.onSuccess {
                            val miseNotificationService = project.service<MiseNotificationService>()
                            miseNotificationService.info(
                                "Mise tools installed",
                                "Ran 'mise install --yes' successfully"
                            )
                        }
                        
                        installResult.onFailure { exception ->
                            if (exception !is MiseCommandLineNotFoundException && exception !is ProcessCanceledException) {
                                MiseNotificationServiceUtils.notifyException(
                                    "Failed to run 'mise install --yes'",
                                    exception,
                                    project
                                )
                            }
                        }
                    } else {
                        logger.debug { "All tools are already installed" }
                    }
                },
                onFailure = { exception ->
                    if (exception !is MiseCommandLineNotFoundException) {
                        logger.warn("Failed to check tool installation status", exception)
                    }
                }
            )
        } finally {
            isCheckingInstall.set(false)
        }
    }
    
    private fun runMiseInstallBlocking(
        workingDirectory: String?,
        configEnvironment: String?,
    ): Result<String> {
        return try {
            if (application.isDispatchThread) {
                runWithModalProgressBlocking(project, "Running 'mise install --yes'") {
                    MiseCommandLineHelper.install(workingDirectory, configEnvironment)
                }
            } else if (!application.isReadAccessAllowed) {
                var result: Result<String>? = null
                application.invokeAndWait {
                    runWithModalProgressBlocking(project, "Running 'mise install --yes'") {
                        result = MiseCommandLineHelper.install(workingDirectory, configEnvironment)
                    }
                }
                result ?: Result.failure(ProcessCanceledException("Progress was cancelled"))
            } else {
                runBlocking(Dispatchers.IO) {
                    MiseCommandLineHelper.install(workingDirectory, configEnvironment)
                }
            }
        } catch (e: Exception) {
            if (e is ProcessCanceledException) throw e
            logger.warn("Failed to run mise install", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private val logger = Logger.getInstance(MiseProjectService::class.java)
    }
}
