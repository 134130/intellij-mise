package com.github.l34130.mise.core.wsl

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.setting.MiseApplicationSettings
import com.github.l34130.mise.core.util.getUserHomeForProject
import com.github.l34130.mise.core.util.getWslDistribution
import com.github.l34130.mise.core.wsl.WslPathUtils.convertUnixPathForWsl
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.application
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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
    private val logger = logger<WslPathUtils>()

    // Regex pattern for WSL command detection (wsl.exe -d format)
    // WslPath API handles UNC paths, but not command-line format
    // Matches: wsl.exe -d "Ubuntu 20.04" or wsl -d Ubuntu or wsl.exe -d 'Debian'
    private val WSL_COMMAND_PATTERN = Regex("""wsl(?:\.exe)?\s+-d\s+(?:"([^"]+)"|'([^']+)'|(\S+))""")

    /**
     * Detects if the given executable path represents a WSL-based mise installation.
     *
     * Checks for:
     * - "wsl.exe" or "wsl " in the path
     * - UNC paths starting with "\\wsl.localhost\" or "\\wsl$\" (or with forward slashes)
     *
     * @param executablePath The path to check
     * @return true if WSL mode is detected, false otherwise
     */
    fun detectWslMode(executablePath: String): Boolean {
        // Only works on Windows where IntelliJ's WslPath API is available
        if (!SystemInfo.isWindows) return false
        if (executablePath.isBlank()) return false

        // Check for wsl.exe command format
        if (executablePath.contains("wsl.exe", ignoreCase = true) ||
            executablePath.startsWith("wsl ", ignoreCase = true)
        ) {
            return true
        }

        // Use IntelliJ's WslPath API for UNC path detection
        // Handles both \\wsl.localhost\ and \\wsl$\ formats
        val normalizedPath = executablePath.replace('/', '\\')
        return WslPath.isWslUncPath(normalizedPath)
    }

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
        // Only works on Windows where IntelliJ's WslPath API is available
        if (!SystemInfo.isWindows) return null
        if (executablePath.isBlank()) return null

        // Try command format: wsl.exe -d <distro>
        // Handles: "Ubuntu 20.04" (double quotes), 'Debian' (single quotes), or Ubuntu (unquoted)
        WSL_COMMAND_PATTERN.find(executablePath)?.let { matchResult ->
            // Check groups 1, 2, 3 for the distribution name (one will be non-empty)
            return matchResult.groups[1]?.value
                ?: matchResult.groups[2]?.value
                ?: matchResult.groups[3]?.value
        }

        // Use IntelliJ's WslPath API for UNC path parsing
        val normalizedPath = executablePath.replace('/', '\\')
        WslPath.parseWindowsUncPath(normalizedPath)?.let { wslPath ->
            return wslPath.distributionId
        }

        // Fallback: only try to get a distribution if this looks like a WSL path
        // (to avoid returning a distribution for non-WSL paths)
        if (!detectWslMode(executablePath)) {
            return null
        }

        // This is a WSL path but distribution wasn't explicitly specified
        // Try to get a distribution using IntelliJ's WslDistributionManager
        // Note: We return the first installed distribution as a best-effort fallback
        // This may not be the WSL default distribution; users should specify explicitly
        return try {
            val distributions = WslDistributionManager.getInstance().installedDistributions
            val firstDistribution = distributions.firstOrNull()

            if (firstDistribution != null) {
                logger.debug(
                    "Distribution not explicitly specified in path, using first available: ${firstDistribution.msId}"
                )
                firstDistribution.msId
            } else {
                logger.debug("No WSL distributions found")
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to query WSL distributions", e)
            null
        }
    }

    /**
     * Converts a Unix path (from WSL) to a Windows UNC path.
     *
     * Uses IntelliJ's WslPath API to ensure modern \\wsl.localhost\ format.
     *
     * Examples:
     * - "/home/user/.mise" → "\\wsl.localhost\Ubuntu\home\user\.mise"
     * - "/usr/bin/node" → "\\wsl.localhost\Debian\usr\bin\node"
     *
     * @param unixPath The Unix-style path to convert
     * @param distribution The WSL distribution name
     * @return The converted Windows UNC path
     * @throws IllegalArgumentException if unixPath doesn't start with /, distribution is blank, or distribution is not installed
     */
    fun convertWslToWindowsUncPath(unixPath: String, distribution: String): String {
        // Only makes sense on Windows
        if (!SystemInfo.isWindows) return unixPath

        require(unixPath.startsWith("/")) {
            "Unix path must start with /: $unixPath"
        }
        require(distribution.isNotBlank()) {
            "Distribution name cannot be blank"
        }

        // Get the WSL distribution object to use its actual UNC root path
        // This ensures we use \\wsl.localhost\ (modern) instead of \\wsl$\ (legacy)
        val wslDistribution = WslDistributionManager.getInstance().installedDistributions
            .find { it.msId.equals(distribution, ignoreCase = true) }
            ?: throw IllegalArgumentException(
                "WSL distribution '$distribution' not found. " +
                        "Available distributions: ${WslDistributionManager.getInstance().installedDistributions.joinToString { it.msId }}"
            )

        // Use the distribution's method which respects the system's WSL version
        // Convert Path to string and remove any trailing separator to avoid double slashes
        return wslDistribution.getWindowsPath(unixPath)
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

    fun maybeConvertWindowsUncToUnixPath(maybeUncPath: String): String = convertWindowsUncToUnixPath(maybeUncPath) ?: maybeUncPath

    /**
     * Validates that a UNC path exists and is accessible.
     *
     * @param uncPath The UNC path to validate
     * @return true if the path exists and is accessible, false otherwise
     */
    fun validateUncPath(uncPath: String): Boolean {
        // Only validate on Windows where UNC paths are relevant
        if (!SystemInfo.isWindows) return true
        if (uncPath.isBlank()) return false

        return try {
            val path = Paths.get(uncPath)
            Files.exists(path)
        } catch (e: Exception) {
            logger.warn("Failed to validate UNC path: $uncPath", e)
            false
        }
    }

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
    fun convertUnixPathForWsl(unixPath: String, executablePath: String? = null): String {
        if (!SystemInfo.isWindows) return unixPath

        // Try to detect distribution from executable path
        val distribution = executablePath?.let { extractDistribution(it) }
            ?: run {
                // Fallback: check application settings
                val settings = application.service<MiseApplicationSettings>()
                val configuredPath = settings.state.executablePath
                if (configuredPath.isNotEmpty()) {
                    extractDistribution(configuredPath)
                } else {
                    null
                }
            }

        if (distribution == null) {
            // Not in WSL context, return original path
            logger.debug("Not in WSL context, returning original path: $unixPath")
            return unixPath
        }

        return try {
            val uncPath = convertWslToWindowsUncPath(unixPath, distribution)
            if (!validateUncPath(uncPath)) {
                logger.warn("Converted UNC path is not currently accessible (WSL may be cold): $uncPath; proceeding")
            } else {
                logger.info("Converted WSL path: $unixPath -> $uncPath")
            }
            uncPath
        } catch (e: Exception) {
            logger.error("Failed to convert WSL path: $unixPath", e)
            throw IllegalStateException(
                "Cannot access path at $unixPath in WSL. " +
                        "Ensure WSL is running and distribution '$distribution' is accessible.",
                e,
            )
        }
    }

    /**
     * Converts a mise tool's install path for WSL compatibility.
     *
     * This is a convenience wrapper around [convertUnixPathForWsl] that extracts
     * the path from a MiseDevTool object.
     *
     * @param tool The MiseDevTool containing the install path to convert
     * @return The converted path (Windows UNC path for WSL, original path otherwise)
     * @throws IllegalStateException if WSL mode is enabled but distribution is not configured,
     *         or if the converted UNC path is not accessible
     */
    fun convertToolPathForWsl(tool: MiseDevTool): String {
        return convertUnixPathForWsl(tool.shimsInstallPath())
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
