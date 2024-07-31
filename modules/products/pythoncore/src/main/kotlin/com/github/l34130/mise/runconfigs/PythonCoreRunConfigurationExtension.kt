package com.github.l34130.mise.runconfigs

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.settings.ui.RunConfigurationSettingsEditor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.options.SettingsEditor
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonRunConfigurationExtension
import org.jdom.Element

class PythonCoreRunConfigurationExtension : PythonRunConfigurationExtension() {
    override fun getEditorTitle(): String = RunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : AbstractPythonRunConfiguration<*>> createEditor(configuration: P): SettingsEditor<P> =
        RunConfigurationSettingsEditor(configuration)

    override fun getSerializationId(): String = RunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        element: Element
    ) {
        RunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: AbstractPythonRunConfiguration<*>,
        element: Element
    ) {
        RunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun patchCommandLine(
        configuration: AbstractPythonRunConfiguration<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String
    ) {
        if (RunConfigurationSettingsEditor.isMiseEnabled(configuration)) {
            val workingDir = configuration.workingDirectory ?: return
            MiseCmd.loadEnv(workingDir).forEach { (key, value) ->
                cmdLine.environment[key] = value
            }
        }
    }

    override fun isApplicableFor(configuration: AbstractPythonRunConfiguration<*>): Boolean = true

    override fun isEnabledFor(
        applicableConfiguration: AbstractPythonRunConfiguration<*>,
        runnerSettings: RunnerSettings?
    ): Boolean = true
}