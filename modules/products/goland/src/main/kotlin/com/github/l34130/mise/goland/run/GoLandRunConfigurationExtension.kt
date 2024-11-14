package com.github.l34130.mise.goland.run

import com.github.l34130.mise.core.command.MiseCmd
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.goide.execution.GoRunConfigurationBase
import com.goide.execution.GoRunningState
import com.goide.execution.extension.GoRunConfigurationExtension
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.target.TargetedCommandLineBuilder
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class GoLandRunConfigurationExtension : GoRunConfigurationExtension() {
    override fun getEditorTitle(): String = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : GoRunConfigurationBase<*>> createEditor(configuration: P): SettingsEditor<P> =
        MiseRunConfigurationSettingsEditor(configuration.getProject())

    override fun getSerializationId(): String = MiseRunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: GoRunConfigurationBase<*>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: GoRunConfigurationBase<*>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun patchCommandLine(
        configuration: GoRunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: TargetedCommandLineBuilder,
        runnerId: String,
        state: GoRunningState<out GoRunConfigurationBase<*>>,
        commandLineType: GoRunningState.CommandLineType,
    ) {
        val miseState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)
        if (miseState?.useMiseDirEnv == true) {
            MiseCmd
                .loadEnv(
                    workDir = configuration.getWorkingDirectory(),
                    miseProfile = miseState.miseProfile,
                    project = configuration.getProject(),
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
