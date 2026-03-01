package com.github.l34130.mise.core.command

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class MiseDevToolTest {
    @JvmField
    @Rule
    val tempFolder = TemporaryFolder()

    private fun tool(
        version: String,
        requestedVersion: String?,
        installPath: String,
    ) = MiseDevTool(
        version = version,
        requestedVersion = requestedVersion,
        installPath = installPath,
        installed = true,
        active = true,
    )

    @Test
    fun `resolvedInstallPath returns shimsInstallPath when no requestedVersion`() {
        val dir = tempFolder.newFolder("22.22.0")
        val t = tool(version = "22.22.0", requestedVersion = null, installPath = dir.absolutePath)
        assertEquals(dir.absolutePath, t.resolvedInstallPath())
    }

    @Test
    fun `resolvedInstallPath returns shimsInstallPath when path is a directory`() {
        val base = tempFolder.newFolder("node")
        val actualDir = base.resolve("22.22.0").also { it.mkdir() }
        // Create base/22 as a directory (not an alias file)
        base.resolve("22").mkdir()
        val t = tool(version = "22.22.0", requestedVersion = "22", installPath = actualDir.absolutePath)
        // shimsInstallPath() = base/22, which is a directory → returns it unchanged
        val expected = base.resolve("22").absolutePath
        assertEquals(expected, t.resolvedInstallPath())
    }

    @Test
    fun `resolvedInstallPath resolves alias file to actual directory`() {
        val base = tempFolder.newFolder("node")
        val actualDir = base.resolve("22.22.0").also { it.mkdir() }
        // Simulate mise alias file: base/22 is a text file containing "./22.22.0"
        base.resolve("22").also { it.writeText("./22.22.0") }
        val t = tool(version = "22.22.0", requestedVersion = "22", installPath = actualDir.absolutePath)
        // resolvedInstallPath should resolve base/22 (alias file) → base/22.22.0
        assertEquals(actualDir.canonicalPath, t.resolvedInstallPath())
    }
}
