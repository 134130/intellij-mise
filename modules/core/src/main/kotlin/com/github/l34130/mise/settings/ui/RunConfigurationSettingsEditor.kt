package com.github.l34130.mise.settings.ui

import com.github.l34130.mise.settings.MiseSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.Key
import org.jdom.Element
import javax.swing.JComponent

class RunConfigurationSettingsEditor<T : RunConfigurationBase<*>>(
    configuration: T,
) : SettingsEditor<T>() {
    private val editor = RunConfigurationSettingsPanel()

    override fun resetEditorFrom(config: T) {
        config.getCopyableUserData(USER_DATA_KEY)?.let {
            editor.state = it
        }
    }

    @Throws(ConfigurationException::class)
    override fun applyEditorTo(config: T) {
        config.putCopyableUserData(USER_DATA_KEY, editor.state)
    }

    override fun createEditor(): JComponent = editor

    companion object {
        const val EDITOR_TITLE = "Mise"
        const val SERIALIZATION_ID = "com.github.l34130.mise"
        val USER_DATA_KEY: Key<MiseSettings.State> = Key("Mise Settings")

        private const val FIELD_MISE_ENABLED = "MISE_ENABLED"

        fun readExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            val isMiseEnabled = element.getAttributeValue(FIELD_MISE_ENABLED)?.toBoolean() ?: false
            runConfiguration.putCopyableUserData(USER_DATA_KEY, MiseSettings.State(isMiseEnabled = isMiseEnabled))
        }

        fun writeExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            runConfiguration.getCopyableUserData(USER_DATA_KEY)?.let { settings ->
                element.setAttribute(FIELD_MISE_ENABLED, settings.isMiseEnabled.toString())
            }
        }

        fun isMiseEnabled(configuration: RunConfigurationBase<*>): Boolean =
            configuration.getCopyableUserData(USER_DATA_KEY)?.isMiseEnabled ?: false
    }
}
