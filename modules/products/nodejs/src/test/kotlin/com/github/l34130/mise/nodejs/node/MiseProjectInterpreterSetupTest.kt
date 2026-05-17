package com.github.l34130.mise.nodejs.node

import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.testFramework.LightPlatformTestCase

class MiseProjectInterpreterSetupTest : LightPlatformTestCase() {
    fun `test creates NodeJsLocalInterpreter when no WSL distribution`() {
        val interpreter = MiseProjectInterpreterSetup.createNodeJsInterpreter(
            binPath = "/usr/local/bin/node",
            wslDistributionMsId = null,
        )
        assertTrue(
            "Expected NodeJsLocalInterpreter but was ${interpreter::class.simpleName}",
            interpreter is NodeJsLocalInterpreter,
        )
        assertEquals(
            "/usr/local/bin/node",
            (interpreter as NodeJsLocalInterpreter).interpreterSystemIndependentPath,
        )
    }
}
