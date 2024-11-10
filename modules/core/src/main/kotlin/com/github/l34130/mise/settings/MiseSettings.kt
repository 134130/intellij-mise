package com.github.l34130.mise.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "com.github.l34130.mise.settings.MiseSettings", storages = [Storage("mise.xml")])
class MiseSettings private constructor() : PersistentStateComponent<MiseSettings.State> {
    override fun getState() = STATE

    override fun loadState(state: MiseSettings.State) {
        STATE.isMiseEnabled = state.isMiseEnabled
        STATE.miseProfile = state.miseProfile
    }

    companion object {
        private val STATE = State()
        val instance: MiseSettings = ApplicationManager.getApplication().getService(MiseSettings::class.java)
    }

    data class State(
        var isMiseEnabled: Boolean = true,
        var miseProfile: String = ""
    )
}
