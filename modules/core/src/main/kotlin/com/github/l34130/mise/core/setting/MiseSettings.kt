package com.github.l34130.mise.core.setting

import com.github.l34130.mise.core.notification.MiseNotificationService
import com.intellij.notification.NotificationAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EnvironmentUtil
import com.intellij.util.messages.Topic
import java.io.File

data class MiseState(
    var executablePath: String = "",
    var useMiseDirEnv: Boolean = true,
    var miseProfile: String = "",
) : Cloneable {
    public override fun clone(): MiseState =
        MiseState(
            executablePath = executablePath,
            useMiseDirEnv = useMiseDirEnv,
            miseProfile = miseProfile,
        )
}

@Service(Service.Level.PROJECT)
@State(name = "com.github.l34130.mise.settings.MiseSettings", storages = [Storage("mise.xml")])
class MiseSettings : PersistentStateComponent<MiseState> {
    private var state = MiseState()

    override fun getState() = state

    override fun loadState(state: MiseState) {
        this.state = state.clone()
    }

    override fun noStateLoaded() {
        state = MiseState(
            executablePath = getMiseExecutablePath() ?: "",
        )
    }

    override fun initializeComponent() {
        state = MiseState(
            executablePath = state.executablePath.takeIf { it.isNotEmpty() } ?: getMiseExecutablePath() ?: "",
            useMiseDirEnv = state.useMiseDirEnv,
            miseProfile = state.miseProfile,
        )

        if (state.executablePath.isEmpty()) {
            MiseNotificationService.getInstance().warn(
                title = "Mise Executable Not Found",
                htmlText = """
                    Mise executable not found in PATH.<br/>
                    Please specify the path to the mise executable in the settings.
                """.trimIndent(),
                actionProvider = {
                    NotificationAction.createSimple(
                        "Open settings",
                    ) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(null, MiseConfigurable::class.java)
                    }
                },
            )
        }
    }

    companion object {
        fun getService(project: Project): MiseSettings = project.service<MiseSettings>()

        val MISE_SETTINGS_TOPIC = Topic.create("Mise Settings", SettingsChangeListener::class.java)

        private fun getMiseExecutablePath(): String? {
            val path = EnvironmentUtil.getValue("PATH") ?: return null

            for (dir in StringUtil.tokenize(path, File.pathSeparator)) {
                val file = File(dir, "mise")
                if (file.canExecute()) {
                    return file.absolutePath
                }
            }

            return null
        }
    }

    interface SettingsChangeListener {
        fun settingsChanged(
            oldState: MiseState,
            newState: MiseState,
        )
    }
}
