package com.github.l34130.mise.rust.run

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element
import org.rust.cargo.runconfig.CargoCommandConfigurationExtension
import org.rust.cargo.runconfig.ConfigurationExtensionContext
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

class MiseCargoRunConfigurationExtension : CargoCommandConfigurationExtension() {
    override fun getEditorTitle() = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    override fun getSerializationId() = MiseRunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun <P : CargoCommandConfiguration> createEditor(configuration: P): SettingsEditor<P> =
        MiseRunConfigurationSettingsEditor(configuration.project)

    override fun readExternal(
        runConfiguration: CargoCommandConfiguration,
        element: Element
    ) {
        MiseRunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: CargoCommandConfiguration,
        element: Element
    ) {
        MiseRunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun attachToProcess(
        configuration: CargoCommandConfiguration,
        handler: ProcessHandler,
        environment: ExecutionEnvironment,
        context: ConfigurationExtensionContext
    ) {
        val context = context
    }

    override fun patchCommandLine(
        configuration: CargoCommandConfiguration,
        environment: ExecutionEnvironment,
        cmdLine: GeneralCommandLine,
        context: ConfigurationExtensionContext
    ) {
        MiseHelper.getMiseEnvVarsOrNotify(configuration, configuration::getWorkingDirectory)
            .forEach { (k, v) -> cmdLine.withEnvironment(k, v) }
    }


    override fun isApplicableFor(configuration: CargoCommandConfiguration): Boolean = true

    override fun isEnabledFor(
        configuration: CargoCommandConfiguration,
        runnerSettings: RunnerSettings?
    ): Boolean = true
}