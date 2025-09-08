package com.github.l34130.mise.python.run

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonRunConfigurationExtension
import org.jdom.Element

class PythonRunConfigurationExtension : PythonRunConfigurationExtension() {
    override fun isApplicableFor(runConfiguration: AbstractPythonRunConfiguration<*>): Boolean = true

    override fun isEnabledFor(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean = true

    override fun writeExternal(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun readExternal(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun getEditorTitle(): String = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : AbstractPythonRunConfiguration<*>> createEditor(configuration: P): SettingsEditor<P> =
        MiseRunConfigurationSettingsEditor(configuration.project)

    override fun patchCommandLine(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String,
    ) {
        val project = runConfiguration.project
        val projectState = project.service<MiseSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(runConfiguration)

        val (workDir, configEnvironment) =
            when {
                projectState.useMiseDirEnv -> project.basePath to projectState.miseConfigEnvironment
                runConfigState?.useMiseDirEnv == true -> {
                    val pythonWorkDir = runConfiguration.workingDirectory ?: runConfiguration.projectPathOnTarget
                    (pythonWorkDir ?: runConfiguration.project.basePath) to runConfigState.miseConfigEnvironment
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

        cmdLine.withEnvironment(envVars)
    }
}
