package com.github.l34130.mise.nodejs.node

import com.intellij.javascript.nodejs.interpreter.wsl.WslNodeInterpreter
import com.intellij.testFramework.LightPlatformTestCase

/**
 * WSL-only tests for [MiseProjectInterpreterSetup].
 *
 * Excluded on non-Windows hosts via the module's `build.gradle.kts`, because
 * IntelliJ's WSL services are only wired up on Windows.
 */
class MiseProjectInterpreterSetupWslTest : LightPlatformTestCase() {
    fun `test creates WslNodeInterpreter with unix path when distribution is set`() {
        val uncPath = "\\\\wsl.localhost\\Ubuntu\\home\\user\\.local\\share\\mise\\installs\\node\\22.0.0\\bin\\node"
        val interpreter = MiseProjectInterpreterSetup.createNodeJsInterpreter(
            binPath = uncPath,
            wslDistributionMsId = "Ubuntu",
        )
        assertTrue(
            "Expected WslNodeInterpreter but was ${interpreter::class.simpleName}",
            interpreter is WslNodeInterpreter,
        )
        interpreter as WslNodeInterpreter
        assertEquals("Ubuntu", interpreter.wslDistributionId)
        // The buggy behavior was returning a path like C:/home/user/... or a raw UNC path;
        // IntelliJ expects the WSL-internal unix path so it can construct a wsl:// reference.
        assertEquals(
            "/home/user/.local/share/mise/installs/node/22.0.0/bin/node",
            interpreter.wslInterpreterPath,
        )
    }
}
