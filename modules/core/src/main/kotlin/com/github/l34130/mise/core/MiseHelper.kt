package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.application
import java.util.function.Supplier

object MiseHelper {
    fun getMiseEnvVarsOrNotify(
        configuration: RunConfigurationBase<*>,
        workingDirectory: Supplier<String?>,
    ): Map<String, String> {
        val project = configuration.project
        val projectState = project.service<MiseProjectSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)

        val (workDir, configEnvironment) =
            when {
                projectState.useMiseDirEnv -> {
                    project.basePath to projectState.miseConfigEnvironment
                }
                runConfigState?.useMiseDirEnv == true -> {
                    (workingDirectory.get()?.takeIf { it.isNotBlank() } ?: project.basePath) to runConfigState.miseConfigEnvironment
                }
                else -> return emptyMap()
            }

        val result =
            if (application.isDispatchThread) {
                logger.debug { "dispatch thread detected, loading env vars on current thread" }
                runWithModalProgressBlocking(project, "Loading Mise Environment Variables") {
                    MiseCommandLineHelper.getEnvVars(workDir, configEnvironment)
                }
            } else if (!application.isReadAccessAllowed) {
                logger.debug { "no read lock detected, loading env vars on dispatch thread" }
                var result: Result<Map<String, String>>? = null
                application.invokeAndWait {
                    logger.debug { "loading env vars on invokeAndWait" }
                    runWithModalProgressBlocking(project, "Loading Mise Environment Variables") {
                        result = MiseCommandLineHelper.getEnvVars(workDir, configEnvironment)
                    }
                }
                result ?: throw ProcessCanceledException()
            } else {
                logger.debug { "unable to open the dialog. just load synchronously" }
                MiseCommandLineHelper.getEnvVars(workDir, configEnvironment)
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

    private val logger = Logger.getInstance(MiseHelper::class.java)
}
