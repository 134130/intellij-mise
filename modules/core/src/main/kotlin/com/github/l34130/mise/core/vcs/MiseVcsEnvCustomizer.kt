package com.github.l34130.mise.core.vcs

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.setting.MiseConfigurable
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsEnvCustomizer
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class MiseVcsEnvCustomizer : VcsEnvCustomizer() {
    private val logger = Logger.getInstance(MiseVcsEnvCustomizer::class.java)

    override fun customizeCommandAndEnvironment(
        project: Project?,
        envs: MutableMap<String?, String?>,
        context: VcsExecutableContext,
    ) {
        if (project == null) return

        val miseProjectSettings = project.service<MiseProjectSettings>().state
        if (!miseProjectSettings.useMiseDirEnv) return

        try {
            // TODO: Cache the env vars to avoid running the command every time
            val miseEnvsResult =
                runBlocking {
                    withBackgroundProgress(project, "Mise: Getting EnvVars") {
                        MiseCommandLineHelper.getEnvVars(project.basePath, miseProjectSettings.miseConfigEnvironment)
                    }
                }

            miseEnvsResult.fold(
                onSuccess = { envs.putAll(it) },
                onFailure = {
                    logger.warn("Failed to get Mise env vars", it)
                    MiseNotificationService.getInstance(project).warn(
                        title = "Failed to get Mise env vars",
                        htmlText =
                            """
                            Failed to get Mise env vars for VCS operations.<br/>
                            ${it.message}<br/>
                            Please check your Mise configuration.<br/>
                            """.trimIndent(),
                    )
                },
            )
        } catch (e: CancellationException) {
            if (e is ProcessCanceledException) {
                throw e
            }
            throw ProcessCanceledException(e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ProcessCanceledException(e)
        }
    }

    override fun getConfigurable(project: Project?): UnnamedConfigurable? {
        if (project == null) return null
        return MiseConfigurable(project)
    }
}
