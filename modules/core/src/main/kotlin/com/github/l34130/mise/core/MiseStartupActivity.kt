package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.util.prewarmProjectInfo
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MiseStartupActivity :
    ProjectActivity,
    DumbAware {
    override suspend fun execute(project: Project) {
        // Pre-warm project environment info for early callers.
        var warmed = false
        try {
            withBackgroundProgress(project, "Mise: initializing WSL context") {
                withContext(Dispatchers.IO) {
                    project.prewarmProjectInfo()
                }
            }
            warmed = true
        } finally {
            if (warmed) {
                MiseProjectEventListener.broadcast(
                    project,
                    MiseProjectEvent(MiseProjectEvent.Kind.STARTUP, "startup")
                )
            }
        }
    }
}
