package com.github.l34130.mise.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

/**
 * Project-level service that manages the VFS listener for Mise TOML files.
 * This ensures that the listener is only registered once per project,
 * avoiding duplicate listener registrations.
 */
@Service(Service.Level.PROJECT)
class MiseTomlFileListener(
    private val project: Project,
) : Disposable {
    init {
        // Register the VFS listener once for the entire project
        val connection = project.messageBus.connect(this)
        MiseTomlFileVfsListener.startListening(project, this, connection)
    }

    override fun dispose() {
        // Cleanup is handled automatically by the connection disposal
    }

    companion object {
        /**
         * Topic for broadcasting MISE TOML file changes.
         * Services can subscribe to this topic to be notified of changes.
         */
        val MISE_TOML_CHANGED = Topic.create("MISE_TOML_CHANGED", Function0::class.java)
    }
}
