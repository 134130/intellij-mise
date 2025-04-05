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
import com.intellij.util.application

@Service(Service.Level.PROJECT)
@State(name = "com.github.l34130.mise.settings.MiseSettings", storages = [Storage("mise.xml")])
class MiseProjectSettings(
    private val project: Project,
) : PersistentStateComponent<MiseProjectSettings.MyState> {
    private var myState = MyState()

    override fun getState() = myState

    override fun loadState(state: MyState) {
        myState = state.clone()
    }

    override fun noStateLoaded() {
        myState = MyState()
    }

    override fun initializeComponent() {
        myState =
            MyState().also {
                it.useMiseDirEnv = myState.useMiseDirEnv
                it.miseConfigEnvironment = myState.miseConfigEnvironment
            }

        val applicationSettings = application.service<MiseApplicationSettings>()
        if (applicationSettings.state.executablePath.isEmpty()) {
            MiseNotificationService.getInstance(project).warn(
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
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Mise Settings")
                    }
                },
            )
        }
    }

    class MyState : Cloneable {
        var useMiseDirEnv: Boolean = true
        var miseConfigEnvironment: String = ""

        public override fun clone(): MyState =
            MyState().also {
                it.useMiseDirEnv = useMiseDirEnv
                it.miseConfigEnvironment = miseConfigEnvironment
            }
    }
}
