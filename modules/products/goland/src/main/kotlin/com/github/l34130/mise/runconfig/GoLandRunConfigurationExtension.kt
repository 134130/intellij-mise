package com.github.l34130.mise.runconfig

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.settings.ui.MiseConfigurationPanelEditor
import com.goide.execution.GoRunConfigurationBase
import com.goide.execution.GoRunningState
import com.goide.execution.extension.GoRunConfigurationExtension
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.target.TargetedCommandLineBuilder
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class GoLandRunConfigurationExtension : GoRunConfigurationExtension() {
    override fun getEditorTitle(): String = MiseConfigurationPanelEditor.EDITOR_TITLE

    override fun <P : GoRunConfigurationBase<*>> createEditor(configuration: P): SettingsEditor<P> =
        MiseConfigurationPanelEditor(configuration)

    override fun getSerializationId(): String = MiseConfigurationPanelEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: GoRunConfigurationBase<*>,
        element: Element,
    ) {
        MiseConfigurationPanelEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: GoRunConfigurationBase<*>,
        element: Element,
    ) {
        MiseConfigurationPanelEditor.writeExternal(runConfiguration, element)
    }

    override fun patchCommandLine(
        configuration: GoRunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: TargetedCommandLineBuilder,
        runnerId: String,
        state: GoRunningState<out GoRunConfigurationBase<*>>,
        commandLineType: GoRunningState.CommandLineType,
    ) {
        if (MiseConfigurationPanelEditor.isMiseEnabled(configuration)) {
            MiseCmd.loadEnv(
                workDir = configuration.getWorkingDirectory(),
                miseProfile = MiseConfigurationPanelEditor.getMiseProfile(configuration)
            ).forEach { (k, v) ->
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
