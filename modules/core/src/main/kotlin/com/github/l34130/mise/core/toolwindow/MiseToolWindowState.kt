package com.github.l34130.mise.core.toolwindow

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(name = "com.github.l34130.mise.toolwindow.MiseToolWindowState", storages = [Storage("mise.xml")])
class MiseToolWindowState : PersistentStateComponent<MiseToolWindowState.MyState> {
    private var myState = MyState()

    override fun getState(): MyState = myState

    override fun loadState(state: MyState) {
        myState = state.clone()
    }

    class MyState : Cloneable {
        var groupByConfigPath: Boolean = false
        var nonProjectPathDisplay: NonProjectPathDisplay = NonProjectPathDisplay.RELATIVE
        var envOverride: String = ""
        var envInitialized: Boolean = false

        public override fun clone(): MyState =
            MyState().also {
                it.groupByConfigPath = groupByConfigPath
                it.nonProjectPathDisplay = nonProjectPathDisplay
                it.envOverride = envOverride
                it.envInitialized = envInitialized
            }
    }
}

enum class NonProjectPathDisplay {
    RELATIVE,
    ABSOLUTE,
}
