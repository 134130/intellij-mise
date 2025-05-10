package com.github.l34130.mise.core.setting

import com.intellij.execution.Platform
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EnvironmentUtil
import com.intellij.util.SystemProperties
import com.intellij.util.system.OS
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

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
                it.executablePath = getMiseExecutablePath()?.absolutePathString() ?: ""
            }
    }

    override fun initializeComponent() {
        myState =
            MyState().also {
                it.executablePath = myState.executablePath.takeIf { it.isNotEmpty() } ?: getMiseExecutablePath()?.absolutePathString() ?: ""
            }
    }

    companion object {
        private fun getMiseExecutablePath(): Path? {
            // try to find the mise executable in the PATH
            val path = EnvironmentUtil.getValue("PATH")
            if (path != null) {
                for (dir in StringUtil.tokenize(path, File.pathSeparator)) {
                    val file = File(dir, "mise")
                    if (file.canExecute()) {
                        return file.toPath()
                    }
                }
            }

            // try to find the mise executable in usual system-specific directories
            when (OS.CURRENT.platform) {
                Platform.WINDOWS -> {
                    val localAppData: Path? = EnvironmentUtil.getValue("LOCALAPPDATA")?.let { Path.of(it) }
                    if (localAppData != null) {
                        val path = localAppData.resolve("Microsoft/WinGet/Links/mise.exe")
                        if (runCatching { path.toFile().canExecute() }.getOrNull() == true) {
                            return path
                        }
                    }

                    val userHome = Path.of(SystemProperties.getUserHome())
                    val path = userHome.resolve("AppData/Local/Microsoft/WinGet/Links/mise.exe")
                    if (runCatching { path.toFile().canExecute() }.getOrNull() == true) {
                        return path
                    }
                }
                Platform.UNIX -> {
                    // do nothing
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
