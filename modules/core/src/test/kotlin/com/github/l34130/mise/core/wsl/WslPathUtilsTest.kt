

package com.github.l34130.mise.core.wsl

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Assume.assumeTrue

class WslPathUtilsTest : LightPlatformTestCase() {
    // ========================
    // extractDistribution() Tests
    // ========================
    fun `test extractDistribution from wsl localhost UNC path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "\\\\wsl.localhost\\Ubuntu\\usr\\bin\\mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Ubuntu", result)
    }
    fun `test extractDistribution from wsl dollar sign UNC path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "\\\\wsl\$\\Debian\\home\\user\\mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Debian", result)
    }
    fun `test extractDistribution returns null for non-WSL path`() {
        val path = "C:\\Program Files\\mise\\mise.exe"
        val result = WslPathUtils.extractDistribution(path)
        assertNull(result)
    }
    fun `test extractDistribution returns null for empty path`() {
        val path = ""
        val result = WslPathUtils.extractDistribution(path)
        assertNull(result)
    }

    // ========================
    // convertWindowsUncToUnixPath() Tests
    // ========================
    fun `test convertWindowsUncToUnixPath converts wsl localhost path with backslashes`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val uncPath = "$uncRoot\\home\\user\\.mise"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/home/user/.mise", result)
    }
    fun `test convertWindowsUncToUnixPath converts wsl localhost path with forward slashes`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution).replace('\\', '/')
        val uncPath = "$uncRoot/usr/bin/node"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/usr/bin/node", result)
    }
    fun `test convertWindowsUncToUnixPath handles path with spaces`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val uncPath = "$uncRoot\\home\\user\\my folder\\file.txt"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/home/user/my folder/file.txt", result)
    }
    fun `test convertWindowsUncToUnixPath handles mixed slashes`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val mixedRoot = uncRoot.replaceFirst("\\\\", "//")
        val uncPath = "$mixedRoot/home/testuser\\src\\github"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/home/testuser/src/github", result)
    }
    fun `test convertWindowsUncToUnixPath returns null for non-UNC path`() {
        val uncPath = "C:\\Users\\project"
        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)
        assertNull(result)
    }
    fun `test convertWindowsUncToUnixPath returns null for empty path`() {
        val uncPath = ""
        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)
        assertNull(result)
    }
    fun `test convertWindowsUncToUnixPath returns null for blank path`() {
        val uncPath = "   "
        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)
        assertNull(result)
    }
    fun `test convertWindowsUncToUnixPath handles root path`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val uncPath = "$uncRoot\\"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/", result)
    }

    private fun assumeFirstDistribution(): WSLDistribution {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val distributions = WslDistributionManager.getInstance().installedDistributions
        assumeTrue("No WSL distributions installed", distributions.isNotEmpty())
        return distributions.first()
    }

    private fun uncRoot(distribution: WSLDistribution): String =
        @Suppress("UnstableApiUsage")
        distribution.getUNCRootPath().toString().trimEnd('\\', '/')
}
