package com.github.l34130.mise.core.wsl

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.openapi.util.SystemInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class WslPathUtilsTest {
    // ========================
    // detectWslMode() Tests
    // ========================

    @Test
    fun `detectWslMode returns true for wsl exe command`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl.exe -d Ubuntu mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }

    @Test
    fun `detectWslMode returns true for wsl command without exe`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl -d Debian /usr/bin/mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }

    @Test
    fun `detectWslMode returns true for wsl localhost UNC path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "\\\\wsl.localhost\\Ubuntu\\usr\\bin\\mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }

    @Test
    fun `detectWslMode returns true for wsl localhost UNC path with forward slashes`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "//wsl.localhost/Ubuntu/usr/bin/mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }

    @Test
    fun `detectWslMode returns true for wsl dollar sign UNC path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "\\\\wsl\$\\Debian\\home\\user\\mise"
        assertTrue(WslPathUtils.detectWslMode(path))
    }

    @Test
    fun `detectWslMode returns false for regular Windows path`() {
        val path = "C:\\Program Files\\mise\\mise.exe"
        assertFalse(WslPathUtils.detectWslMode(path))
    }

    @Test
    fun `detectWslMode returns false for regular Unix path`() {
        val path = "/usr/local/bin/mise"
        assertFalse(WslPathUtils.detectWslMode(path))
    }

    @Test
    fun `detectWslMode returns false for empty path`() {
        val path = ""
        assertFalse(WslPathUtils.detectWslMode(path))
    }

    @Test
    fun `detectWslMode returns false for blank path`() {
        val path = "   "
        assertFalse(WslPathUtils.detectWslMode(path))
    }

    // ========================
    // extractDistribution() Tests
    // ========================

    @Test
    fun `extractDistribution from wsl exe command format`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl.exe -d Ubuntu mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Ubuntu", result)
    }

    @Test
    fun `extractDistribution from wsl command format`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl -d Debian /usr/bin/mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Debian", result)
    }

    @Test
    fun `extractDistribution from wsl command with quoted distribution`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl.exe -d \"Ubuntu-20.04\" mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Ubuntu-20.04", result)
    }

    @Test
    fun `extractDistribution from wsl command with spaces in quoted distribution`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "wsl.exe -d \"Ubuntu 20.04\" mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Ubuntu 20.04", result)
    }

    @Test
    fun `extractDistribution from wsl localhost UNC path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "\\\\wsl.localhost\\Ubuntu\\usr\\bin\\mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Ubuntu", result)
    }

    @Test
    fun `extractDistribution from wsl dollar sign UNC path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "\\\\wsl\$\\Debian\\home\\user\\mise"
        val result = WslPathUtils.extractDistribution(path)
        assertEquals("Debian", result)
    }

    @Test
    fun `extractDistribution returns null for non-WSL path`() {
        val path = "C:\\Program Files\\mise\\mise.exe"
        val result = WslPathUtils.extractDistribution(path)
        assertNull(result)
    }

    @Test
    fun `extractDistribution returns null for empty path`() {
        val path = ""
        val result = WslPathUtils.extractDistribution(path)
        assertNull(result)
    }

    // ========================
    // convertWslToWindowsUncPath() Tests
    // ========================

    @Test
    fun `convertWslToWindowsUncPath converts simple home path`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val unixPath = "/home/user/.mise"

        val result = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)

        assertEquals("$uncRoot\\home\\user\\.mise", result)
    }

    @Test
    fun `convertWslToWindowsUncPath converts usr bin path`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val unixPath = "/usr/local/bin/node"

        val result = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)

        assertEquals("$uncRoot\\usr\\local\\bin\\node", result)
    }

    @Test
    fun `convertWslToWindowsUncPath converts root path`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val unixPath = "/opt/mise"

        val result = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)

        assertEquals("$uncRoot\\opt\\mise", result)
    }

    @Test
    fun `convertWslToWindowsUncPath converts path with spaces`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val unixPath = "/home/user/my folder/file.txt"

        val result = WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)

        assertEquals("$uncRoot\\home\\user\\my folder\\file.txt", result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `convertWslToWindowsUncPath throws for non-absolute path`() {
        val distribution = assumeFirstDistribution()
        val unixPath = "home/user/.mise"
        WslPathUtils.convertWslToWindowsUncPath(unixPath, distribution.msId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `convertWslToWindowsUncPath throws for blank distribution`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val unixPath = "/home/user/.mise"
        WslPathUtils.convertWslToWindowsUncPath(unixPath, "")
    }

    // ========================
    // convertWindowsUncToUnixPath() Tests
    // ========================

    @Test
    fun `convertWindowsUncToUnixPath converts wsl localhost path with backslashes`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val uncPath = "$uncRoot\\home\\user\\.mise"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/home/user/.mise", result)
    }

    @Test
    fun `convertWindowsUncToUnixPath converts wsl localhost path with forward slashes`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution).replace('\\', '/')
        val uncPath = "$uncRoot/usr/bin/node"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/usr/bin/node", result)
    }

    @Test
    fun `convertWindowsUncToUnixPath handles path with spaces`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val uncPath = "$uncRoot\\home\\user\\my folder\\file.txt"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/home/user/my folder/file.txt", result)
    }

    @Test
    fun `convertWindowsUncToUnixPath handles mixed slashes`() {
        val distribution = assumeFirstDistribution()
        val uncRoot = uncRoot(distribution)
        val mixedRoot = uncRoot.replaceFirst("\\\\", "//")
        val uncPath = "$mixedRoot/home/testuser\\src\\github"

        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)

        assertEquals("/home/testuser/src/github", result)
    }

    @Test
    fun `convertWindowsUncToUnixPath returns null for non-UNC path`() {
        val uncPath = "C:\\Users\\project"
        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)
        assertNull(result)
    }

    @Test
    fun `convertWindowsUncToUnixPath returns null for empty path`() {
        val uncPath = ""
        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)
        assertNull(result)
    }

    @Test
    fun `convertWindowsUncToUnixPath returns null for blank path`() {
        val uncPath = "   "
        val result = WslPathUtils.convertWindowsUncToUnixPath(uncPath)
        assertNull(result)
    }

    @Test
    fun `convertWindowsUncToUnixPath handles root path`() {
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

    @Test
    fun `validateUncPath returns false for empty path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = ""
        val result = WslPathUtils.validateUncPath(path)
        assertFalse(result)
    }

    @Test
    fun `validateUncPath returns false for blank path`() {
        assumeTrue("WSL path functions require Windows", SystemInfo.isWindows)
        val path = "   "
        val result = WslPathUtils.validateUncPath(path)
        assertFalse(result)
    }

    // Note: Testing actual UNC path validation requires WSL to be running
    // and is better suited for integration tests rather than unit tests

    // ========================
    // WslMiseInstallation Data Class Tests
    // ========================

    @Test
    fun `WslMiseInstallation data class creates correctly`() {
        val installation = WslMiseInstallation("Ubuntu", "/usr/bin/mise")
        assertEquals("Ubuntu", installation.distribution)
        assertEquals("/usr/bin/mise", installation.path)
    }

    @Test
    fun `WslMiseInstallation data class equality works`() {
        val installation1 = WslMiseInstallation("Ubuntu", "/usr/bin/mise")
        val installation2 = WslMiseInstallation("Ubuntu", "/usr/bin/mise")
        assertEquals(installation1, installation2)
    }

    @Test
    fun `WslMiseInstallation data class toString works`() {
        val installation = WslMiseInstallation("Ubuntu", "/usr/bin/mise")
        val string = installation.toString()
        assertTrue(string.contains("Ubuntu"))
        assertTrue(string.contains("/usr/bin/mise"))
    }

    // ========================
    // Integration Pattern Tests
    // ========================

    @Test
    fun `round trip conversion for common paths`() {
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

    @Test
    fun `WSL detection and distribution extraction work together`() {
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
