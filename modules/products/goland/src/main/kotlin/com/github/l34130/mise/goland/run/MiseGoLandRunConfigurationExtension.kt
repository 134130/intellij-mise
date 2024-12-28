package com.github.l34130.mise.goland.run

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseSettings
import com.goide.execution.GoRunConfigurationBase
import com.goide.execution.GoRunningState
import com.goide.execution.extension.GoRunConfigurationExtension
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.target.TargetedCommandLineBuilder
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class MiseGoLandRunConfigurationExtension : GoRunConfigurationExtension() {
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
        val project = configuration.getProject()
        val projectState = project.service<MiseSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)

        val (workDir, configEnvironment) =
            when {
                projectState.useMiseDirEnv -> project.basePath to projectState.miseConfigEnvironment
                runConfigState?.useMiseDirEnv == true -> configuration.getWorkingDirectory() to runConfigState.miseConfigEnvironment
                else -> return
            }

        MiseCommandLineHelper
            .getEnvVars(workDir, configEnvironment)
            .fold(
                onSuccess = { envVars -> envVars },
                onFailure = {
                    if (it !is MiseCommandLineNotFoundException) {
                        MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it)
                    }
                    emptyMap()
                },
            ).forEach { (k, v) -> cmdLine.addEnvironmentVariable(k, v) }
    }

    override fun isApplicableFor(configuration: GoRunConfigurationBase<*>): Boolean = true

    override fun isEnabledFor(
        applicableConfiguration: GoRunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean = true
}
