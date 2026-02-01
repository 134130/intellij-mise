package com.github.l34130.mise.nx.run

import com.github.l34130.mise.core.command.MiseCommandLineEnvCustomizer
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project

private val NX_EXECUTABLES = listOf("nx", "nx.cmd")

/**
 * NX-specific command line customizer that customizes mise environment variables
 * only for NX executable commands (nx, nx.cmd).
 *
 * Extends the base MiseCommandLineEnvCustomizer and filters to only apply to NX executables.
 */
class MiseNxCommandLineEnvCustomizer : MiseCommandLineEnvCustomizer() {
    override fun shouldCustomizeForProject(project: Project, commandLine: GeneralCommandLine): Boolean = MiseCommandLineHelper.matchesExecutableNames(commandLine, NX_EXECUTABLES)

    override fun shouldCustomizeForSettings(settings: MiseProjectSettings.MyState): Boolean {
        return settings.useMiseDirEnv && settings.useMiseInNxCommands
    }
}
