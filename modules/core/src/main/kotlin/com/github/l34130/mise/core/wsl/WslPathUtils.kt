package com.github.l34130.mise.core.wsl

import com.github.l34130.mise.core.util.getUserHomeForProject
import com.github.l34130.mise.core.util.getWslDistribution
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Utility object for WSL (Windows Subsystem for Linux) path operations and detection.
 *
 * This utility leverages IntelliJ Platform's WSL APIs (WslPath, WslDistributionManager, WSLDistribution)
 * for consistent and reliable WSL integration on Windows.
 *
 * Provides functions to:
 * - Detect WSL mode from executable paths
 * - Extract WSL distribution names
 * - Convert between Windows and WSL path formats (UNC and mount paths)
 * - Validate UNC paths
 * - Discover mise installations in WSL distributions
 */
object WslPathUtils {
    /**
     * Extracts the WSL distribution name from an executable path.
     *
     * Supports formats:
     * - Command: "wsl.exe -d Ubuntu ..." → "Ubuntu"
     * - UNC: "\\wsl.localhost\Debian\..." → "Debian"
     * - UNC (old): "\\wsl$\Ubuntu\..." → "Ubuntu"
     *
     * @param executablePath The path containing distribution information
     * @return The distribution name, or null if not found
     */
    fun extractDistribution(executablePath: String): String? {
        // Default to platform standard parsing.
        return WslPath.getDistributionByWindowsUncPath(executablePath)?.msId
    }

    /**
     * Converts a Windows UNC WSL path to a Unix path.
     *
     * Examples:
     * - "\\wsl.localhost\Ubuntu\home\user\.mise" → "/home/user/.mise"
     * - "//wsl.localhost/Debian/usr/bin/node" → "/usr/bin/node"
     * - "\\wsl$\Ubuntu\opt\mise" → "/opt/mise"
     *
     * @param uncPath The UNC WSL path to convert
     * @return The Unix path, or null if not a valid UNC WSL path
     */
    fun convertWindowsUncToUnixPath(uncPath: String): String? {
        // Only works on Windows where IntelliJ's WslPath API is available
        if (!SystemInfo.isWindows) return null
        if (uncPath.isBlank()) return null

        // Use IntelliJ's WslPath API for parsing
        val wslPath = WslPath.parseWindowsUncPath(uncPath) ?: return null
        return wslPath.linuxPath
    }

    fun maybeConvertWindowsUncToUnixPath(maybeUncPath: String): String =
        convertWindowsUncToUnixPath(maybeUncPath) ?: maybeUncPath

    /**
     * Converts a Unix path to a Windows UNC path for WSL compatibility.
     *
     * On non-Windows systems, returns the original path unchanged.
     * On Windows, attempts to detect WSL context from the executable path and convert accordingly.
     *
     * @param unixPath The Unix path to convert
     * @param executablePath The executable path to infer distribution from (optional)
     * @return The converted path (Windows UNC path for WSL, original path otherwise)
     * @throws IllegalStateException if distribution cannot be detected or path is not accessible
     */
    fun maybeConvertUnixPathToWsl(unixPath: String, executablePath: String?): String {
        if (executablePath == null) return unixPath
        return WslPath.getDistributionByWindowsUncPath(executablePath)?.getWindowsPath(unixPath) ?: unixPath
    }

    fun resolveUserHomeAbbreviations(
        path: String,
        project: Project
    ): Path {
        val homePrefixes = listOf("~/", "~\\", $$"$HOME/", $$"$HOME\\", $$"${HOME}/", $$"${HOME}\\")
        val matchingPrefix = homePrefixes.firstOrNull { path.startsWith(it) }

        val resolvedHome = if (matchingPrefix != null) {
            val userHome = Path(project.getUserHomeForProject())
            userHome.resolve(path.removePrefix(matchingPrefix)).toAbsolutePath()
        } else {
            Path(path).toAbsolutePath()
        }
        val distribution = project.getWslDistribution()
        if (distribution != null && resolvedHome.startsWith("/")) {
            return Path(distribution.getWindowsPath(resolvedHome.toString()))
        }
        return resolvedHome
    }
}
