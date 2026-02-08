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
 * 
 * Relies on `mise config --tracked-configs` to report all files that mise is watching,
 * including:
 * - .mise.toml files and other project-level configs
 * - External files like .env files referenced via env_file directives
 * - Global config files in ~/.config/mise/
 * - Files referenced via MISE_ENV_FILE environment variable
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
     * 
     * Relies entirely on `mise config --tracked-configs` to report all files
     * that mise is watching, including global configs and env_file references.
     */
    private fun refreshTrackedConfigs() {
        cs.launch(Dispatchers.IO) {
            try {
                val settings = project.service<MiseProjectSettings>()
                val configEnvironment = settings.state.miseConfigEnvironment
                
                // Get all tracked configs from mise CLI
                // mise should report all files it's tracking, including:
                // - Project-level configs
                // - Global configs
                // - env_file references (both local and from MISE_ENV_FILE)
                val result = MiseCommandLineHelper.getTrackedConfigs(project, configEnvironment)
                result.onSuccess { configs ->
                    // Update the tracked configs set atomically
                    synchronized(trackedConfigs) {
                        trackedConfigs.clear()
                        trackedConfigs.addAll(configs)
                    }
                    logger.info("Refreshed tracked configs from mise CLI: ${configs.size} files")
                    if (logger.isDebugEnabled) {
                        configs.forEach { logger.debug("  Tracking: $it") }
                    }
                }.onFailure { error ->
                    logger.debug("Failed to get tracked configs from mise CLI", error)
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
