package com.github.l34130.mise.python.run

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonRunParams
import com.jetbrains.python.run.target.HelpersAwareTargetEnvironmentRequest
import com.jetbrains.python.run.target.PythonCommandLineTargetEnvironmentProvider

@Suppress("UnstableApiUsage")
class PythonCommandLineTargetEnvironmentProvider : PythonCommandLineTargetEnvironmentProvider {
    override fun extendTargetEnvironment(
        project: Project,
        helpersAwareTargetRequest: HelpersAwareTargetEnvironmentRequest,
        pythonExecution: PythonExecution,
        runParams: PythonRunParams,
    ) {
        if (runParams is AbstractPythonRunConfiguration<*>) {
            val projectState = project.service<MiseSettings>().state
            val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(runParams)

            val (workDir, configEnvironment) =
                when {
                    projectState.useMiseDirEnv -> project.basePath to projectState.miseConfigEnvironment
                    runConfigState?.useMiseDirEnv == true -> {
                        val pythonWorkDir = runParams.workingDirectory ?: runParams.projectPathOnTarget
                        (pythonWorkDir ?: runParams.project.basePath) to runConfigState.miseConfigEnvironment
                    }
                    else -> null to null
                }

            val envVars =
                MiseCommandLineHelper
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

            envVars.forEach { (key, value) ->
                pythonExecution.addEnvironmentVariable(key, value)
            }
        }
    }
}
