package com.github.l34130.mise.runconfigs

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.notifications.Notification
import com.github.l34130.mise.settings.ui.RunConfigurationSettingsEditor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonRunConfigurationExtension
import com.jetbrains.python.run.PythonRunParams
import com.jetbrains.python.run.target.HelpersAwareTargetEnvironmentRequest
import com.jetbrains.python.run.target.PythonCommandLineTargetEnvironmentProvider
import org.jdom.Element

class PyCharmRunConfigurationExtension :
    PythonRunConfigurationExtension(),
    PythonCommandLineTargetEnvironmentProvider {
    override fun getEditorTitle(): String = RunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : AbstractPythonRunConfiguration<*>> createEditor(configuration: P): SettingsEditor<P> =
        RunConfigurationSettingsEditor(configuration)

    override fun getSerializationId(): String = RunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        element: Element,
    ) {
        RunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        element: Element,
    ) {
        RunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun patchCommandLine(
        configuration: AbstractPythonRunConfiguration<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String,
    ) {
        Notification.notify("Patching command line for Pythonid run configuration", NotificationType.INFORMATION)
        Notification.notify("${cmdLine.environment}", NotificationType.INFORMATION)
        Notification.notify(configuration.getWorkingDirectorySafe(), NotificationType.INFORMATION)

        if (RunConfigurationSettingsEditor.isMiseEnabled(configuration)) {
            cmdLine.environment.putAll(MiseCmd.loadEnv(configuration.workingDirectory))
        }
    }

    override fun isApplicableFor(configuration: AbstractPythonRunConfiguration<*>): Boolean = true

    override fun isEnabledFor(
        applicableConfiguration: AbstractPythonRunConfiguration<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean = true

    override fun extendTargetEnvironment(
        project: Project,
        helpersAwareTargetRequest: HelpersAwareTargetEnvironmentRequest,
        pythonExecution: PythonExecution,
        runParams: PythonRunParams,
    ) {
        if (runParams is AbstractPythonRunConfiguration<*> &&
            RunConfigurationSettingsEditor.isMiseEnabled(runParams)
        ) {
            MiseCmd.loadEnv(runParams.workingDirectory).forEach { (k, v) ->
                pythonExecution.addEnvironmentVariable(k, v)
            }
            runParams.getEnvs().forEach { (k, v) ->
                pythonExecution.addEnvironmentVariable(k, v)
            }
        }
    }
}
