package com.github.l34130.mise.core.command

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MiseDevToolTest {
    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun `shimsInstallPath returns installPath when requestedVersion is null`() {
        val tool = MiseDevTool(
            version = "22.22.0",
            requestedVersion = null,
            installPath = "/home/user/.local/share/mise/installs/node/22.22.0",
            installed = true,
            active = true,
        )
        assert(tool.shimsInstallPath() == "/home/user/.local/share/mise/installs/node/22.22.0")
    }

    @Test
    fun `shimsInstallPath replaces version with requestedVersion`() {
        val tool = MiseDevTool(
            version = "22.22.0",
            requestedVersion = "22",
            installPath = "/home/user/.local/share/mise/installs/node/22.22.0",
            installed = true,
            active = true,
        )
        // When path doesn't exist on filesystem, returns the computed shims path
        assert(tool.shimsInstallPath().endsWith("/mise/installs/node/22"))
    }

    @Test(expected = IllegalStateException::class)
    fun `shimsInstallPath throws when version not found in path`() {
        val tool = MiseDevTool(
            version = "22.22.0",
            requestedVersion = "22",
            installPath = "/some/unexpected/path",
            installed = true,
            active = true,
        )
        tool.shimsInstallPath()
    }

    @Test
    fun `resolveShortcutFile resolves file containing relative path`() {
        // Simulate Windows mise shortcut: a file "22" containing ".\22.22.0"
        val installDir = tempFolder.newFolder("installs", "node")
        val actualDir = File(installDir, "22.22.0")
        actualDir.mkdir()

        val shortcutFile = File(installDir, "22")
        shortcutFile.writeText(".${File.separator}22.22.0")

        val resolved = MiseDevTool.resolveShortcutFile(shortcutFile.absolutePath)
        assert(resolved == actualDir.canonicalPath) {
            "Expected ${actualDir.canonicalPath} but got $resolved"
        }
    }

    @Test
    fun `resolveShortcutFile resolves file with forward slash relative path`() {
        // Simulate shortcut file containing "./22.22.0"
        val installDir = tempFolder.newFolder("installs", "node")
        val actualDir = File(installDir, "22.22.0")
        actualDir.mkdir()

        val shortcutFile = File(installDir, "22")
        shortcutFile.writeText("./22.22.0")

        val resolved = MiseDevTool.resolveShortcutFile(shortcutFile.absolutePath)
        assert(resolved == actualDir.canonicalPath) {
            "Expected ${actualDir.canonicalPath} but got $resolved"
        }
    }

    @Test
    fun `resolveShortcutFile returns original path for directories`() {
        val installDir = tempFolder.newFolder("installs", "node", "22.22.0")
        val resolved = MiseDevTool.resolveShortcutFile(installDir.absolutePath)
        assert(resolved == installDir.absolutePath) {
            "Expected ${installDir.absolutePath} but got $resolved"
        }
    }

    @Test
    fun `resolveShortcutFile returns original path for non-existent paths`() {
        val nonExistent = "/non/existent/path"
        val resolved = MiseDevTool.resolveShortcutFile(nonExistent)
        assert(resolved == nonExistent) {
            "Expected $nonExistent but got $resolved"
        }
    }

    @Test
    fun `resolveShortcutFile returns original path when target does not exist`() {
        val installDir = tempFolder.newFolder("installs", "node")
        val shortcutFile = File(installDir, "22")
        shortcutFile.writeText("./nonexistent")

        val resolved = MiseDevTool.resolveShortcutFile(shortcutFile.absolutePath)
        assert(resolved == shortcutFile.absolutePath) {
            "Expected ${shortcutFile.absolutePath} but got $resolved"
        }
    }

    @Test
    fun `shimsInstallPath resolves shortcut file on filesystem`() {
        // Simulate the full flow: installPath points to actual dir, requestedVersion creates shortcut
        val installDir = tempFolder.newFolder("installs", "node")
        val actualDir = File(installDir, "22.22.0")
        actualDir.mkdir()

        val shortcutFile = File(installDir, "22")
        shortcutFile.writeText("./22.22.0")

        val tool = MiseDevTool(
            version = "22.22.0",
            requestedVersion = "22",
            installPath = actualDir.absolutePath,
            installed = true,
            active = true,
        )

        val result = tool.shimsInstallPath()
        assert(result == actualDir.canonicalPath) {
            "Expected ${actualDir.canonicalPath} but got $result"
        }
    }

    @Test
    fun `resolveShortcutFile handles file with whitespace in content`() {
        val installDir = tempFolder.newFolder("installs", "node")
        val actualDir = File(installDir, "22.22.0")
        actualDir.mkdir()

        val shortcutFile = File(installDir, "22")
        shortcutFile.writeText("  ./22.22.0  \n")

        val resolved = MiseDevTool.resolveShortcutFile(shortcutFile.absolutePath)
        assert(resolved == actualDir.canonicalPath) {
            "Expected ${actualDir.canonicalPath} but got $resolved"
        }
    }
}
