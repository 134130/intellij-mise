package com.github.l34130.mise.core.run

import com.github.l34130.mise.core.setting.MiseState

data class MiseRunConfigurationState(
    var useMiseDirEnv: Boolean = true,
    var miseProfile: String = "",
) : Cloneable {
    public override fun clone() =
        MiseRunConfigurationState(
            useMiseDirEnv = useMiseDirEnv,
            miseProfile = miseProfile,
        )

    fun mergeProjectState(projectState: MiseState): MiseRunConfigurationState =
        this.copy(
            useMiseDirEnv = projectState.useMiseDirEnv,
            miseProfile = projectState.miseProfile,
        )
}
