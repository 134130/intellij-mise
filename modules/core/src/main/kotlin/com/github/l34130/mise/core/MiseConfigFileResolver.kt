package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.stringArray
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.childrenOfType
import com.intellij.util.EnvironmentUtil
import com.intellij.util.application
import com.intellij.util.containers.addIfNotNull
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlFileType
import org.toml.lang.psi.TomlTable
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class MiseConfigFileResolver(
    val project: Project,
) : Disposable {
    data class TrackedConfigSnapshot(
        val configTomlFiles: List<VirtualFile>,
        val externalTrackedFiles: List<VirtualFile>,
        val trackedInputs: List<VirtualFile>,
        val normalizedTrackedPaths: Set<String>,
    )

    private val snapshotByContext = ConcurrentHashMap<String, TrackedConfigSnapshot>()
    private val globalTrackedPathIndex = ConcurrentHashMap.newKeySet<String>()
    private val logger = logger<MiseConfigFileResolver>()

    init {
        // Ensure the VFS listener service is initialized
        project.service<MiseTomlFileListener>()

        // Subscribe to cache invalidation events
        MiseProjectEventListener.subscribe(project, this) { event ->
            if (event.kind == MiseProjectEvent.Kind.TOML_CHANGED || event.kind == MiseProjectEvent.Kind.SETTINGS_CHANGED) {
                snapshotByContext.clear()
                globalTrackedPathIndex.clear()
            }
        }
    }

    fun isTrackedPath(file: VirtualFile): Boolean = globalTrackedPathIndex.contains(normalizePath(file.path))

    suspend fun resolveConfigFiles(
        baseDirVf: VirtualFile,
        refresh: Boolean = false,
        configEnvironment: String? = null,
    ): List<VirtualFile> = resolveTrackedFiles(baseDirVf, refresh, configEnvironment).configTomlFiles

    suspend fun resolveTrackedFiles(
        baseDirVf: VirtualFile,
        refresh: Boolean = false,
        configEnvironment: String? = null,
    ): TrackedConfigSnapshot {
        val cacheKey = "${baseDirVf.path}:${configEnvironment.orEmpty()}"
        if (!refresh && !application.isUnitTestMode) {
            snapshotByContext[cacheKey]?.let { return it }
        }

        val trackedTomlFiles =
            resolveTrackedTomlFiles(baseDirVf, configEnvironment).ifEmpty {
                resolveFallbackTomlFiles(baseDirVf, configEnvironment)
            }

        val configRoot = computeConfigRoot(trackedTomlFiles.firstOrNull(), baseDirVf)
        val externalTrackedFiles = resolveExternalTrackedFiles(trackedTomlFiles, configRoot, baseDirVf)
        val allTrackedFiles = (trackedTomlFiles + externalTrackedFiles).distinctBy { normalizePath(it.path) }
        val snapshot =
            TrackedConfigSnapshot(
                configTomlFiles = trackedTomlFiles,
                externalTrackedFiles = externalTrackedFiles,
                trackedInputs = allTrackedFiles,
                normalizedTrackedPaths = allTrackedFiles.mapTo(linkedSetOf()) { normalizePath(it.path) },
            )

        if (!application.isUnitTestMode) {
            snapshotByContext[cacheKey] = snapshot
            refreshTrackedPathLookup()
            logger.info(
                @Suppress("ktlint:standard:max-line-length")
                "Resolved ${snapshot.configTomlFiles.size} config TOML files and ${snapshot.externalTrackedFiles.size} external files for environment '$configEnvironment': ${snapshot.trackedInputs.joinToString { it.path }}",
            )
        }
        return snapshot
    }

    override fun dispose() { }

    private suspend fun resolveTrackedTomlFiles(
        baseDirVf: VirtualFile,
        configEnvironment: String?,
    ): List<VirtualFile> {
        val tracked =
            MiseCommandLineHelper.getTrackedConfigs(project, configEnvironment.orEmpty())
                .getOrElse { return emptyList() }
        val fs = LocalFileSystem.getInstance()
        return tracked
            .asSequence()
            .filter { it.endsWith(".toml", ignoreCase = true) }
            .mapNotNull { fs.refreshAndFindFileByPath(it) }
            .filter { it.isFile }
            .distinctBy { normalizePath(it.path) }
            .toList()
    }

    private suspend fun resolveFallbackTomlFiles(
        baseDirVf: VirtualFile,
        configEnvironment: String?,
    ): List<VirtualFile> {
        // Parse environments outside readAction for efficiency
        val environments =
            if (!configEnvironment.isNullOrBlank()) {
                configEnvironment.split(',').map { it.trim() }
            } else {
                emptyList()
            }

        return smartReadAction(project) {
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
    }

    private suspend fun resolveExternalTrackedFiles(
        trackedTomlFiles: List<VirtualFile>,
        configRoot: VirtualFile,
        baseDirVf: VirtualFile,
    ): List<VirtualFile> {
        val filesFromToml =
            trackedTomlFiles.flatMap { toml ->
                extractExternalPathsFromToml(toml)
                    .mapNotNull { raw -> resolveExternalPath(raw, configRoot, toml, baseDirVf) }
            }

        val envFilePath =
            EnvironmentUtil.getValue("MISE_ENV_FILE")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val mergedFiles =
            buildList {
                addAll(filesFromToml)
                if (envFilePath != null) {
                    resolveExternalPath(envFilePath, configRoot, trackedTomlFiles.firstOrNull(), baseDirVf)?.let { add(it) }
                }
            }

        return mergedFiles
            .asSequence()
            .filter { it.isFile }
            .distinctBy { normalizePath(it.path) }
            .toList()
    }

    private suspend fun extractExternalPathsFromToml(tomlFile: VirtualFile): List<String> {
        val content =
            runCatching {
                String(tomlFile.contentsToByteArray(), Charsets.UTF_8)
            }.getOrElse {
                logger.debug("Failed to read TOML file for external reference extraction: ${tomlFile.path}", it)
                return emptyList()
            }

        val parsedToml = readAction {
            PsiFileFactory.getInstance(project)
                .createFileFromText("tracked-mise.toml", TomlFileType, content) as? TomlFile
        } ?: return emptyList()

        val result = mutableListOf<String>()
        readAction {
            result += parsedToml.getValueWithKey("env_file")?.stringArray ?: emptyList()
            for (table in parsedToml.childrenOfType<TomlTable>()) {
                val segments = table.header.key?.segments?.map { it.name } ?: continue
                if (segments == listOf("env", "_")) {
                    result += (table.getValueWithKey("file")?.stringArray ?: emptyList())
                    result += (table.getValueWithKey("source")?.stringArray ?: emptyList())
                }
                if (segments == listOf("vars", "_")) {
                    result += (table.getValueWithKey("file")?.stringArray ?: emptyList())
                    result += (table.getValueWithKey("source")?.stringArray ?: emptyList())
                }
            }
        }
        return result
    }

    private fun resolveExternalPath(
        rawPath: String,
        configRoot: VirtualFile,
        sourceTomlFile: VirtualFile?,
        baseDirVf: VirtualFile,
    ): VirtualFile? {
        val trimmed = rawPath.trim()
        if (trimmed.isBlank()) return null

        val fs = LocalFileSystem.getInstance()
        val normalizedPath = normalizePath(trimmed)
        val absolute =
            runCatching { Paths.get(normalizedPath).isAbsolute }
                .getOrDefault(false)

        if (absolute) {
            return fs.refreshAndFindFileByPath(normalizedPath)
        }

        return configRoot.findFileOrDirectory(trimmed)
            ?: sourceTomlFile?.parent?.findFileOrDirectory(trimmed)
            ?: baseDirVf.findFileOrDirectory(trimmed)
    }

    private fun computeConfigRoot(
        firstTrackedToml: VirtualFile?,
        baseDirVf: VirtualFile,
    ): VirtualFile {
        val file = firstTrackedToml ?: return baseDirVf
        if (file.name == "config.toml" && file.parent?.name in setOf("mise", ".mise")) {
            return file.parent?.parent ?: file.parent
        }
        if (file.name == "mise.toml" && file.parent?.name == ".config") {
            return file.parent?.parent ?: file.parent
        }
        if (file.name == "config.toml" && file.parent?.name == "mise" && file.parent?.parent?.name == ".config") {
            return file.parent?.parent?.parent ?: file.parent
        }
        if (file.name.endsWith(".toml", ignoreCase = true) &&
            file.parent?.name == "conf.d" &&
            file.parent?.parent?.name == "mise" &&
            file.parent?.parent?.parent?.name == ".config"
        ) {
            return file.parent?.parent?.parent?.parent ?: file.parent
        }
        return file.parent ?: baseDirVf
    }

    private fun refreshTrackedPathLookup() {
        globalTrackedPathIndex.clear()
        for (snapshot in snapshotByContext.values) {
            globalTrackedPathIndex.addAll(snapshot.normalizedTrackedPaths)
        }
    }

    private fun normalizePath(path: String): String {
        val normalized =
            runCatching {
                val nio = Paths.get(path).normalize()
                nio.toString()
            }.getOrDefault(path)
        return normalized.replace('\\', '/')
    }
}
