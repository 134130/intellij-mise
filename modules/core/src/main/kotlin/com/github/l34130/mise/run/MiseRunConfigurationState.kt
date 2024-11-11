package com.github.l34130.mise.run

data class MiseRunConfigurationState(
    var useMiseDirEnv: Boolean = true,
    var miseProfile: String = ".",
) : Cloneable {
    public override fun clone() =
        MiseRunConfigurationState(
            useMiseDirEnv = useMiseDirEnv,
            miseProfile = miseProfile,
        )
}
