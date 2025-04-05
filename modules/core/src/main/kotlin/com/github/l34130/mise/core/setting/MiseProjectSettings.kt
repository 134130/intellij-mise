package com.github.l34130.mise.core.setting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(name = "com.github.l34130.mise.settings.MiseSettings", storages = [Storage("mise.xml")])
class MiseProjectSettings : PersistentStateComponent<MiseProjectSettings.MyState> {
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
