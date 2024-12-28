package com.github.l34130.mise.core.setting

import com.github.l34130.mise.core.notification.MiseNotificationService
import com.intellij.notification.NotificationAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EnvironmentUtil
import java.io.File

@Service(Service.Level.APP)
@State(name = "com.github.l34130.mise.settings.MiseSettings", storages = [Storage("mise.xml")])
class MiseSettings : PersistentStateComponent<MiseSettings.MyState> {
    private var myState = MyState()

    override fun getState() = myState

    override fun loadState(state: MyState) {
        myState = state.clone()
    }

    override fun noStateLoaded() {
        myState =
            MyState().also {
                it.executablePath = getMiseExecutablePath() ?: ""
            }
    }

    override fun initializeComponent() {
        myState =
            MyState().also {
                it.executablePath = myState.executablePath.takeIf { it.isNotEmpty() } ?: getMiseExecutablePath() ?: ""
                it.useMiseDirEnv = myState.useMiseDirEnv
                it.miseConfigEnvironment = myState.miseConfigEnvironment
            }

        if (myState.executablePath.isEmpty()) {
            MiseNotificationService.getInstance(null).warn(
                title = "Mise Executable Not Found",
                htmlText =
                    """
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

    class MyState : Cloneable {
        var executablePath: String = ""
        var useMiseDirEnv: Boolean = true
        var miseConfigEnvironment: String = ""

        public override fun clone(): MyState =
            MyState().also {
                it.executablePath = executablePath
                it.useMiseDirEnv = useMiseDirEnv
                it.miseConfigEnvironment = miseConfigEnvironment
            }
    }
}
