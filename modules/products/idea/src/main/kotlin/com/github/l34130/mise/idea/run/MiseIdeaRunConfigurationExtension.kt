package com.github.l34130.mise.idea.run

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element
import java.util.concurrent.ConcurrentHashMap

open class MiseIdeaRunConfigurationExtension : RunConfigurationExtension() {
    override fun getEditorTitle(): String = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : RunConfigurationBase<*>> createEditor(configuration: P): SettingsEditor<P> = MiseRunConfigurationSettingsEditor()

    override fun getSerializationId(): String = MiseRunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: RunConfigurationBase<*>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: RunConfigurationBase<*>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun <T : RunConfigurationBase<*>> updateJavaParameters(
        configuration: T,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
    ) {
        val envVars = envProvider(configuration, resolveWorkingDirectory(configuration, params))
        if (envVars.isEmpty()) return

        params.env = params.env + envVars

        if (configuration is ExternalSystemRunConfiguration) {
            MiseIdeaRunConfigurationEnvironmentStore.snapshot(configuration)
            configuration.settings.env =
                HashMap(configuration.settings.env).apply {
                    putAll(envVars)
                }
        }
    }

    override fun attachToProcess(
        configuration: RunConfigurationBase<*>,
        handler: ProcessHandler,
        runnerSettings: RunnerSettings?,
    ) {
        if (configuration is ExternalSystemRunConfiguration) {
            handler.addProcessListener(
                object : ProcessListener {
                    override fun processTerminated(event: ProcessEvent) {
                        MiseIdeaRunConfigurationEnvironmentStore.restore(configuration)
                    }
                },
            )
        }
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean = true

    override fun isEnabledFor(
        applicableConfiguration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean = true

    private fun resolveWorkingDirectory(
        configuration: RunConfigurationBase<*>,
        params: JavaParameters,
    ): String? =
        if (configuration is ExternalSystemRunConfiguration) {
            params.workingDirectory ?: configuration.settings.externalProjectPath
        } else {
            params.workingDirectory
        }
}

internal object MiseIdeaRunConfigurationEnvironmentStore {
    private val runningProcessEnvs = ConcurrentHashMap<RunConfigurationBase<*>, Map<String, String>>()

    fun snapshot(configuration: ExternalSystemRunConfiguration) {
        runningProcessEnvs.putIfAbsent(configuration, configuration.settings.env.toMap())
    }

    fun restore(configuration: RunConfigurationBase<*>) {
        val externalSystemConfiguration = configuration as? ExternalSystemRunConfiguration ?: return
        val envsToRestore = runningProcessEnvs.remove(configuration) ?: return
        externalSystemConfiguration.settings.env = HashMap(envsToRestore)
    }

    fun hasSnapshot(configuration: RunConfigurationBase<*>): Boolean = runningProcessEnvs.containsKey(configuration)

    fun clear() {
        runningProcessEnvs.clear()
    }
}

internal var envProvider: (RunConfigurationBase<*>, String?) -> Map<String, String> =
    MiseHelper::getMiseEnvVarsOrNotify