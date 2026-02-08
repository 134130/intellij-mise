package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that tracks all configuration files that mise is monitoring.
 * This includes .mise.toml files as well as external files like .env files
 * referenced via env_file directives.
 *
 * The tracked configs list is updated when:
 * - Project starts up
 * - TOML files change (which may add/remove env_file references)
 * - Settings change (which may affect the config environment)
 */
@Service(Service.Level.PROJECT)
class MiseTrackedConfigService(
    private val project: Project,
    private val cs: CoroutineScope
) : Disposable {
    private val logger = logger<MiseTrackedConfigService>()
    
    // Thread-safe set of tracked config file paths
    private val trackedConfigs: MutableSet<String> = ConcurrentHashMap.newKeySet()
    
    init {
        // Listen for events that should trigger re-querying tracked configs
        MiseProjectEventListener.subscribe(project, this) { event ->
            when (event.kind) {
                MiseProjectEvent.Kind.STARTUP,
                MiseProjectEvent.Kind.TOML_CHANGED,
                MiseProjectEvent.Kind.SETTINGS_CHANGED -> {
                    refreshTrackedConfigs()
                }
                else -> Unit
            }
        }
    }
    
    /**
     * Refresh the list of tracked configuration files by querying mise CLI.
     * This is done asynchronously to avoid blocking.
     */
    private fun refreshTrackedConfigs() {
        cs.launch(Dispatchers.IO) {
            try {
                val settings = project.service<MiseProjectSettings>()
                val configEnvironment = settings.state.miseConfigEnvironment
                
                val result = MiseCommandLineHelper.getTrackedConfigs(project, configEnvironment)
                result.onSuccess { configs ->
                    // Update the tracked configs set
                    trackedConfigs.clear()
                    trackedConfigs.addAll(configs)
                    logger.debug("Refreshed tracked configs: ${configs.size} files")
                }.onFailure { error ->
                    logger.debug("Failed to refresh tracked configs", error)
                }
            } catch (e: Exception) {
                logger.debug("Exception while refreshing tracked configs", e)
            }
        }
    }
    
    /**
     * Check if a file path is in the tracked configs list.
     * This is used by the file listener to determine if changes should invalidate the cache.
     */
    fun isTrackedConfig(filePath: String): Boolean {
        return trackedConfigs.contains(filePath)
    }
    
    /**
     * Get all currently tracked configuration files.
     */
    fun getTrackedConfigs(): Set<String> {
        return trackedConfigs.toSet()
    }
    
    override fun dispose() {
        // Cleanup handled by coroutine scope
    }
}
