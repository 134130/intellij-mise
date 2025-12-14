package com.github.l34130.mise.clion.run

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.cidr.execution.CidrRunConfigurationExtensionBase
import com.jetbrains.cidr.execution.ConfigurationExtensionContext
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.OCRunConfiguration
import org.jdom.Element

class MiseCidrRunConfigurationExtension : CidrRunConfigurationExtensionBase() {
    override fun isEnabledFor(
        runConfiguration: OCRunConfiguration<*, *>,
        toolEnvironment: CidrToolEnvironment,
        runnerSettings: RunnerSettings?,
    ): Boolean = true

    override fun isApplicableFor(runConfiguration: OCRunConfiguration<*, *>): Boolean = true

    override fun getSerializationId(): String = MiseRunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun getEditorTitle(): @NlsContexts.TabTitle String? = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    // NOTE: C/C++, CMake, Makefile doesn't allow extending the run configuration editor
    override fun <P : OCRunConfiguration<*, *>> createEditor(configuration: P): SettingsEditor<P> = MiseRunConfigurationSettingsEditor()

    override fun readExternal(
        runConfiguration: OCRunConfiguration<*, *>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: OCRunConfiguration<*, *>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun patchCommandLine(
        configuration: OCRunConfiguration<*, *>,
        runnerSettings: RunnerSettings?,
        environment: CidrToolEnvironment,
        cmdLine: GeneralCommandLine,
        runnerId: String,
        context: ConfigurationExtensionContext,
    ) {
        MiseHelper
            .getMiseEnvVarsOrNotify(configuration, configuration.getWorkingDirectory())
            .forEach { (k, v) -> cmdLine.withEnvironment(k, v) }
    }
}
