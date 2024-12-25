package com.github.l34130.mise.core.run

data class MiseRunConfigurationState(
    var useMiseDirEnv: Boolean = true,
    var miseConfigEnvironment: String = "",
) : Cloneable {
    public override fun clone() =
        MiseRunConfigurationState(
            useMiseDirEnv = useMiseDirEnv,
            miseConfigEnvironment = miseConfigEnvironment,
        )
}
