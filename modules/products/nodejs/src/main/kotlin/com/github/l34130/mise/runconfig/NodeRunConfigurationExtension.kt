package com.github.l34130.mise.runconfig

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.notifications.Notification
import com.github.l34130.mise.settings.ui.MiseConfigurationPanelEditor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile
import com.intellij.javascript.nodejs.execution.runConfiguration.AbstractNodeRunConfigurationExtension
import com.intellij.javascript.nodejs.execution.runConfiguration.NodeRunConfigurationLaunchSession
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import com.jetbrains.nodejs.run.NodeJsRunConfiguration
import org.jdom.Element

class NodeRunConfigurationExtension : AbstractNodeRunConfigurationExtension() {
    companion object {
        private val LOG = Logger.getInstance(NodeRunConfigurationExtension::class.java)
    }

    override fun getEditorTitle(): String = MiseConfigurationPanelEditor.EDITOR_TITLE

    override fun <P : AbstractNodeTargetRunProfile> createEditor(configuration: P): SettingsEditor<P> =
        MiseConfigurationPanelEditor(configuration)

    override fun getSerializationId(): String = MiseConfigurationPanelEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: AbstractNodeTargetRunProfile,
        element: Element,
    ) {
        MiseConfigurationPanelEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: AbstractNodeTargetRunProfile,
        element: Element,
    ) {
        MiseConfigurationPanelEditor.writeExternal(runConfiguration, element)
    }

    override fun createLaunchSession(
        configuration: AbstractNodeTargetRunProfile,
        environment: ExecutionEnvironment,
    ): NodeRunConfigurationLaunchSession? {
        if (!MiseConfigurationPanelEditor.isMiseEnabled(configuration)) {
            return null
        }

        try {
            when (configuration) {
                is NodeJsRunConfiguration -> {
                    val envs = configuration.envs.toMutableMap()
                    envs.putAll(
                        MiseCmd.loadEnv(
                            workDir = configuration.workingDirectory,
                            miseProfile = MiseConfigurationPanelEditor.getMiseProfile(configuration),
                        )
                    )
                    configuration.envs = envs
                }

                else -> {
                    LOG.warn("Unsupported configuration type: ${configuration.javaClass}")
                }
            }
        } catch (e: Exception) {
            LOG.error("Failed to load Mise environment", e)
            Notification.notify("Failed to load Mise environment: ${e.message}", NotificationType.ERROR)
        }
        return null
    }

    override fun isApplicableFor(configuration: AbstractNodeTargetRunProfile): Boolean = true
}
