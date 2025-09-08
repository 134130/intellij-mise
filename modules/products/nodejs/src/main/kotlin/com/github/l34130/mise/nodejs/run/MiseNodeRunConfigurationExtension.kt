package com.github.l34130.mise.nodejs.run

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.execution.runConfiguration.AbstractNodeRunConfigurationExtension
import com.intellij.javascript.nodejs.execution.runConfiguration.NodeRunConfigurationLaunchSession
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import com.jetbrains.nodejs.run.NodeJsRunConfiguration
import org.jdom.Element

class MiseNodeRunConfigurationExtension : AbstractNodeRunConfigurationExtension() {
    companion object {
        private val LOG = Logger.getInstance(MiseNodeRunConfigurationExtension::class.java)
    }

    override fun getEditorTitle(): String = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : AbstractNodeTargetRunProfile> createEditor(configuration: P): SettingsEditor<P> = MiseRunConfigurationSettingsEditor()

    override fun getSerializationId(): String = MiseRunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: AbstractNodeTargetRunProfile,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: AbstractNodeTargetRunProfile,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun createLaunchSession(
        configuration: AbstractNodeTargetRunProfile,
        environment: ExecutionEnvironment,
    ): NodeRunConfigurationLaunchSession? {
        val envVars =
            MiseHelper.getMiseEnvVarsOrNotify(
                configuration = configuration,
                workingDirectory = {
                    when (configuration) {
                        is NodeJsRunConfiguration -> configuration.workingDirectory
                        else -> null
                    }
                },
            )

        return object : NodeRunConfigurationLaunchSession() {
            override fun addNodeOptionsTo(targetRun: NodeTargetRun) {
                for ((key, value) in envVars) {
                    targetRun.commandLineBuilder.addEnvironmentVariable(key, value)
                }
            }
        }
    }

    override fun isApplicableFor(configuration: AbstractNodeTargetRunProfile): Boolean = true
}
