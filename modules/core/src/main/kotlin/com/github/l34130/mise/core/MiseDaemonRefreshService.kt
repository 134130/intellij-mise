package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ZipperUpdater
import com.intellij.util.Alarm
import com.intellij.util.application

/**
 * Restarts the IDE daemon analyzer when the task cache refreshes.
 *
 * This exists because task data is refreshed asynchronously and several editor features
 * (reference resolve, inspections, run line markers) depend on that cache. A daemon restart
 * forces a re-highlight so those features reflect the newly refreshed tasks.
 *
 * Threading: [Alarm.ThreadToUse.SWING_THREAD] means the updater posts its queued work to
 * the Swing event queue (the EDT), instead of a pooled/background thread. This matters
 * because [DaemonCodeAnalyzer.restart] must run on the EDT (it triggers UI updates), while
 * the listener can be invoked from arbitrary threads (e.g., IO refresh). The 1000ms debounce
 * coalesces rapid refresh events into a single daemon restart, preventing UI thrashing.
 *
 * Note: This is a light service and is lazy by default, so it must be
 * initialized (e.g., in startup activity) to register its event listener.
 */
@Service(Service.Level.PROJECT)
class MiseDaemonRefreshService(
    private val project: Project,
) : Disposable {
    private val updater = ZipperUpdater(1000, Alarm.ThreadToUse.SWING_THREAD, this)

    init {
        MiseProjectEventListener.subscribe(project, this) { event ->
            if (event.kind == MiseProjectEvent.Kind.TASK_CACHE_REFRESHED) {
                scheduleDaemonRestart()
            }
        }
    }

    private fun scheduleDaemonRestart() {
        if (application.isUnitTestMode || project.isDisposed) return
        updater.queue {
            if (project.isDisposed) return@queue
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    override fun dispose() {}
}
