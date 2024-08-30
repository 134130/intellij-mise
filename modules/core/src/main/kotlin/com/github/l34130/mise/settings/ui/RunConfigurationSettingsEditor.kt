package com.github.l34130.mise.settings.ui

import com.github.l34130.mise.settings.MiseSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.Key
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import org.jdom.Element
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class RunConfigurationSettingsEditor<T : RunConfigurationBase<*>>(
    configuration: T,
) : SettingsEditor<T>() {
    private val editor = RunConfigurationSettingsPanel(configuration)

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
        val USER_DATA_KEY: Key<MiseSettings> = Key("Mise Settings")

        private const val FIELD_MISE_ENABLED = "MISE_ENABLED"

        fun readExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            val miseEnabled = element.getAttributeValue(FIELD_MISE_ENABLED)?.toBoolean() ?: false
            runConfiguration.putCopyableUserData(USER_DATA_KEY, MiseSettings(miseEnabled))
        }

        fun writeExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            runConfiguration.getCopyableUserData(USER_DATA_KEY)?.let { settings ->
                element.setAttribute(FIELD_MISE_ENABLED, settings.miseEnabled.toString())
            }
        }

        fun isMiseEnabled(configuration: RunConfigurationBase<*>): Boolean =
            configuration.getCopyableUserData(USER_DATA_KEY)?.miseEnabled ?: false
    }

    private class RunConfigurationSettingsPanel<T : RunConfigurationBase<*>>(
        private val configuration: T,
    ) : JPanel(BorderLayout()) {
        val enableMiseCheckBox = JBCheckBox("Enable mise")

        init {
            val p =
                panel {
                    row {
                        cell(enableMiseCheckBox).comment(
                            "Load environment variables from mise configuration file(s)",
                        )
                    }
                }

            add(p, BorderLayout.NORTH)
        }

        var state: MiseSettings
            get() = MiseSettings(enableMiseCheckBox.isSelected)
            set(value) {
                enableMiseCheckBox.isSelected = value.miseEnabled
            }
    }
}
