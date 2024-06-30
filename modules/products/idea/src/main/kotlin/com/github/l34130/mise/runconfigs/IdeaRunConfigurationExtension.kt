package com.github.l34130.mise.runconfigs

import com.github.l34130.mise.commands.MiseEnvCmd
import com.github.l34130.mise.settings.ui.RunConfigurationSettingsEditor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class IdeaRunConfigurationExtension : RunConfigurationExtension() {
    override fun getEditorTitle(): String = RunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : RunConfigurationBase<*>> createEditor(configuration: P): SettingsEditor<P> =
        RunConfigurationSettingsEditor(configuration)

    override fun getSerializationId(): String = RunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: RunConfigurationBase<*>,
        element: Element,
    ) {
        RunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: RunConfigurationBase<*>,
        element: Element,
    ) {
        RunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun <T : RunConfigurationBase<*>> updateJavaParameters(
        configuration: T,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
    ) {
        val sourceEnv =
            GeneralCommandLine()
                .withEnvironment(params.env)
                .withParentEnvironmentType(
                    if (params.isPassParentEnvs) {
                        GeneralCommandLine.ParentEnvironmentType.CONSOLE
                    } else {
                        GeneralCommandLine.ParentEnvironmentType.NONE
                    },
                ).effectiveEnvironment
        params.env.putAll(sourceEnv)

        if (RunConfigurationSettingsEditor.isMiseEnabled(configuration)) {
            params.env.putAll(MiseEnvCmd(params.workingDirectory).load())
        }
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean = true

    override fun isEnabledFor(
        applicableConfiguration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean = true
}
