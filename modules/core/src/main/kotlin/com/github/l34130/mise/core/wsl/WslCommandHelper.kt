package com.github.l34130.mise.core.wsl

import com.github.l34130.mise.core.setting.MiseApplicationSettings
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.wsl.WSLCommandLineOptions
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.application
import java.nio.file.Path

/**
 * Helper to centralize WSL command and path handling via the IntelliJ Platform APIs.
 */
object WslCommandHelper {
    private val logger = logger<WslCommandHelper>()

    fun resolveDistribution(state: MiseApplicationSettings.MyState): WSLDistribution? {
        if (!SystemInfo.isWindows || !state.isWslMode) return null
        val manager = WslDistributionManager.getInstance()
        val id = state.wslDistribution
        if (!id.isNullOrBlank()) {
            manager.installedDistributions.firstOrNull { it.msId.equals(id, ignoreCase = true) }?.let { return it }
        }
        return manager.installedDistributions.firstOrNull()
    }

    /**
     * Resolve distribution from a UNC path when settings are not explicitly in WSL mode.
     */
    fun resolveDistributionFromPath(path: String?): WSLDistribution? {
        if (!SystemInfo.isWindows || path.isNullOrBlank()) return null
        val normalized = path.replace('/', '\\')
        val distroId = WslPath.parseWindowsUncPath(normalized)?.distributionId
            ?: WslPathUtils.extractDistribution(path)
        if (distroId.isNullOrBlank()) return null
        return WslDistributionManager.getInstance().installedDistributions
            .firstOrNull { it.msId.equals(distroId, ignoreCase = true) }
    }

    /**
     * Build a command line that executes inside WSL using the platform's patcher.
     */
    fun buildWslCommandLine(
        distribution: WSLDistribution,
        linuxCommand: List<String>,
        workDir: String?,
        project: Project? = null,
    ): GeneralCommandLine {
        val commandLine = GeneralCommandLine(linuxCommand)
        val options =
            WSLCommandLineOptions()
                .setRemoteWorkingDirectory(workDir)
                .setPassEnvVarsUsingInterop(true)
        return distribution.patchCommandLine(commandLine, project, options)
    }

    /**
     * Convert a Windows/UNC path into a Linux path for the given distribution.
     */
    fun toLinuxPath(distribution: WSLDistribution, path: String?): String? {
        if (path.isNullOrBlank()) return null

        // UNC paths
        if (path.startsWith("//") || path.startsWith("\\\\")) {
            val normalized = path.replace('/', '\\')
            val uncParsed =
                WslPath.parseWindowsUncPath(path)
                    ?: WslPath.parseWindowsUncPath(normalized)
            if (uncParsed != null) {
                return uncParsed.linuxPath
            }
            if (WslPath.isWslUncPath(normalized)) {
                return null
            }
        }

        // Linux path
        if (path.startsWith("/")) return path

        // Drive path
        return runCatching { distribution.getWslPath(Path.of(path)) }.getOrNull()
    }

    /**
     * Extract a Linux-side executable path from the configured executable string.
     * Falls back to "mise" when not derivable.
     */
    fun linuxExecutableFromConfig(executablePath: String): String {
        // UNC -> linux path
        WslPathUtils.convertWindowsUncToUnixPath(executablePath)?.let { return it }

        // Command that starts with wsl - assume last token is the linux path if it looks absolute
        if (executablePath.contains("wsl", ignoreCase = true)) {
            val tokens = executablePath.trim().split(Regex("\\s+"))
            val candidate = tokens.lastOrNull()
            if (candidate != null && candidate.startsWith("/")) {
                return candidate
            }
        }

        // Already a linux-looking path
        if (executablePath.startsWith("/")) return executablePath

        logger.debug("Falling back to plain 'mise' executable for WSL (could not parse): $executablePath")
        return "mise"
    }

    /**
     * Decide on the working directory to feed into WSL.
     */
    fun toLinuxWorkDir(distribution: WSLDistribution, workDir: String?): String? {
        if (workDir.isNullOrBlank()) {
            return null
        }

        val converted = toLinuxPath(distribution, workDir)
        return converted ?: workDir
    }
}
