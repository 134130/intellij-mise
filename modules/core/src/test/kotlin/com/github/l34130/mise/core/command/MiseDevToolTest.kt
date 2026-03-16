package com.github.l34130.mise.core.command

import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiseDevToolTest : LightPlatformTestCase() {

    // ---- resolvedVersion -------------------------------------------------------

    @Test
    fun `resolvedVersion returns version when non-blank`() {
        assertEquals("1.2.3", tool(version = "1.2.3").resolvedVersion)
    }

    @Test
    fun `resolvedVersion returns actual installed version not the constraint`() {
        assertEquals("1.2.3", tool(version = "1.2.3", requestedVersion = "1.2").resolvedVersion)
    }

    @Test
    fun `resolvedVersion falls back to requestedVersion when version is blank`() {
        assertEquals("latest", tool(version = "", requestedVersion = "latest").resolvedVersion)
    }

    @Test
    fun `resolvedVersion is empty when both version and requestedVersion are blank`() {
        assertEquals("", tool(version = "", requestedVersion = "").resolvedVersion)
    }

    @Test
    fun `resolvedVersion is empty when version is blank and requestedVersion is null`() {
        assertEquals("", tool(version = "", requestedVersion = null).resolvedVersion)
    }

    // ---- displayVersion --------------------------------------------------------

    @Test
    fun `displayVersion returns requestedVersion when present`() {
        assertEquals("1.2", tool(version = "1.2.3", requestedVersion = "1.2").displayVersion)
    }

    @Test
    fun `displayVersion returns requestedVersion for floating constraint`() {
        assertEquals("latest", tool(version = "4.0.1", requestedVersion = "latest").displayVersion)
    }

    @Test
    fun `displayVersion falls back to resolvedVersion when requestedVersion is null`() {
        assertEquals("1.2.3", tool(version = "1.2.3", requestedVersion = null).displayVersion)
    }

    @Test
    fun `displayVersion falls back to resolvedVersion when requestedVersion is blank`() {
        assertEquals("1.2.3", tool(version = "1.2.3", requestedVersion = "").displayVersion)
    }

    // ---- displayVersionWithResolved --------------------------------------------

    @Test
    fun `displayVersionWithResolved is version when no requestedVersion`() {
        assertEquals("1.2.3", tool(version = "1.2.3", requestedVersion = null).displayVersionWithResolved)
    }

    @Test
    fun `displayVersionWithResolved is just requested when version is blank`() {
        assertEquals("1.2", tool(version = "", requestedVersion = "1.2").displayVersionWithResolved)
    }

    @Test
    fun `displayVersionWithResolved is just requested when they are equal`() {
        assertEquals("1.2.3", tool(version = "1.2.3", requestedVersion = "1.2.3").displayVersionWithResolved)
    }

    @Test
    fun `displayVersionWithResolved appends resolved in parens for floating constraint`() {
        assertEquals("latest (1.2.3)", tool(version = "1.2.3", requestedVersion = "latest").displayVersionWithResolved)
    }

    @Test
    fun `displayVersionWithResolved appends resolved in parens for partial constraint`() {
        assertEquals("21 (21.0.9-b895.149)", tool(version = "21.0.9-b895.149", requestedVersion = "21").displayVersionWithResolved)
    }

    // ---- resolvedInstallPath ---------------------------------------------------

    @Test
    fun `resolvedInstallPath returns installPath when wslDistribution is null`() {
        val tool = tool()
        assertNull(tool.wslDistributionMsId)
        assertEquals("/home/user/.local/share/mise/installs/node/22.0.0", tool.resolvedInstallPath)
    }

    // ---- helpers ---------------------------------------------------------------

    private fun tool(
        version: String = "22.0.0",
        requestedVersion: String? = null,
        installPath: String = "/home/user/.local/share/mise/installs/node/22.0.0",
    ) = MiseDevTool(
        version = version,
        requestedVersion = requestedVersion,
        installPath = installPath,
        installed = true,
        active = true,
        wslDistributionMsId = null,
    )
}
