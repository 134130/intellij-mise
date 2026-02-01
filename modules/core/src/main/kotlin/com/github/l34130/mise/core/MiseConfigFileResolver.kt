package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.isFile
import com.intellij.util.containers.addIfNotNull
import com.intellij.util.application
import fleet.multiplatform.shims.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class MiseConfigFileResolver(
    val project: Project,
) : Disposable {
    private val cache = ConcurrentHashMap<String, List<VirtualFile>>()

    init {
        // Ensure the VFS listener service is initialized
        project.service<MiseTomlFileListener>()
        
        // Subscribe to cache invalidation events
        MiseProjectEventListener.subscribe(project, this) { event ->
            if (event.kind == MiseProjectEvent.Kind.TOML_CHANGED) {
                cache.clear()
            }
        }
    }

    suspend fun resolveConfigFiles(
        baseDirVf: VirtualFile,
        refresh: Boolean = false,
        configEnvironment: String? = null,
    ): List<VirtualFile> {
        val cacheKey = "${baseDirVf.path}:${configEnvironment.orEmpty()}"
        if (!refresh && !application.isUnitTestMode) {
            cache[cacheKey]?.let { return it }
        }

        // Parse environments outside readAction for efficiency
        val environments = if (!configEnvironment.isNullOrBlank()) {
            configEnvironment.split(',').map { it.trim() }
        } else {
            emptyList()
        }

        val result =
            smartReadAction(project) {
                buildList {
                    addIfNotNull(baseDirVf.findFileOrDirectory("mise/config.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".mise/config.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".config/mise.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".config/mise/config.toml")?.takeIf { it.isFile })
                    // .config/mise/conf.d/*.toml
                    baseDirVf.findFileOrDirectory(".config/mise/conf.d")?.takeIf { it.isDirectory }?.let { dir ->
                        addAll(dir.children.filter { it.name.endsWith(".toml") && it.isFile })
                    }
                    
                    // Add environment-specific config files if configEnvironment is specified
                    // These are loaded after base configs but before local configs
                    for (env in environments) {
                        addIfNotNull(baseDirVf.findFileOrDirectory(".config/mise.$env.toml")?.takeIf { it.isFile })
                        addIfNotNull(baseDirVf.findFileOrDirectory(".mise.$env.toml")?.takeIf { it.isFile })
                        addIfNotNull(baseDirVf.findFileOrDirectory("mise.$env.toml")?.takeIf { it.isFile })
                    }
                    
                    addIfNotNull(baseDirVf.findFileOrDirectory("mise.local.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory("mise.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".mise.local.toml")?.takeIf { it.isFile })
                    addIfNotNull(baseDirVf.findFileOrDirectory(".mise.toml")?.takeIf { it.isFile })
                }
            }

        if (!application.isUnitTestMode) {
            cache[cacheKey] = result
        }
        return result
    }

    override fun dispose() { }
}
