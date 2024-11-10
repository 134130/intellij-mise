package com.github.l34130.mise.settings.ui

import com.github.l34130.mise.settings.MiseSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.Key
import org.jdom.Element
import javax.swing.JComponent

class MiseConfigurationPanelEditor<T : RunConfigurationBase<*>>(
    configuration: T,
) : SettingsEditor<T>() {
    private val editor = MiseRunConfigurationPanel()

    override fun resetEditorFrom(config: T) {
        val defaultState = getDefaultState()
        val isMiseEnabled = config.getCopyableUserData(USER_DATA_KEY)?.isMiseEnabled ?: defaultState.isMiseEnabled
        val miseProfile = config.getCopyableUserData(USER_DATA_KEY)?.miseProfile ?: defaultState.miseProfile
        editor.state = MiseSettings.State(
            isMiseEnabled = isMiseEnabled,
            miseProfile = miseProfile,
        )
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
        private const val FIELD_MISE_PROFILE = "MISE_PROFILE"

        fun readExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            val defaultState = getDefaultState()
            val isMiseEnabled =
                element.getAttributeValue(FIELD_MISE_ENABLED)?.toBoolean() ?: defaultState.isMiseEnabled
            val miseProfile = element.getAttributeValue(FIELD_MISE_PROFILE) ?: defaultState.miseProfile

            runConfiguration.putCopyableUserData(USER_DATA_KEY, MiseSettings.State(isMiseEnabled = isMiseEnabled))
            runConfiguration.putCopyableUserData(USER_DATA_KEY, MiseSettings.State(miseProfile = miseProfile))
        }

        fun writeExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            runConfiguration.getCopyableUserData(USER_DATA_KEY)?.let { settings ->
                element.setAttribute(FIELD_MISE_ENABLED, settings.isMiseEnabled.toString())
                element.setAttribute(FIELD_MISE_PROFILE, settings.miseProfile)
            }
        }

        fun getDefaultState(): MiseSettings.State = MiseSettings.State(
            isMiseEnabled = MiseSettings.instance.state.isMiseEnabled,
            miseProfile = MiseSettings.instance.state.miseProfile,
        )

        fun isMiseEnabled(configuration: RunConfigurationBase<*>): Boolean =
            configuration.getCopyableUserData(USER_DATA_KEY)?.isMiseEnabled ?: getDefaultState().isMiseEnabled

        fun getMiseProfile(configuration: RunConfigurationBase<*>): String =
            configuration.getCopyableUserData(USER_DATA_KEY)?.miseProfile ?: getDefaultState().miseProfile
    }
}
