package com.github.l34130.mise.nodejs.run

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.execution.runConfiguration.AbstractNodeRunConfigurationExtension
import com.intellij.javascript.nodejs.execution.runConfiguration.NodeRunConfigurationLaunchSession
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import com.jetbrains.nodejs.run.NodeJsRunConfiguration
import org.jdom.Element

class NodeRunConfigurationExtension : AbstractNodeRunConfigurationExtension() {
    companion object {
        private val LOG = Logger.getInstance(NodeRunConfigurationExtension::class.java)
    }

    override fun getEditorTitle(): String = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : AbstractNodeTargetRunProfile> createEditor(configuration: P): SettingsEditor<P> =
        MiseRunConfigurationSettingsEditor(configuration.project)

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
        val project = configuration.project
        val projectState = project.service<MiseSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)

        val (workDir, configEnvironment) =
            when {
                projectState.useMiseDirEnv -> project.basePath to projectState.miseConfigEnvironment
                runConfigState?.useMiseDirEnv == true -> {
                    val nodejsWorkDir = when(configuration) {
                        is NodeJsRunConfiguration -> configuration.workingDirectory
                        else -> null
                    }

                    (nodejsWorkDir ?: project.basePath) to runConfigState.miseConfigEnvironment
                }
                else -> return null
            }

        return object : NodeRunConfigurationLaunchSession() {
            override fun addNodeOptionsTo(targetRun: NodeTargetRun) {
                val envVars = MiseCommandLineHelper.getEnvVars(workDir, configEnvironment)
                    .fold(
                        onSuccess = { envVars -> envVars },
                        onFailure = {
                            if (it !is MiseCommandLineNotFoundException) {
                                MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it)
                            }
                            emptyMap()
                        },
                    )

                for ((key, value) in envVars) {
                    targetRun.commandLineBuilder.addEnvironmentVariable(key, value)
                }
            }
        }
    }

    override fun isApplicableFor(configuration: AbstractNodeTargetRunProfile): Boolean = true
}
