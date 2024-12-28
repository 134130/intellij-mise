package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.components.service
import com.intellij.util.application
import java.util.function.Supplier

object MiseHelper {
    fun getMiseEnvVarsOrNotify(
        configuration: RunConfigurationBase<*>,
        workingDirectory: Supplier<String?>,
    ): Map<String, String> {
        val project = configuration.project
        val projectState = application.service<MiseSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)

        val (workDir, configEnvironment) =
            when {
                projectState.useMiseDirEnv -> {
                    project.basePath to projectState.miseConfigEnvironment
                }
                runConfigState?.useMiseDirEnv == true -> {
                    (workingDirectory.get() ?: project.basePath) to runConfigState.miseConfigEnvironment
                }
                else -> return emptyMap()
            }

        return MiseCommandLineHelper
            .getEnvVars(workDir, configEnvironment)
            .fold(
                onSuccess = { envVars -> envVars },
                onFailure = {
                    if (it !is MiseCommandLineNotFoundException) {
                        MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it)
                    }
                    emptyMap()
                },
            )
    }
}
