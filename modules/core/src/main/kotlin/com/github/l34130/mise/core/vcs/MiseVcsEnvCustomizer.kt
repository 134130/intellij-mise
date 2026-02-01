package com.github.l34130.mise.core.vcs

import com.github.l34130.mise.core.MiseEnvCustomizer
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.setting.MiseConfigurable
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.util.waitForProjectCache
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsEnvCustomizer

/**
 * VCS environment customizer that customizes mise environment variables for VCS commands (git, hg, svn, etc.).
 *
 * Follows the same pattern as MiseCommandLineEnvCustomizer but adapted for the VCS API.
 * Controlled by the "Use in VCS integration" setting.
 */
class MiseVcsEnvCustomizer : VcsEnvCustomizer(), MiseEnvCustomizer {
    override val logger = Logger.getInstance(MiseVcsEnvCustomizer::class.java)

    override fun customizeCommandAndEnvironment(
        project: Project?,
        envs: MutableMap<String?, String?>,
        context: VcsExecutableContext,
    ) {
        if (project == null) return
        // Immediately exit if this isn't required. This must always be the first check to avoid recursion
        if (!MiseCommandLineHelper.environmentNeedsCustomization(envs) || !project.waitForProjectCache()) return


        val workDir = context.root?.path ?: project.guessMiseProjectPath()

        // Perform checks against settings using overridden shouldCustomizeForSettings and if good do the actual customization
        customizeMiseEnvironment(project, workDir, envs)
    }

    /**
     * Hook for settings validation (matches CommandLineEnvCustomizer pattern).
     * Can be overridden by subclasses if needed in future.
     */
    override fun shouldCustomizeForSettings(settings: MiseProjectSettings.MyState): Boolean {
        return settings.useMiseDirEnv && settings.useMiseVcsIntegration
    }

    override fun getConfigurable(project: Project?): UnnamedConfigurable? {
        if (project == null) return null
        return MiseConfigurable(project)
    }
}
