package com.github.l34130.mise.core.setting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo

@Service(Service.Level.APP)
@State(name = "com.github.l34130.mise.settings.MiseApplicationSettings", storages = [Storage("mise.xml")])
class MiseApplicationSettings : PersistentStateComponent<MiseApplicationSettings.MyState> {
    private var myState = MyState()

    override fun getState(): MyState = myState

    override fun loadState(state: MyState) {
        myState = state.clone()

        // Migration: Clear old WSL auto-discovered paths if they exist
        if (SystemInfo.isWindows && myState.executablePath.contains("wsl.exe")) {
            logger.info("Migrating old WSL executable setting. Clearing to use PATH default.")
            myState.executablePath = ""
        }
    }

    override fun noStateLoaded() {
        myState = MyState()  // Just use empty defaults
    }

    companion object {
        private val logger = logger<MiseApplicationSettings>()
    }

    class MyState : Cloneable {
        var executablePath: String = ""

        public override fun clone(): MyState =
            MyState().also {
                it.executablePath = executablePath
            }
    }
}
