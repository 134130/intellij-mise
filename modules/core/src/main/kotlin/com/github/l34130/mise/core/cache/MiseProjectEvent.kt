package com.github.l34130.mise.core.cache

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

/**
 * Project lifecycle events for project-scoped changes.
 *
 * STARTUP is emitted only after ProjectInfo has been warmed and MUST NOT
 * invalidate caches. It signals that project startup initialization completed.
 */
data class MiseProjectEvent(
    val kind: Kind,
    val reason: String? = null,
) {
    enum class Kind {
        STARTUP,
        SETTINGS_CHANGED,
        EXECUTABLE_CHANGED,
        TOML_CHANGED,
        TASK_CACHE_REFRESHED,
    }
}

fun interface MiseProjectEventListener {
    fun onProjectEvent(event: MiseProjectEvent)

    companion object {
        @JvmField
        @Topic.ProjectLevel
        val TOPIC =
            Topic(
                "Mise Project Event",
                MiseProjectEventListener::class.java,
                Topic.BroadcastDirection.NONE,
            )

        fun subscribe(
            project: Project,
            parentDisposable: Disposable,
            listener: MiseProjectEventListener,
        ) {
            project.messageBus.connect(parentDisposable).subscribe(TOPIC, listener)
        }

        fun subscribe(
            project: Project,
            parentDisposable: Disposable,
            handler: (MiseProjectEvent) -> Unit,
        ) {
            subscribe(project, parentDisposable, MiseProjectEventListener { event -> handler(event) })
        }

        fun broadcast(
            project: Project,
            event: MiseProjectEvent,
        ) {
            if (!project.isDisposed) {
                logger<MiseProjectEvent>().trace { "Broadcast: $event" }
                project.messageBus.syncPublisher(TOPIC).onProjectEvent(event)
            }
        }
    }
}
