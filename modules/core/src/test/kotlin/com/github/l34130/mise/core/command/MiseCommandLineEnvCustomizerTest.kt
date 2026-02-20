package com.github.l34130.mise.core.command

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MiseCommandLineEnvCustomizerTest : BasePlatformTestCase() {

    fun `test environment customization avoids deadlock when write action is active`() {
        val customizer = MiseCommandLineEnvCustomizer()

        // 1. Set up a valid dummy command line that will pass the initial early-exit checks
        val commandLine = GeneralCommandLine("dummy-executable")
        commandLine.withWorkDirectory(project.basePath) // Must have a valid work dir to resolve the project

        val env = mutableMapOf<String, String>()

        val latch = CountDownLatch(1)
        var backgroundThreadCompleted = false

        // 2. Start a Write Action on the main Event Dispatch Thread (EDT)
        ApplicationManager.getApplication().runWriteAction {

            // 3. While holding the Write Action, spawn a background thread that tries to customize the environment
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    // This calls resolveProjectFromCommandLine under the hood.
                    // OLD CODE: Blocks forever waiting for a Read Action.
                    // NEW CODE: tryComputeReadAction returns null, customization bails out safely.
                    customizer.customizeEnv(commandLine, env)

                    backgroundThreadCompleted = true
                } finally {
                    latch.countDown()
                }
            }

            // 4. Wait for the background thread to finish.
            // We use a latch with a timeout so the test fails cleanly instead of freezing the entire test suite.
            val success = latch.await(3, TimeUnit.SECONDS)

            // 5. Assertions
            assertTrue(
                "Background thread deadlocked waiting for a Read Action!",
                success
            )
            assertTrue(
                "Customizer should have finished executing without errors",
                backgroundThreadCompleted
            )
        }
    }
}
