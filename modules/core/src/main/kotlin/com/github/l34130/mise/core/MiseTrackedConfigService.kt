package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Path
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
     * Queries two mise commands:
     * 1. `mise config --tracked-configs` - Returns TOML config files
     * 2. `mise env --json-extended` - Returns env vars with source files (to find .env files)
     * 
     * Converts all relative paths to absolute paths based on the project directory.
     */
    private fun refreshTrackedConfigs() {
        cs.launch(Dispatchers.IO) {
            try {
                val settings = project.service<MiseProjectSettings>()
                val configEnvironment = settings.state.miseConfigEnvironment
                val projectPath = project.guessMiseProjectPath()
                val allFiles = mutableSetOf<String>()
                
                // Get TOML config files from mise CLI
                val configResult = MiseCommandLineHelper.getTrackedConfigs(project, configEnvironment)
                configResult.onSuccess { configs ->
                    allFiles.addAll(configs)
                    logger.debug("Got ${configs.size} tracked configs from mise config --tracked-configs")
                }.onFailure { error ->
                    logger.debug("Failed to get tracked configs from mise CLI", error)
                }
                
                // Get env vars with sources to find .env files
                val envResult = MiseCommandLineHelper.getEnvVarsExtended(project, projectPath, configEnvironment)
                envResult.onSuccess { envMap ->
                    val envFiles = envMap.values
                        .mapNotNull { it.source }
                        .filter { source ->
                            // Filter to files that look like env files (not config.toml files)
                            !source.endsWith(".toml") && (source.contains(".env") || source.contains("env"))
                        }
                        .toSet()
                    allFiles.addAll(envFiles)
                    logger.debug("Got ${envFiles.size} env files from mise env --json-extended")
                    if (logger.isDebugEnabled && envFiles.isNotEmpty()) {
                        envFiles.forEach { logger.debug("  Env file: $it") }
                    }
                }.onFailure { error ->
                    logger.debug("Failed to get env sources from mise CLI", error)
                }
                
                // Convert all paths to absolute paths for consistent matching with VFS events
                val absolutePaths = allFiles.map { path ->
                    if (Path.of(path).isAbsolute) {
                        path
                    } else {
                        // Relative path - resolve it relative to the project directory
                        Path.of(projectPath).resolve(path).normalize().toString()
                    }
                }.toSet()
                
                // Update the tracked configs set atomically
                synchronized(trackedConfigs) {
                    trackedConfigs.clear()
                    trackedConfigs.addAll(absolutePaths)
                }
                logger.info("Refreshed tracked configs: ${absolutePaths.size} files total")
                if (logger.isDebugEnabled) {
                    absolutePaths.forEach { logger.debug("  Tracking: $it") }
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
