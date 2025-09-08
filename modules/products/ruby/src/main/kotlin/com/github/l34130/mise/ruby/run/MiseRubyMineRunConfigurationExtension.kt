package com.github.l34130.mise.ruby.run

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.NlsContexts
import org.jdom.Element
import org.jetbrains.plugins.ruby.ruby.run.configuration.AbstractRubyRunConfiguration
import org.jetbrains.plugins.ruby.ruby.run.configuration.RubyRunConfigurationExtension

class MiseRubyMineRunConfigurationExtension : RubyRunConfigurationExtension() {
    override fun getEditorTitle(): @NlsContexts.TabTitle String? = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    override fun getSerializationId(): String = MiseRunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun writeExternal(
        configuration: AbstractRubyRunConfiguration<*>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.writeExternal(configuration, element)
    }

    override fun readExternal(
        configuration: AbstractRubyRunConfiguration<*>,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.readExternal(configuration, element)
    }

    override fun <P : AbstractRubyRunConfiguration<*>> createEditor(configuration: P): SettingsEditor<P> =
        MiseRunConfigurationSettingsEditor()

    override fun isApplicableFor(configuration: AbstractRubyRunConfiguration<*>): Boolean = true

    override fun isEnabledFor(
        configuration: AbstractRubyRunConfiguration<*>,
        settings: RunnerSettings?,
    ): Boolean = true

    override fun patchCommandLine(
        configuration: AbstractRubyRunConfiguration<*>,
        settings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String,
    ) {
        MiseHelper
            .getMiseEnvVarsOrNotify(configuration, configuration::getWorkingDirectory)
            .forEach { (k, v) -> cmdLine.withEnvironment(k, v) }
    }
}
