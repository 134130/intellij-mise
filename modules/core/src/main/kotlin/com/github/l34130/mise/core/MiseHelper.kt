package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.ConfigEnvironmentStrategy
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object MiseHelper {
    fun runMiseInstallIfNeeded(
        configuration: RunConfigurationBase<*>,
        workingDirectory: String?,
    ) {
        val project = configuration.project
        val projectState = project.service<MiseProjectSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)

        val isRunConfigDisabled = runConfigState?.useMiseDirEnv == false
        val useOverrideSettings = runConfigState?.configEnvironmentStrategy == ConfigEnvironmentStrategy.OVERRIDE_PROJECT_SETTINGS
        val useProjectSettings = projectState.useMiseDirEnv

        // Check if we should run mise install
        val shouldRunInstall = when {
            isRunConfigDisabled -> false
            runConfigState?.runMiseInstallBeforeRun == true -> true
            runConfigState?.runMiseInstallBeforeRun == null && projectState.runMiseInstallBeforeRun -> true
            else -> false
        }

        if (!shouldRunInstall) return

        val (workDir, configEnvironment) =
            when {
                useOverrideSettings -> {
                    val workDir = workingDirectory?.takeIf { it.isNotBlank() } ?: project.basePath
                    workDir to runConfigState.miseConfigEnvironment
                }
                useProjectSettings -> project.basePath to projectState.miseConfigEnvironment
                else -> return
            }

        logger.debug { "Running mise install before run configuration" }
        val result = runMiseInstall(project, workDir, configEnvironment)
        result.onFailure { exception ->
            if (exception !is MiseCommandLineNotFoundException && exception !is ProcessCanceledException) {
                MiseNotificationServiceUtils.notifyException("Failed to run 'mise install --yes'", exception, project)
            }
        }
    }

    fun getMiseEnvVarsOrNotify(
        configuration: RunConfigurationBase<*>,
        workingDirectory: String?,
    ): Map<String, String> {
        val project = configuration.project
        val projectState = project.service<MiseProjectSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)

        val isRunConfigDisabled = runConfigState?.useMiseDirEnv == false
        val useOverrideSettings = runConfigState?.configEnvironmentStrategy == ConfigEnvironmentStrategy.OVERRIDE_PROJECT_SETTINGS
        val useProjectSettings = projectState.useMiseDirEnv

        val (workDir, configEnvironment) =
            when {
                isRunConfigDisabled -> return emptyMap()
                useOverrideSettings -> {
                    val workDir = workingDirectory?.takeIf { it.isNotBlank() } ?: project.basePath
                    workDir to runConfigState.miseConfigEnvironment
                }
                useProjectSettings -> project.basePath to projectState.miseConfigEnvironment
                else -> return emptyMap()
            }

        return getMiseEnvVarsOrNotify(project, workDir, configEnvironment)
    }

    fun getMiseEnvVarsOrNotify(
        project: Project?,
        workingDirectory: String? = null,
        configEnvironment: String? = null,
    ): Map<String, String> {
        val project =
            project ?: workingDirectory?.let { workDir ->
                LocalFileSystem.getInstance().findFileByPath(workDir)?.let { vf ->
                    ProjectLocator.getInstance().guessProjectForFile(vf)
                }
            } ?: ProjectUtil.getActiveProject() ?: ProjectUtil.getOpenProjects().firstOrNull()

        if (project == null) {
            logger.warn("No project found to load Mise environment variables")
            return emptyMap()
        }

        val projectState = project.service<MiseProjectSettings>().state

        val useMiseDirEnv = projectState.useMiseDirEnv
        if (!useMiseDirEnv) {
            logger.debug { "Mise environment variables loading is disabled in project settings" }
            return emptyMap()
        }

        val configEnvironment = configEnvironment ?: projectState.miseConfigEnvironment

        val result =
            if (application.isDispatchThread) {
                logger.debug { "dispatch thread detected, loading env vars on current thread" }
                runWithModalProgressBlocking(project, "Loading Mise Environment Variables") {
                    MiseCommandLineHelper.getEnvVars(workingDirectory, configEnvironment)
                }
            } else if (!application.isReadAccessAllowed) {
                logger.debug { "no read lock detected, loading env vars on dispatch thread" }
                var result: Result<Map<String, String>>? = null
                application.invokeAndWait {
                    logger.debug { "loading env vars on invokeAndWait" }
                    runWithModalProgressBlocking(project, "Loading Mise Environment Variables") {
                        result = MiseCommandLineHelper.getEnvVars(workingDirectory, configEnvironment)
                    }
                }
                result ?: throw ProcessCanceledException()
            } else {
                logger.debug { "read access allowed, executing on background thread" }
                runBlocking(Dispatchers.IO) {
                    MiseCommandLineHelper.getEnvVars(workingDirectory, configEnvironment)
                }
            }

        return result
            .fold(
                onSuccess = { envVars -> envVars },
                onFailure = {
                    if (it !is MiseCommandLineNotFoundException) {
                        MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it, project)
                    }
                    mapOf()
                },
            )
    }

    fun runMiseInstall(
        project: Project,
        workingDirectory: String?,
        configEnvironment: String?,
    ): Result<String> {
        logger.debug { "Running mise install in directory: $workingDirectory" }

        return try {
            if (application.isDispatchThread) {
                logger.debug { "dispatch thread detected, running mise install on current thread" }
                runWithModalProgressBlocking(project, "Running 'mise install --yes'") {
                    MiseCommandLineHelper.install(workingDirectory, configEnvironment)
                }
            } else if (!application.isReadAccessAllowed) {
                logger.debug { "no read lock detected, running mise install on dispatch thread" }
                var result: Result<String>? = null
                application.invokeAndWait {
                    logger.debug { "running mise install on invokeAndWait" }
                    runWithModalProgressBlocking(project, "Running 'mise install --yes'") {
                        result = MiseCommandLineHelper.install(workingDirectory, configEnvironment)
                    }
                }
                result ?: throw ProcessCanceledException()
            } else {
                logger.debug { "read access allowed, executing on background thread" }
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

    private val logger = Logger.getInstance(MiseHelper::class.java)
}
