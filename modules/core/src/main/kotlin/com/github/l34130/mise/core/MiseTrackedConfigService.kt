package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.getUserHomeForProject
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
import kotlin.io.path.exists
import kotlin.io.path.pathString

/**
 * Service that tracks all configuration files that mise is monitoring.
 * This includes:
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
     * Refresh the list of tracked configuration files by querying mise CLI
     * and adding global config files.
     * This is done asynchronously to avoid blocking.
     * 
     * Note: Currently relies on `mise config --tracked-configs` to report env_file references.
     * If mise doesn't include files from MISE_ENV_FILE or [settings].env_file in its output,
     * those files won't be watched for changes. This is a limitation of the mise CLI command.
     */
    private fun refreshTrackedConfigs() {
        cs.launch(Dispatchers.IO) {
            try {
                val settings = project.service<MiseProjectSettings>()
                val configEnvironment = settings.state.miseConfigEnvironment
                
                val allTrackedFiles = mutableSetOf<String>()
                
                // Get project-level tracked configs from mise CLI
                // This should include env_file references if mise reports them
                val result = MiseCommandLineHelper.getTrackedConfigs(project, configEnvironment)
                result.onSuccess { configs ->
                    allTrackedFiles.addAll(configs)
                    logger.debug("Got ${configs.size} tracked configs from mise CLI")
                }.onFailure { error ->
                    logger.debug("Failed to get tracked configs from mise CLI", error)
                }
                
                // Add global mise config files that mise might not report
                // When these files change, we need to refresh because they might contain
                // env_file or MISE_ENV_FILE settings
                val userHome = project.getUserHomeForProject()
                val globalConfigPaths = listOf(
                    "$userHome/.config/mise/config.toml",
                    "$userHome/.config/mise.toml",
                    "$userHome/.mise/config.toml",
                    "$userHome/.mise.toml"
                )
                
                // Only add global config files that actually exist
                globalConfigPaths.forEach { path ->
                    if (Path.of(path).exists()) {
                        allTrackedFiles.add(path)
                        logger.debug("Added global config file: $path")
                    }
                }
                
                // TODO: If mise config --tracked-configs doesn't include env_file references,
                // we could parse the TOML files ourselves to extract env_file paths and add them here.
                // However, this would require:
                // 1. Reading all config files (including global ones)
                // 2. Parsing TOML to find env_file and MISE_ENV_FILE settings
                // 3. Resolving relative paths correctly
                // 4. Handling environment variable expansion
                // This is complex and error-prone, so we currently rely on mise CLI to report these.
                
                // Update the tracked configs set atomically
                synchronized(trackedConfigs) {
                    trackedConfigs.clear()
                    trackedConfigs.addAll(allTrackedFiles)
                }
                
                val miseCount = result.getOrNull()?.size ?: 0
                val globalCount = allTrackedFiles.size - miseCount
                logger.info("Refreshed tracked configs: ${allTrackedFiles.size} files total ($miseCount from mise, $globalCount global)")
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
