package com.github.l34130.mise.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic

data class MiseState(
    var isMiseEnabled: Boolean = true,
    var miseProfile: String = "",
)

@State(name = "com.github.l34130.mise.settings.MiseSettings", storages = [Storage("mise.xml")])
class MiseSettings private constructor() : PersistentStateComponent<MiseState> {
    override fun getState() = STATE

    override fun loadState(state: MiseState) {
        val oldState = STATE.copy()
        STATE.isMiseEnabled = state.isMiseEnabled
        STATE.miseProfile = state.miseProfile
        ApplicationManager.getApplication().messageBus
            .syncPublisher(MISE_SETTINGS_TOPIC)
            .settingsChanged(oldState, state)
    }

    companion object {
        val MISE_SETTINGS_TOPIC = Topic.create("Mise Settings", SettingsChangeListener::class.java)
        private val STATE = MiseState()
        val instance: MiseSettings = ApplicationManager.getApplication().getService(MiseSettings::class.java)
    }

    interface SettingsChangeListener {
        fun settingsChanged(
            oldState: MiseState,
            newState: MiseState)
    }
}
