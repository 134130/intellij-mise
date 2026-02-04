package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.MiseTomlFileListener
import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.util.waitForProjectCache
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.ZipperUpdater
import com.intellij.util.Alarm

class MiseProjectSdkSetupActivity : ProjectActivity, DumbAware, Disposable {
    override suspend fun execute(project: Project) {
        project.service<MiseTomlFileListener>()

        // Debounce bursts of project events into a single SDK recheck.
        val updater = ZipperUpdater(400, Alarm.ThreadToUse.POOLED_THREAD, this)
        val recheckTask = Runnable {
            if (!project.isDisposed) {
                if (!project.waitForProjectCache()) {
                    return@Runnable
                }
                try {
                    AbstractProjectSdkSetup.runAll(project, isUserInteraction = false)
                } catch (e: Throwable) {
                    logger.warn("Failed to re-check SDK configuration", e)
                }
            }
        }

        MiseProjectEventListener.subscribe(project, this) { event ->
            when (event.kind) {
                MiseProjectEvent.Kind.TOML_CHANGED,
                MiseProjectEvent.Kind.SETTINGS_CHANGED,
                MiseProjectEvent.Kind.EXECUTABLE_CHANGED -> updater.queue(recheckTask)
                else -> Unit
            }
        }

        recheckTask.run()
    }

    companion object {
        private val logger = logger<MiseProjectSdkSetupActivity>()
    }

    override fun dispose() {
    }
}
