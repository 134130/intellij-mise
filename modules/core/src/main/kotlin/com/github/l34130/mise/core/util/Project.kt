package com.github.l34130.mise.core.util

import com.github.l34130.mise.core.setting.MiseApplicationSettings
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.EnvironmentUtil
import com.intellij.util.SystemProperties
import com.intellij.util.application
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val logger = Logger.getInstance("com.github.l34130.mise.core.util.Project")

private data class ProjectInfo(
    val wslDistribution: WSLDistribution?,
    val userHome: String,
    val shellPath: String?
)

private val PROJECT_INFO_KEY = Key.create<CachedValue<ProjectInfo>>("mise.project.info")
private val PROJECT_CACHE_LATCH_KEY = Key.create<CountDownLatch>("mise.project.cache.latch")

/**
 * Gets the canonical path for the Mise project directory.
 *
 * This provides a consistent way to get the project path suitable for use with
 * mise commands and file system operations.
 *
 * @return The canonical path of the project directory
 * @throws IllegalStateException when the project directory cannot be resolved to a canonical path
 */
fun Project.guessMiseProjectPath(): String {
    val projectDir = guessMiseProjectDir()
    return checkNotNull(projectDir.canonicalPath) {
        "Mise project dir has no canonical path. project=${this.name}, dir=${projectDir.path}, fs=${projectDir.fileSystem.protocol}"
    }
}

/**
 * Gets the VirtualFile for the Mise project directory.
 *
 * This provides a consistent way to get the VirtualFile that represents the project home
 *
 * @return The virtual file representing the project directory, or falls back to the user's home directory
 * @throws IllegalStateException when neither the project directory nor user home can be resolved
 */
fun Project.guessMiseProjectDir(): VirtualFile {
    val projectDir = guessProjectDir()
    if (projectDir != null) return projectDir

    val userHome = SystemProperties.getUserHome()
    val userHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(userHome)
    checkNotNull(userHomeDir) {
        "Project has no base directory and user home is unavailable. project=${this.name}, userHome=$userHome"
    }

    logger.warn("guessProjectDir() returned null for project ${this.name}, falling back to user home: ${userHomeDir.path}")
    return userHomeDir
}

/**
 * Gets the shell path appropriate for the project's execution environment.
 *
 * For WSL projects, returns the Windows UNC path to the user's shell
 * (e.g., `\\wsl.localhost\Ubuntu\usr\bin\zsh`).
 * For native projects, returns the shell from the SHELL environment variable.
 *
 * @return Shell path for the project environment, or null if unavailable
 */
fun Project.getProjectShell(): String? {
    return getProjectInfo().shellPath
}

/**
 * Gets the user home directory path appropriate for the project's execution environment.
 *
 * For WSL projects, returns the Windows UNC path to the WSL user's home directory
 * (e.g., `\\wsl.localhost\Ubuntu\home\user`).
 * For native projects, returns the system user home directory.
 *
 * @return User home path for the project environment
 */
fun Project.getUserHomeForProject(): String {
    return getProjectInfo().userHome
}

fun Project.getWslDistribution(): WSLDistribution? {
    return getProjectInfo().wslDistribution
}

fun Project.prewarmProjectInfo() {
    getProjectInfo()
}

fun Project.markProjectCacheReady() {
    getProjectCacheLatch().countDown()
}

private fun Project.getProjectInfo(): ProjectInfo {
    val project = this
    val cachedValuesManager = CachedValuesManager.getManager(project)
    return cachedValuesManager.getCachedValue(
        project,
        PROJECT_INFO_KEY,
        {
            val info =
                if (SystemInfo.isWindows) {
                    runWithProgressIndicator { project.computeProjectInfo() }
                } else {
                    project.computeProjectInfo()
                }
            val cachedResult = CachedValueProvider.Result.create(info, ModificationTracker.NEVER_CHANGED)
            project.markProjectCacheReady()
            cachedResult
        },
        false,
    )
}

private fun Project.computeProjectInfo(): ProjectInfo {
    if (!SystemInfo.isWindows) {
        logger.debug("Project info skipped: non-Windows project")
        return ProjectInfo(
            wslDistribution = null,
            userHome = SystemProperties.getUserHome(),
            shellPath = EnvironmentUtil.getValue("SHELL")
        )
    }

    val distribution = resolveWslDistribution()
    val userHome = distribution?.userHome
    val resolvedUserHome =
        if (!userHome.isNullOrBlank()) {
            distribution.getWindowsPath(userHome)
        } else {
            SystemProperties.getUserHome()
        }

    val shellPath = distribution?.shellPath
    val resolvedShellPath =
        if (!shellPath.isNullOrBlank()) {
            distribution.getWindowsPath(shellPath)
        } else if (!EnvironmentUtil.getValue("SHELL").isNullOrBlank()) {
            EnvironmentUtil.getValue("SHELL")
        } else {
            "cmd"
        }

    val info = ProjectInfo(
        wslDistribution = distribution,
        userHome = resolvedUserHome,
        shellPath = resolvedShellPath
    )
    val distroId = distribution?.msId ?: "null"
    logger.debug("Project info cached: distro=$distroId userHome=$resolvedUserHome shellPath=${resolvedShellPath ?: "null"}")
    return info
}

private fun Project.resolveWslDistribution(): WSLDistribution? {
    if (!SystemInfo.isWindows) return null

    val projectPath = guessMiseProjectPath()
    val distributionId =
        WslPathUtils.extractDistribution(projectPath)
            ?: run {
                val configuredPath = application.service<MiseApplicationSettings>().state.executablePath
                configuredPath.takeIf { it.isNotBlank() }?.let { WslPathUtils.extractDistribution(it) }
            }

    if (distributionId.isNullOrBlank()) return null

    val manager = WslDistributionManager.getInstance()
    return manager.cachedInstalledDistributions?.firstOrNull { it.msId.equals(distributionId, ignoreCase = true) }
        ?: manager.installedDistributions.firstOrNull { it.msId.equals(distributionId, ignoreCase = true) }
}

private fun Project.getProjectCacheLatch(): CountDownLatch {
    return synchronized(this) {
        getUserData(PROJECT_CACHE_LATCH_KEY) ?: CountDownLatch(1).also { latch ->
            putUserData(PROJECT_CACHE_LATCH_KEY, latch)
        }
    }
}

private fun <T> runWithProgressIndicator(action: () -> T): T {
    val progressManager = ProgressManager.getInstance()
    val indicator = progressManager.progressIndicator
    return if (indicator != null) {
        action()
    } else {
        progressManager.runProcess(
            Computable<T> { action() },
            EmptyProgressIndicator()
        )
    }
}

fun Project.waitForProjectCache(): Boolean {
    return try {
        getProjectCacheLatch().await(10, TimeUnit.SECONDS)
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        false
    }
}
