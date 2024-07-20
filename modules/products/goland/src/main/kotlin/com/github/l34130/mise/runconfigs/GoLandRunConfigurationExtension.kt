package com.github.l34130.mise.runconfigs

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.settings.ui.RunConfigurationSettingsEditor
import com.goide.execution.GoRunConfigurationBase
import com.goide.execution.GoRunningState
import com.goide.execution.extension.GoRunConfigurationExtension
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.target.TargetedCommandLineBuilder
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class GoLandRunConfigurationExtension : GoRunConfigurationExtension() {
    override fun getEditorTitle(): String = RunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : GoRunConfigurationBase<*>> createEditor(configuration: P): SettingsEditor<P> =
        RunConfigurationSettingsEditor(configuration)

    override fun getSerializationId(): String = RunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: GoRunConfigurationBase<*>,
        element: Element,
    ) {
        RunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: GoRunConfigurationBase<*>,
        element: Element,
    ) {
        RunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun patchCommandLine(
        configuration: GoRunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: TargetedCommandLineBuilder,
        runnerId: String,
        state: GoRunningState<out GoRunConfigurationBase<*>>,
        commandLineType: GoRunningState.CommandLineType,
    ) {
        if (RunConfigurationSettingsEditor.isMiseEnabled(configuration)) {
            MiseCmd.loadEnv(configuration.getWorkingDirectory()).forEach { (k, v) ->
                cmdLine.addEnvironmentVariable(k, v)
            }
        }
        super.patchCommandLine(configuration, runnerSettings, cmdLine, runnerId, state, commandLineType)
    }

    override fun isApplicableFor(configuration: GoRunConfigurationBase<*>): Boolean = true

    override fun isEnabledFor(
        applicableConfiguration: GoRunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean = true
}
