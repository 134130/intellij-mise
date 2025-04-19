package com.github.l34130.mise.clion.run

import com.intellij.execution.configurations.RunnerSettings
import com.jetbrains.cidr.execution.CidrRunConfigurationExtensionBase
import com.jetbrains.cidr.lang.workspace.OCRunConfiguration
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import org.jetbrains.intellij.build.impl.Git

class MiseCidrRunConfigurationExtension : CidrRunConfigurationExtensionBase() {
    override fun isEnabledFor(
        runConfiguration: OCRunConfiguration<*, *>,
        toolEnvironment: CidrToolEnvironment,
        runnerSettings: RunnerSettings?
    ): Boolean {
        Git
        return true
    }

    override fun isApplicableFor(runConfiguration: OCRunConfiguration<*, *>): Boolean = true
}