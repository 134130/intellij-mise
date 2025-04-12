package com.github.l34130.mise.core.setting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EnvironmentUtil
import java.io.File

@Service(Service.Level.APP)
@State(name = "com.github.l34130.mise.settings.MiseApplicationSettings", storages = [Storage("mise.xml")])
class MiseApplicationSettings : PersistentStateComponent<MiseApplicationSettings.MyState> {
    private var myState = MyState()

    override fun getState(): MyState = myState

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

        public override fun clone(): MyState =
            MyState().also {
                it.executablePath = executablePath
            }
    }
}
