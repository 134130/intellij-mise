package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.model.MiseShellScriptTask
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.model.MiseTomlFile
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.github.l34130.mise.core.model.hasDifferentUiContentFrom
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.baseDirectory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.util.childrenOfType
import com.intellij.util.application
import fleet.multiplatform.shims.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.isExecutable

/**
 * Threading model:
 * - Uses lock-free data structures (ConcurrentHashMap, AtomicBoolean, AtomicReference) to allow
 *   EDT reads without blocking, critical for IDE responsiveness during reference resolution,
 *   completion, and line markers.
 * - cache/stale: ConcurrentHashMap + AtomicBoolean for lock-free reads (safe on EDT)
 * - refreshInFlight/refreshPending: AtomicBoolean for lock-free coalescing of refresh requests
 * - pendingEnvironment: AtomicReference to track last-requested env during coalescing
 * - Refreshes run on Dispatchers.IO to avoid EDT blocking during file I/O and PSI operations
 * - Writes to cache are lock-free; readers may briefly see stale data, but this is acceptable
 *   as the cache will be eventually consistent and TASK_CACHE_REFRESHED events notify listeners
 */
@Service(Service.Level.PROJECT)
class MiseTaskResolver(
    val project: Project,
    private val cs: CoroutineScope,
) : Disposable {
    private val cache = ConcurrentHashMap<String, List<MiseTask>>()
    private val stale = ConcurrentHashMap<String, AtomicBoolean>()
    private val refreshInFlight = AtomicBoolean(false)
    private val refreshPending = AtomicBoolean(false)
    private val pendingEnvironment = AtomicReference<String?>(null)

    private fun staleFlag(env: String): AtomicBoolean = stale.computeIfAbsent(env) { AtomicBoolean(false) }

    init {
        // Ensure the VFS listener service is initialized
        project.service<MiseTomlFileListener>()

        // Subscribe to cache refresh events
        MiseProjectEventListener.subscribe(project, this) { event ->
            when (event.kind) {
                MiseProjectEvent.Kind.STARTUP -> {
                    queueTaskRefresh()
                }

                MiseProjectEvent.Kind.TOML_CHANGED -> {
                    markCacheAsStaleAndTriggerRefresh(null)
                }

                else -> {}
            }
        }
    }

    /**
     * Marks the cache as stale and triggers a refresh. The staleness marker ensures that if there is a failure in the
     * refresh for any reason the refresh is triggered again at the next call to [getCachedTasksOrEmptyList]
     *
     * Omitting or passing a null `configEnvironment` string refreshes the currently active environment, other
     * environments are only marked as stale, not refreshed.
     */
    fun markCacheAsStaleAndTriggerRefresh(configEnvironment: String? = null) {
        markCacheAsStale(configEnvironment)
        queueTaskRefresh(configEnvironment)
    }

    /**
     * Mark the internal task cache as stale.
     *
     * Primarily used by tests. In production code use [markCacheAsStaleAndTriggerRefresh].
     * Called when mise configuration files change to mark the cache as stale. When the cache is marked as
     * stale the next run of [getCachedTasksOrEmptyList] triggers [queueTaskRefresh].
     *
     * Omitting or passing a null `configEnvironment` string marks all previous & current cached environments as stale.
     */
    @JvmOverloads
    fun markCacheAsStale(configEnvironment: String? = null) {
        val envs =
            if (configEnvironment != null) {
                listOf(configEnvironment)
            } else {
                val currentEnv = project.service<MiseProjectSettings>().state.miseConfigEnvironment
                (cache.keys + currentEnv).toSet()
            }
        envs.forEach { env -> staleFlag(env).set(true) }
    }

    /**
     * Return cached tasks immediately (safe on EDT).
     *
     * This may return stale or empty data when the cache is cold. In that case,
     * a background refresh is scheduled and the next call will see fresh data.
     * Callers that need to react to refreshed tasks should subscribe to
     * [MiseProjectEvent.Kind.TASK_CACHE_REFRESHED] and re-read this cache on event.
     * Editor highlighting updates are handled centrally by [MiseDaemonRefreshService].
     */
    fun getCachedTasksOrEmptyList(configEnvironment: String? = null): List<MiseTask> {
        val env = configEnvironment ?: project.service<MiseProjectSettings>().state.miseConfigEnvironment
        val tasks = cache[env] ?: emptyList()
        val isCacheMiss = cache[env] == null
        val isStale = staleFlag(env).get()

        if (isCacheMiss || isStale) {
            logger.trace { "Cache ${if (isCacheMiss) "miss" else "stale"} for env '$env', scheduling refresh" }
            queueTaskRefresh(env)
        } else {
            logger.trace { "Cache hit for env '$env', returning ${tasks.size} tasks" }
        }
        return tasks
    }

    /**
     * Refresh tasks in the background (IO dispatcher) and update the cache.
     *
     * Calls are coalesced: if a refresh is already in-flight, the last requested environment
     * is recorded and refreshed once the current run finishes. Runs on [Dispatchers.IO] to avoid EDT blocking.
     */
    private fun queueTaskRefresh(configEnvironment: String? = null) {
        if (application.isUnitTestMode || project.isDisposed) return
        val env = configEnvironment ?: project.service<MiseProjectSettings>().state.miseConfigEnvironment
        if (!refreshInFlight.compareAndSet(false, true)) {
            logger.trace { "Coalescing refresh request for env '$env' (refresh already in-flight)" }
            pendingEnvironment.set(env)
            refreshPending.set(true)
            return
        }
        logger.trace { "Starting task refresh for env '$env' on IO dispatcher" }
        cs.launch(Dispatchers.IO) {
            try {
                computeTasksFromSource(configEnvironment = env)
            } catch (e: CancellationException) {
                logger.trace { "Mise task refresh for env '$env' was cancelled." }
                throw e
            } catch (e: Exception) {
                if (e !is ControlFlowException) {
                    logger.warn("Failed to refresh Mise tasks for env '$env'", e)
                }
            } finally {
                refreshInFlight.set(false)
                if (refreshPending.getAndSet(false)) {
                    val pendingEnv = pendingEnvironment.getAndSet(null)
                    logger.trace { "Processing coalesced refresh for env '$pendingEnv'" }
                    queueTaskRefresh(pendingEnv)
                }
            }
        }
    }

    /**
     * Compute tasks from source and write to cache.
     *
     * This always re-scans config/filesystem and should run off the EDT.
     * Prefer [getCachedTasksOrEmptyList] for UI/resolve paths.
     */
    suspend fun computeTasksFromSource(configEnvironment: String? = null): List<MiseTask> {
        val baseDirVf: VirtualFile =
            readAction {
                if (application.isUnitTestMode) {
                    VirtualFileManager.getInstance().findFileByUrl("temp:///src")
                        ?: VirtualFileManager.getInstance().findFileByUrl("file://${project.baseDirectory()}")
                } else {
                    VirtualFileManager.getInstance().findFileByUrl("file://${project.baseDirectory()}")
                }
            } ?: return emptyList()

        val configEnvironment = configEnvironment ?: project.service<MiseProjectSettings>().state.miseConfigEnvironment

        val cacheKey = configEnvironment
        val result = mutableListOf<MiseTask>()
        val configVfs =
            project.service<MiseConfigFileResolver>().resolveConfigFiles(baseDirVf, true, configEnvironment)

        // Resolve tasks from the config file
        // Assumption: configVfs is small (typically 1-5, max ~20) and files are typically short (<100 lines),
        // so holding a brief read lock via smartReadAction is acceptable and keeps the logic simple.
        smartReadAction(project) {
            for (configVf in configVfs) {
                val psiFile = configVf.findPsiFile(project) as? TomlFile ?: continue
                val tomlTableTasks: List<MiseTomlTableTask> = MiseTomlTableTask.resolveAllFromTomlFile(psiFile)
                result.addAll(tomlTableTasks)
            }
        }

        // Resolve tasks from the task config files
        smartReadAction(project) {
            for (configVf in configVfs) {
                val configPsiFile = configVf.findPsiFile(project) as? TomlFile ?: continue
                val taskIncludes = MiseTomlFile.TaskConfig.resolveOrNull(configPsiFile)?.includes ?: continue
                val taskTomlPsiFiles =
                    taskIncludes
                        .mapNotNull { baseDirVf.findFileOrDirectory(it) }
                        .filter { it.isFile }
                        .mapNotNull { it.findPsiFile(project) as? TomlFile }
                for (taskTomlPsiFile in taskTomlPsiFiles) {
                    val tomlTables = taskTomlPsiFile.childrenOfType<TomlTable>()
                    for (tomlTable in tomlTables) {
                        val keySegments = tomlTable.header.key?.segments ?: continue
                        val keySegment = keySegments.singleOrNull() ?: continue
                        val task = MiseTomlTableTask.resolveOrNull(keySegment) ?: continue
                        result.add(task)
                    }
                }
            }
        }

        // Resolve tasks from the task directories (Shell Script)
        val fileTaskDirectories = resolveFileTaskDirectories(project, baseDirVf, configEnvironment)
        for (fileTaskDir in fileTaskDirectories) {
            for (file in fileTaskDir.leafChildren()) {
                val isExecutable =
                    file.toNioPathOrNull()?.isExecutable()
                        ?: true // assume file is executable if we can't get its path
                if (!isExecutable) continue

                val task: MiseShellScriptTask = MiseShellScriptTask.resolveOrNull(fileTaskDir, file) ?: continue
                result.add(task)
            }
        }

        writeCache(cacheKey, result)
        staleFlag(cacheKey).set(false)
        return result
    }

    /**
     * Write tasks to cache and broadcast refresh event if UI-visible content changed.
     *
     * Only broadcasts TASK_CACHE_REFRESHED when hasDifferentUiContentFrom detects changes
     * that affect the UI (task names, sources, dependencies). This prevents spurious daemon
     * restarts when internal cache state changes but the visible task list is identical.
     */
    private fun writeCache(
        cacheKey: String,
        value: List<MiseTask>,
    ) {
        val previous = cache[cacheKey].orEmpty()
        cache[cacheKey] = value
        if (previous.hasDifferentUiContentFrom(value)) {
            MiseProjectEventListener.broadcast(
                project,
                MiseProjectEvent(
                    MiseProjectEvent.Kind.TASK_CACHE_REFRESHED,
                    "mise task cache was refreshed for $cacheKey",
                ),
            )
        }
    }

    override fun dispose() {}

    companion object {
        private val logger = logger<MiseTaskResolver>()
        private val DEFAULT_TASK_DIRECTORIES =
            listOf(
                "mise-tasks",
                ".mise-tasks",
                "mise/tasks",
                ".mise/tasks",
                ".config/mise/tasks",
            )

        private suspend fun resolveFileTaskDirectories(
            project: Project,
            baseDirVf: VirtualFile,
            configEnvironment: String? = null,
        ): List<VirtualFile> {
            val result = mutableListOf<VirtualFile>()

            // Resolve default task directories
            smartReadAction(project) {
                for (dir in DEFAULT_TASK_DIRECTORIES) {
                    val vf = baseDirVf.resolveFromRootOrRelative(dir) ?: continue
                    if (vf.isDirectory) {
                        result.add(vf)
                    }
                }
            }

            // Resolve task directories defined in the config file
            val configVfs =
                project.service<MiseConfigFileResolver>().resolveConfigFiles(baseDirVf, false, configEnvironment)
            smartReadAction(project) {
                for (configVf in configVfs) {
                    val psiFile = configVf.findPsiFile(project) as? TomlFile ?: continue
                    val taskTomlOrDirs = MiseTomlFile.TaskConfig.resolveOrNull(psiFile)?.includes ?: emptyList()

                    for (taskTomlOrDir in taskTomlOrDirs) {
                        val directory =
                            baseDirVf.findFileOrDirectory(taskTomlOrDir)?.takeIf { it.isDirectory } ?: continue
                        result.add(directory)
                    }
                }
            }

            return result
        }

        private fun VirtualFile.leafChildren(): Sequence<VirtualFile> =
            children.asSequence().flatMap {
                if (it.isDirectory) {
                    it.leafChildren()
                } else {
                    sequenceOf(it)
                }
            }
    }
}
