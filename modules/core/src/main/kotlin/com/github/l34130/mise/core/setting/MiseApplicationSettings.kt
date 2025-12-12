package com.github.l34130.mise.core.setting

import com.github.l34130.mise.core.wsl.WslPathUtils
import com.intellij.execution.Platform
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.SystemInfo
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
        // Recalculate WSL settings in case the environment changed
        updateWslSettings(myState)
    }

    override fun noStateLoaded() {
        myState =
            MyState().also {
                it.executablePath = getMiseExecutablePath() ?: ""
                updateWslSettings(it)
            }
    }

    override fun initializeComponent() {
        // Fill in executable path if empty, but don't replace the loaded state
        if (myState.executablePath.isEmpty()) {
            myState.executablePath = getMiseExecutablePath() ?: ""
            updateWslSettings(myState)
        }
    }

    private fun updateWslSettings(state: MyState) {
        if (!SystemInfo.isWindows) {
            state.isWslMode = false
            state.wslDistribution = null
            return
        }
        state.isWslMode = WslPathUtils.detectWslMode(state.executablePath)
        state.wslDistribution =
            if (state.isWslMode) {
                WslPathUtils.extractDistribution(state.executablePath)
            } else {
                null
            }
    }

    companion object {
        private fun getMiseExecutablePath(): String? {
            // try to find the mise executable in the PATH
            val path = EnvironmentUtil.getValue("PATH")
            if (path != null) {
                for (dir in StringUtil.tokenize(path, File.pathSeparator)) {
                    val file = File(dir, "mise")
                    if (file.canExecute()) {
                        return file.toPath().absolutePathString()
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
                            return path.absolutePathString()
                        }
                    }

                    val userHome = Path.of(SystemProperties.getUserHome())
                    val path = userHome.resolve("AppData/Local/Microsoft/WinGet/Links/mise.exe")
                    if (runCatching { path.toFile().canExecute() }.getOrNull() == true) {
                        return path.absolutePathString()
                    }

                    // try to discover mise in WSL distributions
                    val wslInstallations = WslPathUtils.discoverWslMise()
                    if (wslInstallations.isNotEmpty()) {
                        val first = wslInstallations.first()
                        // Return as compound command: wsl.exe -d <distro> <path>
                        return "wsl.exe -d ${first.distribution} ${first.path}"
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
        var isWslMode: Boolean = false
        var wslDistribution: String? = null

        public override fun clone(): MyState =
            MyState().also {
                it.executablePath = executablePath
                it.isWslMode = isWslMode
                it.wslDistribution = wslDistribution
            }
    }
}
