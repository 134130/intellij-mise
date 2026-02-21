package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.setting.MiseApplicationSettings
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.getProjectShell
import com.github.l34130.mise.core.util.getUserHomeForProject
import com.github.l34130.mise.core.util.getWslDistribution
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.wsl.WslPathUtils.maybeConvertWindowsUncToUnixPath
import com.github.l34130.mise.core.wsl.WslPathUtils.resolveUserHomeAbbreviations
import com.intellij.execution.Platform
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.application
import com.intellij.util.concurrency.ThreadingAssertions.assertBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.system.OS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

data class MiseExecutableInfo(
    val path: String,
    val version: MiseVersion?
)

/**
 * Single source of truth for mise executable path resolution.
 *
 * Resolution priority:
 * 1. Project-level user-configured path (if set)
 * 2. Application-level user-configured path (if set)
 * 3. Auto-detected path from PATH or common locations
 *
 * All resolved paths are cached per project and invalidated when:
 * - User changes the executable path in settings
 * - The cached path is modified/deleted (detected via VFS listener)
 */
@Service(Service.Level.PROJECT)
class MiseExecutableManager(
    private val project: Project,
    private val cs: CoroutineScope
) : Disposable {
    private val logger = logger<MiseExecutableManager>()
    private val cacheService = project.service<MiseCacheService>()

    // The service is its own disposable
    override fun dispose() {}

    init {
        // Keep the executable cache aligned with project lifecycle and settings changes.
        MiseProjectEventListener.subscribe(project, this) { event ->
            when (event.kind) {
                MiseProjectEvent.Kind.STARTUP -> warmCache()
                MiseProjectEvent.Kind.SETTINGS_CHANGED -> handleExecutableChange("settings changed")
                else -> Unit
            }
        }

        // Listen to file system changes
        val connection = project.messageBus.connect(this)
        connection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events.forEach { event ->
                        val path = event.path
                        val execCached = cacheService.getCachedExecutable(EXECUTABLE_KEY)?.path
                        // Only react if the cached executable itself changed on disk.
                        if ((path == execCached)) {
                            handleExecutableChange("file changed: $path", true)
                        }
                    }
                }
            }
        )
    }


    /**
     * Handle executable change from any source (settings or VFS).
     * Invalidates cache, broadcasts to listeners, and re-warms cache in the background.
     */
    fun handleExecutableChange(reason: String, forceBroadcast: Boolean = false) {
        // Capture the previous path to avoid broadcasting when nothing actually changed.
        val previousPath = cacheService.getCachedExecutable(EXECUTABLE_KEY)?.path
        cacheService.invalidateAllExecutables()
        cs.launch(Dispatchers.IO) {
            val newPath = refreshExecutablePathCache()

            if (forceBroadcast || newPath != previousPath) {
                logger.info("Mise executable changed ($reason), notifying listeners")
                MiseProjectEventListener.broadcast(
                    project,
                    MiseProjectEvent(MiseProjectEvent.Kind.EXECUTABLE_CHANGED, reason)
                )
                return@launch
            }
        }
    }


    fun warmCache() {
        cs.launch(Dispatchers.IO) {
            refreshExecutablePathCache()
        }
    }

    private fun refreshExecutablePathCache(): String? {
        return try {
            logger.debug("Warming executable path cache")
            val newPath = getExecutablePath()
            logger.debug("Executable path cache re-warmed successfully")
            newPath
        } catch (e: Exception) {
            logger.warn("Failed to warm executable cache", e)
            null
        }
    }

    companion object {
        const val EXECUTABLE_KEY = "executable-path"
        const val AUTO_DETECTED_KEY = "auto-detected-path"

        /**
         * Version command used for mise detection and verification.
         * Using -vv ensures the debug output shows the actual executable path.
         */
        private val MISE_VERSION_COMMAND = listOf("version", "-vv")
    }

    fun matchesMiseExecutablePath(commandLine: GeneralCommandLine): Boolean {
        val candidateParts = commandLine.getCommandLineList(null)
        val execParts = getExecutableParts()
        return execParts.isNotEmpty() && candidateParts.size >= execParts.size && execParts == candidateParts.take(
            execParts.size
        )
    }

    fun getExecutableParts(): List<String> = ParametersListUtil.parse(getExecutablePath())

    /**
     * Get the mise executable info to use.
     * This is the single source of truth for all mise operations.
     * All WSL handling is delegated to the Eel layer.
     * This is NEVER a multi-argument call to something like `wsl.exe -d <DISTRO> -e cmd`
     *
     * @return Full path and version of mise executable
     */
    fun getExecutableInfo(): MiseExecutableInfo {
        return cacheService.getOrComputeExecutable(EXECUTABLE_KEY) {
            val projectSettings = project.service<MiseProjectSettings>()
            val appSettings = application.service<MiseApplicationSettings>()
            val projectPath = project.guessMiseProjectPath()

            // Priority 1: Project-level user configuration
            val projectExecutablePath = projectSettings.state.executablePath
            if (projectExecutablePath.isNotBlank()) {
                logger.debug("Using project-configured executable: $projectExecutablePath")
                return@getOrComputeExecutable detectMiseExecutableInfo(projectPath, projectExecutablePath)
            }

            // Priority 2: Application-level user configuration
            val applicationExecutablePath = appSettings.state.executablePath
            if (applicationExecutablePath.isNotBlank()) {
                logger.debug("Using app-configured executable: $applicationExecutablePath")
                return@getOrComputeExecutable detectMiseExecutableInfo(projectPath, applicationExecutablePath)
            }

            // Priority 3: Auto-detect (with caching)
            getAutoDetectedInfo()
        }
    }

    fun getExecutablePath(): String = getExecutableInfo().path

    fun getExecutableVersion(): MiseVersion? = getExecutableInfo().version

    fun getAutoDetectedExecutableInfo(): MiseExecutableInfo = getAutoDetectedInfo()

    private fun getAutoDetectedInfo(): MiseExecutableInfo {
        return cacheService.getOrComputeExecutable(AUTO_DETECTED_KEY) {
            val projectPath = project.guessMiseProjectPath()

            // Detect mise executable (pass the project path for WSL context)
            val detected = detectMiseExecutableInfo(projectPath)

            if (detected.version != null) {
                logger.info("Auto-detected mise executable: ${detected.path}, version: ${detected.version}")
                detected
            } else {
                // Fallback to "mise" (will rely on PATH at runtime)
                logger.info("Could not auto-detect mise executable, using 'mise' as fallback")
                MiseExecutableInfo(path = "mise", version = null)
            }
        }
    }

    /**
     * Auto-detect the mise executable path using shell execution and common installation locations.
     * Tries to execute the given command (allowing shell PATH resolution) and falls back to known locations.
     *
     * @param workDir Working directory to determine context (WSL vs. native). Must not be blank.
     * @return Detected path to mise executable, or null if not found
     * @throws IllegalArgumentException if workDir is blank
     */
    private fun detectMiseExecutableInfo(workDir: String, executable: String = "mise"): MiseExecutableInfo {
        require(workDir.isNotBlank()) { "workDir must not be blank" }

        // Get the shell path using project extension (handles WSL vs. native)
        val shell = project.getProjectShell()
        val distribution = project.getWslDistribution()

        // Route to appropriate detection
        return when {
            shell != null && (distribution != null || OS.CURRENT.platform == Platform.UNIX) -> {
                // WSL or Unix
                logger.trace("Detecting mise executable on Linux (workDir: $workDir)")
                detectOnUnix(executable, shell)
            }

            OS.CURRENT.platform == Platform.WINDOWS -> {
                logger.trace("Detecting mise executable on Windows (workDir: $workDir)")
                // Native Windows (not WSL)
                detectOnWindows(executable, shell)
            }

            else -> {
                logger.warn("Could not determine shell for mise detection (workDir: $workDir)")
                null
            }
        } ?: MiseExecutableInfo(path = executable, version = null)
    }

    private data class VersionCommandResult(
        val resolvedPath: String?,
        val version: MiseVersion?
    )


    /**
     * Detect mise on Windows by executing the version command and falling back to common install paths.
     */
    @RequiresBackgroundThread
    private fun detectOnWindows(executable: String = "mise", shell: String?): MiseExecutableInfo? {
        assertBackgroundThread()
        val interactiveShellCommand = if (shell == null ) {
            listOf()
        } else {
            listOf(shell, "/c")
        }

        // fallback: Check common installation paths
        val userHome = Path(project.getUserHomeForProject())
        val fallbackExecPaths = listOf(
            userHome.resolve("AppData/Local/Microsoft/WinGet/Links/mise.exe"),
            userHome.resolve("scoop/apps/mise/current/bin/mise.exe")
        )

        return detectAndVerify(executable, interactiveShellCommand, fallbackExecPaths)
    }

    /**
     * Detect mise on Unix-like systems (Linux/macOS/WSL) using the user's shell.
     * Uses login shell (-l) to ensure rc files are sourced (e.g., ~/.bashrc, ~/.zshrc).
     * When the project path is a WSL UNC path, the Eel layer automatically handles WSL execution.
     *
     * @param shell Shell path to use (Windows UNC for WSL, native path for Unix/macOS)
     * @return Detected info, or null if not found
     */
    @RequiresBackgroundThread
    private fun detectOnUnix(
        executable: String,
        shell: String
    ): MiseExecutableInfo? {
        assertBackgroundThread()
        val interactiveShellCommand = listOf(shell, "-l", "-c")
        val userHome = Path(project.getUserHomeForProject())
        val fallbackExecPaths = listOf(
            userHome.resolve(".local/bin/mise")
        ).filter { it.toFile().canExecute() }

        return detectAndVerify(executable, interactiveShellCommand, fallbackExecPaths)
    }

    private fun detectAndVerify(
        executable: String,
        interactiveShellCommandList: List<String>,
        fallbackExecPaths: List<Path>
    ): MiseExecutableInfo? {
        val workDir = project.guessMiseProjectPath()
        // First try the given executable as-is (user-supplied path or the default "mise").
        val detected = runVersionCommand(executable, interactiveShellCommandList, workDir)

        // Then double-check that the resolved path actually executes (the path is inferred from output).
        val detectedPath = detected?.resolvedPath
        if (detectedPath != null) {
            // Verify by running the newly detected path again
            val verified = runVersionCommand(detectedPath, interactiveShellCommandList, workDir)

            // and checking that we get the same result back
            if (verified != null) {
                val verifiedPath = verified.resolvedPath ?: detectedPath
                val version = verified.version ?: detected.version
                return MiseExecutableInfo(path = verifiedPath, version = version)
            } else {
                logger.warn("Detected mise at $detectedPath but verification failed")
            }
        }

        // If detection failed, try fallback paths and re-verify using the same logic.
        for (candidateExecPath in fallbackExecPaths) {
            if (runCatching { candidateExecPath.toFile().canExecute() }.getOrNull() == true) {
                val candidateExecPathStr = candidateExecPath.absolutePathString()

                // Verify the fallback path
                val verified = detectAndVerify(candidateExecPathStr, interactiveShellCommandList, emptyList())

                if (verified != null) {
                    logger.info("Detected mise at fallback path: ${verified.path}")
                    return verified
                }
            }
        }
        return null
    }

    /**
     * Detect mise executable by running a command and parsing the debug output from 'version -vv'.
     * Captures both stdout and stderr as debug output can appear in either stream.
     *
     * @param executable The executable to run (e.g., "mise" or full path like "/usr/bin/mise")
     * @param interactiveShellCommandList A list of the shell executable (e.g., "/bin/zsh", "cmd") + the command line arguments to run a single command in an interactive shell.
     * @param workDir Working directory for the command
     * @return Detected mise executable path and version, or null if not found
     */
    private fun runVersionCommand(
        executable: String,
        interactiveShellCommandList: List<String>,
        workDir: String
    ): VersionCommandResult? {
        // Avoid running external processes under read actions; hop to pooled thread when needed.
        return if (application.isReadAccessAllowed) {
            runCatching {
                application.executeOnPooledThread<VersionCommandResult?> {
                    runVersionCommandInternal(executable, interactiveShellCommandList, workDir)
                }.get()
            }.getOrNull()
        } else {
            runVersionCommandInternal(executable, interactiveShellCommandList, workDir)
        }
    }

    @RequiresBackgroundThread
    private fun runVersionCommandInternal(
        executable: String,
        interactiveShellCommand: List<String>,
        workDir: String
    ): VersionCommandResult? {
        assertBackgroundThread()

        try {
            // Build the full command. Use the shell if the executable is just a bare command so that shell magic can do its work
            // Otherwise just directly call the executable if there is a full path.
            val commandLineParams = if (isBareCommand(executable)) {
                // If we're running in shell, the executable must be POSIX/Unix format, otherwise we allow EelApi to handle it.
                val nativeExecutable = maybeConvertWindowsUncToUnixPath(executable)
                val shellVersionCommand = (listOf(nativeExecutable) + MISE_VERSION_COMMAND).joinToString(" ")
                // i.e. [`/bin/bash`, `-lc`, `mise version -vv`]
                interactiveShellCommand + shellVersionCommand
            } else {
                // i.e. [`mise`, `version`, `-vv`] or [`/usr/local/bin/mise`, `version`, `-vv`]
                listOf(executable) + MISE_VERSION_COMMAND
            }

            val commandLine = GeneralCommandLine(commandLineParams)
                .withWorkingDirectory(Path.of(workDir))

            // Add an injection marker to prevent environment customization during detection
            MiseCommandLineHelper.environmentSkipCustomization(commandLine.environment)

            // Execute command using a shared helper (failures are expected during detection)
            val processOutput = MiseCommandLine.executeCommandLine(commandLine, allowedToFail = true, timeout = 3000)
                .getOrElse { return null }

            // Capture both stdout and stderr
            val combinedOutput = processOutput.stderr + "\n" + processOutput.stdout
            logger.debug(combinedOutput)

            val resolvedPath = combinedOutput.lineSequence()
                .firstOrNull { line -> line.contains("MISE_BIN:") }
                ?.substringAfter("MISE_BIN:")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { resolveUserHomeAbbreviations(it, project).toString() }

            val version = parseVersionFromOutput(combinedOutput)
            return VersionCommandResult(resolvedPath = resolvedPath, version = version)
        } catch (e: Exception) {
            logger.debug("Failed to detect mise via version command with executable='$executable'", e)
        }

        return null
    }

    private fun parseVersionFromOutput(output: String): MiseVersion? {
        return output.lineSequence().firstNotNullOfOrNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                null
            } else {
                runCatching { MiseVersion.parse(trimmed) }.getOrNull()
            }
        }
    }

    private fun isBareCommand(executablePath: String): Boolean {
        return !executablePath.contains("/") && !executablePath.contains("\\")
    }
}
