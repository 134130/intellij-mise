package com.github.l34130.mise.core.command

import org.junit.Assert.assertEquals
import org.junit.Test

class MiseDevToolTest {
    @Test
    fun `shimsInstallPath returns installPath`() {
        val tool = MiseDevTool(
            version = "22.17.1",
            requestedVersion = null,
            installPath = "/Users/user/.local/share/mise/installs/node/22.17.1",
            installed = true,
            active = true,
        )

        assertEquals("/Users/user/.local/share/mise/installs/node/22.17.1", tool.shimsInstallPath())
    }

    @Test
    fun `shimsInstallPath returns installPath when requestedVersion differs from version`() {
        val tool = MiseDevTool(
            version = "22.17.1",
            requestedVersion = "22.17.0",
            installPath = "/Users/user/.local/share/mise/installs/node/22.17.1",
            installed = true,
            active = true,
        )

        // Should return installPath as-is since mise provides the correct actual installation path
        assertEquals("/Users/user/.local/share/mise/installs/node/22.17.1", tool.shimsInstallPath())
    }

    @Test
    fun `shimsInstallPath returns installPath for aliased tool`() {
        // This simulates an aliased tool like "21" -> "corretto-21.0.8.9.1"
        val tool = MiseDevTool(
            version = "corretto-21.0.8.9.1",
            requestedVersion = "21",
            installPath = "/Users/user/.local/share/mise/installs/java/corretto-21.0.8.9.1",
            installed = true,
            active = true,
        )

        // Should return the installPath as-is since that's where the tool is actually installed
        assertEquals("/Users/user/.local/share/mise/installs/java/corretto-21.0.8.9.1", tool.shimsInstallPath())
    }

    @Test
    fun `shimsVersion returns requestedVersion when available`() {
        val tool = MiseDevTool(
            version = "corretto-21.0.8.9.1",
            requestedVersion = "21",
            installPath = "/Users/user/.local/share/mise/installs/java/corretto-21.0.8.9.1",
            installed = true,
            active = true,
        )

        assertEquals("21", tool.shimsVersion())
    }

    @Test
    fun `shimsVersion returns version when requestedVersion is null`() {
        val tool = MiseDevTool(
            version = "22.17.1",
            requestedVersion = null,
            installPath = "/Users/user/.local/share/mise/installs/node/22.17.1",
            installed = true,
            active = true,
        )

        assertEquals("22.17.1", tool.shimsVersion())
    }
}
