package com.github.l34130.mise.core.run

import com.github.l34130.mise.core.setting.MiseSettings

data class MiseRunConfigurationState(
    var useMiseDirEnv: Boolean = true,
    var miseConfigEnvironment: String = "",
) : Cloneable {
    public override fun clone() =
        MiseRunConfigurationState(
            useMiseDirEnv = useMiseDirEnv,
            miseConfigEnvironment = miseConfigEnvironment,
        )

    fun mergeProjectState(projectState: MiseSettings.MyState): MiseRunConfigurationState =
        this.copy(
            useMiseDirEnv = projectState.useMiseDirEnv,
            miseConfigEnvironment = projectState.miseConfigEnvironment,
        )
}
