package com.github.l34130.mise.runconfig

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.settings.ui.MiseConfigurationPanelEditor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class IdeaRunConfigurationExtension : RunConfigurationExtension() {
    override fun getEditorTitle(): String = MiseConfigurationPanelEditor.EDITOR_TITLE

    override fun <P : RunConfigurationBase<*>> createEditor(configuration: P): SettingsEditor<P> =
        MiseConfigurationPanelEditor(configuration)

    override fun getSerializationId(): String = MiseConfigurationPanelEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: RunConfigurationBase<*>,
        element: Element,
    ) {
        MiseConfigurationPanelEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: RunConfigurationBase<*>,
        element: Element,
    ) {
        MiseConfigurationPanelEditor.writeExternal(runConfiguration, element)
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

        if (MiseConfigurationPanelEditor.isMiseEnabled(configuration)) {
            params.env.putAll(
                MiseCmd.loadEnv(
                    workDir = params.workingDirectory,
                    miseProfile = MiseConfigurationPanelEditor.getMiseProfile(configuration),
                )
            )
        }
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean = true

    override fun isEnabledFor(
        applicableConfiguration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean = true
}
