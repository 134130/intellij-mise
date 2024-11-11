package com.github.l34130.mise.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

data class MiseState(
    var isMiseEnabled: Boolean = true,
    var miseProfile: String = ".",
) : Cloneable {
    public override fun clone(): MiseState =
        MiseState(
            isMiseEnabled = isMiseEnabled,
            miseProfile = miseProfile,
        )
}

@Service(Service.Level.PROJECT)
@State(name = "com.github.l34130.mise.settings.MiseSettings", storages = [Storage("mise.xml")])
class MiseSettings(
    private val project: Project,
) : PersistentStateComponent<MiseState> {
    private var state = MiseState()

    override fun getState() = state

    override fun loadState(state: MiseState) {
        this.state = state.clone()
    }

    companion object {
        fun getService(project: Project): MiseSettings = project.service<MiseSettings>()

        val MISE_SETTINGS_TOPIC = Topic.create("Mise Settings", SettingsChangeListener::class.java)
        val instance: MiseSettings = ApplicationManager.getApplication().getService(MiseSettings::class.java)
    }

    interface SettingsChangeListener {
        fun settingsChanged(
            oldState: MiseState,
            newState: MiseState)
    }
}
