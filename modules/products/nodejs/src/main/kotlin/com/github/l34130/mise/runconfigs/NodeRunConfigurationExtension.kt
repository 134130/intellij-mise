package com.github.l34130.mise.runconfigs

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.settings.ui.RunConfigurationSettingsEditor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile
import com.intellij.javascript.nodejs.execution.runConfiguration.AbstractNodeRunConfigurationExtension
import com.intellij.javascript.nodejs.execution.runConfiguration.NodeRunConfigurationLaunchSession
import com.intellij.openapi.options.SettingsEditor
import com.jetbrains.nodejs.run.NodeJsRunConfiguration
import org.jdom.Element

class NodeRunConfigurationExtension : AbstractNodeRunConfigurationExtension() {
    override fun getEditorTitle(): String = RunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : AbstractNodeTargetRunProfile> createEditor(configuration: P): SettingsEditor<P> =
        RunConfigurationSettingsEditor(configuration)

    override fun getSerializationId(): String = RunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: AbstractNodeTargetRunProfile,
        element: Element,
    ) {
        RunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: AbstractNodeTargetRunProfile,
        element: Element,
    ) {
        RunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun createLaunchSession(
        configuration: AbstractNodeTargetRunProfile,
        environment: ExecutionEnvironment,
    ): NodeRunConfigurationLaunchSession? {
        val config = configuration as NodeJsRunConfiguration
        config.envs.putAll(MiseCmd.loadEnv(config.workingDirectory))
        return null
    }

    override fun isApplicableFor(configuration: AbstractNodeTargetRunProfile): Boolean = true
}
