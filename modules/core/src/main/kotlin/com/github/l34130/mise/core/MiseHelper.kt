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
import java.util.function.Supplier

object MiseHelper {
    fun getMiseEnvVarsOrNotify(
        configuration: RunConfigurationBase<*>,
        workingDirectory: Supplier<String?>,
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
                    val workDir = workingDirectory.get()?.takeIf { it.isNotBlank() } ?: project.basePath
                    workDir to runConfigState.miseConfigEnvironment
                }
                useProjectSettings -> project.basePath to projectState.miseConfigEnvironment
                else -> return emptyMap()
            }

        return getMiseEnvVarsOrNotify(project, workDir, configEnvironment)
    }

    fun getMiseEnvVarsOrNotify(
        project: Project?,
        workDir: String?,
        configEnvironment: String?,
    ): Map<String, String> {
        val project =
            project ?: workDir?.let { workDir ->
                LocalFileSystem.getInstance().findFileByPath(workDir)?.let { vf ->
                    ProjectLocator.getInstance().guessProjectForFile(vf)
                }
            } ?: ProjectUtil.getActiveProject() ?: ProjectUtil.getOpenProjects().firstOrNull()
        val projectState = project?.service<MiseProjectSettings>()?.state
        val configEnvironment = configEnvironment ?: projectState?.miseConfigEnvironment

        val result =
            if (application.isDispatchThread) {
                logger.debug { "dispatch thread detected, loading env vars on current thread" }
                if (project == null) throw IllegalStateException("Cannot load Mise environment variables on EDT without a project")
                runWithModalProgressBlocking(project, "Loading Mise Environment Variables") {
                    MiseCommandLineHelper.getEnvVars(workDir, configEnvironment)
                }
            } else if (!application.isReadAccessAllowed) {
                logger.debug { "no read lock detected, loading env vars on dispatch thread" }
                var result: Result<Map<String, String>>? = null
                if (project == null) throw IllegalStateException("Cannot load Mise environment variables on EDT without a project")
                application.invokeAndWait {
                    logger.debug { "loading env vars on invokeAndWait" }
                    runWithModalProgressBlocking(project, "Loading Mise Environment Variables") {
                        result = MiseCommandLineHelper.getEnvVars(workDir, configEnvironment)
                    }
                }
                result ?: throw ProcessCanceledException()
            } else {
                logger.debug { "read access allowed, executing on background thread" }
                runBlocking(Dispatchers.IO) {
                    MiseCommandLineHelper.getEnvVars(workDir, configEnvironment)
                }
            }

        return result
            .fold(
                onSuccess = { envVars -> envVars },
                onFailure = {
                    if (it !is MiseCommandLineNotFoundException) {
                        if (project == null) {
                            logger.error("Failed to load environment variables, and no project to notify", it)
                        } else {
                            MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it, project)
                        }
                    }
                    mapOf()
                },
            )
    }

    private val logger = Logger.getInstance(MiseHelper::class.java)
}
