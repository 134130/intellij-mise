package com.github.l34130.mise.settings

import com.github.l34130.mise.notifications.Notification
import com.github.l34130.mise.settings.ui.RunConfigurationSettingsEditor
import com.github.l34130.mise.settings.ui.RunConfigurationSettingsPanel
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class MiseConfigurable : SearchableConfigurable {
    private var component: RunConfigurationSettingsPanel? = null

    override fun createComponent(): JComponent {
        component = component ?: RunConfigurationSettingsPanel().also {
            it.state = MiseSettings.instance.state
        }
        return component!!
    }

    override fun isModified(): Boolean {
        return component?.let {
            it.enableMiseCheckBox.isSelected != MiseSettings.instance.state.isMiseEnabled
        } ?: false
    }

    override fun apply() {
        Notification.notify("apply()", NotificationType.INFORMATION)

        component?.let {
            MiseSettings.instance.state.isMiseEnabled = it.enableMiseCheckBox.isSelected
        }
    }

    override fun reset() {
        component?.state = MiseSettings.instance.state
    }

    override fun disposeUIResources() {
        component = null
    }

    override fun getDisplayName(): String = RunConfigurationSettingsEditor.EDITOR_TITLE

    override fun getId(): String = RunConfigurationSettingsEditor.SERIALIZATION_ID
}
