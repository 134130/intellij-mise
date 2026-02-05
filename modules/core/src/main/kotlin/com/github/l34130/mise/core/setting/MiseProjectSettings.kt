package com.github.l34130.mise.core.setting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(name = "com.github.l34130.mise.settings.MiseProjectSettings", storages = [Storage("mise.xml")])
class MiseProjectSettings() : PersistentStateComponent<MiseProjectSettings.MyState> {
    private var myState = MyState()

    override fun getState() = myState

    override fun loadState(state: MyState) {
        myState = state.clone()

        // Migration: Move projectExecutableOverridePath to executablePath if override was enabled
        if (myState.projectExecutableOverrideEnabled && myState.projectExecutableOverridePath.isNotBlank()) {
            myState.executablePath = myState.projectExecutableOverridePath
            myState.projectExecutableOverrideEnabled = false
            myState.projectExecutableOverridePath = ""
        }
    }

    override fun noStateLoaded() {
        myState = MyState()
    }

    override fun initializeComponent() {
        myState =
            MyState().also {
                it.useMiseDirEnv = myState.useMiseDirEnv
                it.miseConfigEnvironment = myState.miseConfigEnvironment
                it.useMiseVcsIntegration = myState.useMiseVcsIntegration
                it.executablePath = myState.executablePath
                it.useMiseInRunConfigurations = myState.useMiseInRunConfigurations
                it.useMiseInDatabaseAuthentication = myState.useMiseInDatabaseAuthentication
                it.useMiseInNxCommands = myState.useMiseInNxCommands
                it.useMiseInAllCommandLines = myState.useMiseInAllCommandLines
                it.sdkSetupOptions = myState.sdkSetupOptions.map { option -> option.copy() }.toMutableList()
            }
    }

    fun getSdkSetupOption(id: String): SdkSetupOption? {
        return myState.sdkSetupOptions.firstOrNull { it.id == id }
    }

    fun effectiveSdkSetupOption(
        id: String,
        defaultAutoInstall: Boolean,
        defaultAutoConfigure: Boolean,
    ): EffectiveSdkSetupOption {
        // Resolve stored overrides against per-provider defaults.
        val option = getSdkSetupOption(id)
        return EffectiveSdkSetupOption(
            autoInstall = option?.autoInstall ?: defaultAutoInstall,
            autoConfigure = option?.autoConfigure ?: defaultAutoConfigure,
        )
    }

    fun upsertSdkSetupOption(
        id: String,
        autoInstall: Boolean,
        autoConfigure: Boolean,
    ) {
        val existing = getSdkSetupOption(id)
        if (existing != null) {
            existing.autoInstall = autoInstall
            existing.autoConfigure = autoConfigure
        } else {
            myState.sdkSetupOptions.add(
                SdkSetupOption(
                    id = id,
                    autoInstall = autoInstall,
                    autoConfigure = autoConfigure,
                ),
            )
        }
    }

    class MyState : Cloneable {
        var useMiseDirEnv: Boolean = true
        var miseConfigEnvironment: String = ""
        var useMiseVcsIntegration: Boolean = true
        var executablePath: String = ""

        // Granular injection controls
        var useMiseInRunConfigurations: Boolean = true
        var useMiseInDatabaseAuthentication: Boolean = true
        var useMiseInNxCommands: Boolean = true
        var useMiseInAllCommandLines: Boolean = false  // Conservative default
        var sdkSetupOptions: MutableList<SdkSetupOption> = mutableListOf()

        // Deprecated fields - kept for backward compatibility during deserialization
        @Deprecated("Use executablePath instead")
        var projectExecutableOverrideEnabled: Boolean = false

        @Deprecated("Use executablePath instead")
        var projectExecutableOverridePath: String = ""

        public override fun clone(): MyState =
            MyState().also {
                it.useMiseDirEnv = useMiseDirEnv
                it.miseConfigEnvironment = miseConfigEnvironment
                it.useMiseVcsIntegration = useMiseVcsIntegration
                it.executablePath = executablePath
                it.useMiseInRunConfigurations = useMiseInRunConfigurations
                it.useMiseInDatabaseAuthentication = useMiseInDatabaseAuthentication
                it.useMiseInNxCommands = useMiseInNxCommands
                it.useMiseInAllCommandLines = useMiseInAllCommandLines
                it.sdkSetupOptions = sdkSetupOptions.map { option -> option.copy() }.toMutableList()
            }
    }

    data class SdkSetupOption(
        var id: String = "",
        var autoInstall: Boolean = false,
        var autoConfigure: Boolean = true,
    )

    data class EffectiveSdkSetupOption(
        val autoInstall: Boolean,
        val autoConfigure: Boolean,
    )
}
