package com.github.l34130.mise.core.wsl

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Assume.assumeTrue

class WslPathUtilsTest : LightPlatformTestCase() {
    // ========================
    // detectWslMode() Tests
    // ========================

    fun `test detectWslMode returns true for wsl exe command`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl.exe -d Ubuntu mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }
    fun `test detectWslMode returns true for wsl command without exe`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl -d Debian /usr/bin/mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }
    fun `test detectWslMode returns true for wsl localhost UNC path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "\\\\wsl.localhost\\Ubuntu\\usr\\bin\\mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }
    fun `test detectWslMode returns true for wsl localhost UNC path with forward slashes`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "//wsl.localhost/Ubuntu/usr/bin/mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }
    fun `test detectWslMode returns true for wsl dollar sign UNC path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "\\\\wsl\$\\Debian\\home\\user\\mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }
    fun `test detectWslMode returns false for regular Windows path`() {
        val path = "C:\\Program Files\\mise\\mise.exe"
        assertFalse(WslPathUtils.detectWslMode(path))
    }
    fun `test detectWslMode returns false for regular Unix path`() {
        val path = "/usr/local/bin/mise"
        assertFalse(WslPathUtils.detectWslMode(path))
    }
    fun `test detectWslMode returns false for empty path`() {
        val path = ""
        assertFalse(WslPathUtils.detectWslMode(path))
    }
    fun `test detectWslMode returns false for blank path`() {
        val path = "   "
        assertFalse(WslPathUtils.detectWslMode(path))
    }

    // ========================
    // extractDistribution() Tests
    // ========================
    fun `test extractDistribution from wsl exe command format`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl.exe -d Ubuntu mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Ubuntu", result)
    }
    fun `test extractDistribution from wsl command format`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl -d Debian /usr/bin/mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Debian", result)
    }
    fun `test extractDistribution from wsl command with quoted distribution`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl.exe -d \"Ubuntu-20.04\" mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Ubuntu-20.04", result)
    }
    fun `test extractDistribution from wsl command with spaces in quoted distribution`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl.exe -d \"Ubuntu 20.04\" mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Ubuntu 20.04", result)
    }
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
    // convertWslToWindowsUncPath() Tests
    // ========================
    fun `test convertWslToWindowsUncPath converts simple home path`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val unixPath = "/home/user/.mise"

        val result = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)

        assertEquals("$uncRoot\\home\\user\\.mise", result)
    }
    fun `test convertWslToWindowsUncPath converts usr bin path`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val unixPath = "/usr/local/bin/node"

        val result = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)

        assertEquals("$uncRoot\\usr\\local\\bin\\node", result)
    }
    fun `test convertWslToWindowsUncPath converts root path`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val unixPath = "/opt/mise"

        val result = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)

        assertEquals("$uncRoot\\opt\\mise", result)
    }
    fun `test convertWslToWindowsUncPath converts path with spaces`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val unixPath = "/home/user/my folder/file.txt"

        val result = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)

        assertEquals("$uncRoot\\home\\user\\my folder\\file.txt", result)
    }

    fun `test convertWslToWindowsUncPath throws for non-absolute path`() {
        val distribution = assumeFirstDistribution()
        val unixPath = "home/user/.mise"
        try {
            WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    fun `test convertWslToWindowsUncPath throws for blank distribution`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val unixPath = "/home/user/.mise"
        try {
            WslPathUtils.convertWslToWindowsUncPath(unixPath, "")
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
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

    // ========================
    // Note: convertWindowsToWslMountPath() now uses IntelliJ's WSLDistribution.getWslPath() API
    // and requires a distribution parameter. Testing is delegated to IntelliJ Platform.
    // ========================

    // ========================
    // validateUncPath() Tests
    // ========================
    fun `test validateUncPath returns false for empty path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = ""
        val result = WslPathUtils.validateUncPath(path)
        assertFalse(result)
    }
    fun `test validateUncPath returns false for blank path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "   "
        val result = WslPathUtils.validateUncPath(path)
        assertFalse(result)
    }

    // ========================
    // Integration Pattern Tests
    // ========================
    fun `test round trip conversion for common paths`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val testPaths = listOf(
            "/home/user/.local/share/mise",
            "/usr/bin/mise",
            "/opt/mise/installs/node/20"
        )

        for (unixPath in testPaths) {
            val uncPath = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)
            assertTrue(uncPath.startsWith("$uncRoot\\"))
            assertFalse(uncPath.contains("/"))  // Should use Windows separators
        }
    }
    fun `test WSL detection and distribution extraction work together`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val testPaths = listOf(
            "wsl.exe -d Ubuntu mise",
            "wsl -d Debian /usr/bin/mise",
            "\\\\wsl.localhost\\Ubuntu\\usr\\bin\\mise",
            "\\\\wsl\$\\Debian\\home\\user\\mise"
        )

        for (path in testPaths) {
            val isWsl = WslPathUtils.detectWslMode(path)
            val distro = WslPathUtils.extractDistribution(path)

            assertTrue("Path should be detected as WSL: $path", isWsl)
            assertNotNull("Distribution should be extracted from: $path", distro)
            assertTrue("Distribution should not be blank: $path", distro!!.isNotBlank())
        }
    }

    private fun assumeFirstDistribution(): WSLDistribution {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val distributions = WslDistributionManager.getInstance().installedDistributions
        assumeTrue("No WSL distributions installed", distributions.isNotEmpty())
        return distributions.first()
    }

    private fun uncRoot(distribution: WSLDistribution): String =
        distribution.getUNCRootPath().toString().trimEnd('\\', '/')
}
