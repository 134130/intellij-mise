package com.github.l34130.mise.core.run

data class MiseRunConfigurationState(
    var configEnvironmentStrategy: ConfigEnvironmentStrategy = ConfigEnvironmentStrategy.USE_PROJECT_SETTINGS,
    var useMiseDirEnv: Boolean = true,
    var miseConfigEnvironment: String = "",
) : Cloneable {
    public override fun clone() =
        MiseRunConfigurationState(
            configEnvironmentStrategy = configEnvironmentStrategy,
            useMiseDirEnv = useMiseDirEnv,
            miseConfigEnvironment = miseConfigEnvironment,
        )
}
