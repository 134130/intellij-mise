package com.github.l34130.mise.pycharm.run

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.intellij.execution.Location
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.options.SettingsEditor
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonRunConfigurationExtension
import org.jdom.Element

class MisePythonRunConfigurationExtension : PythonRunConfigurationExtension() {
    override fun isApplicableFor(runConfiguration: AbstractPythonRunConfiguration<*>): Boolean = true

    override fun isEnabledFor(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean = true

    override fun <P : AbstractPythonRunConfiguration<*>> createEditor(configuration: P): SettingsEditor<P> =
        MiseRunConfigurationSettingsEditor()

    override fun getSerializationId(): String = MiseRunConfigurationSettingsEditor.SERIALIZATION_ID

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

    override fun patchCommandLine(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String,
    ) {
        val envVars =
            MiseHelper.getMiseEnvVarsOrNotify(
                configuration = runConfiguration,
                workingDirectory = runConfiguration.workingDirectory,
            )

        cmdLine.withEnvironment(envVars)
    }

    override fun extendCreatedConfiguration(
        configuration: AbstractPythonRunConfiguration<*>,
        location: Location<*>,
    ) {
        super.extendCreatedConfiguration(configuration, location)
    }
}
