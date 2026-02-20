package com.github.l34130.mise.core

import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.assertInstanceOf
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CancellationException

class MiseEnvCustomizerTest : BasePlatformTestCase() {

    // A lightweight subclass that allows us to trigger exceptions synchronously
    // without waking up the IntelliJ command cache or pooled threads.
    private class TestCustomizer(
        private val throwException: Exception? = null
    ) : MiseEnvCustomizer {
        override val logger = Logger.getInstance(TestCustomizer::class.java)

        override fun shouldCustomizeForSettings(settings: MiseProjectSettings.MyState): Boolean {
            // We throw the exception here so it triggers the catch block in the default method
            // BEFORE any background threads, caching, or execution begins!
            if (throwException != null) {
                throw throwException
            }
            return true
        }
    }

    fun `test ProcessCanceledException bubbles up gracefully`() {
        val customizer = TestCustomizer(ProcessCanceledException())
        val env = mutableMapOf<String?, String?>()

        try {
            customizer.customizeMiseEnvironment(project, "dummy", env)
            fail("Expected ProcessCanceledException to bubble up, but customization completed successfully.")
        } catch (e: ProcessCanceledException) {
            assertInstanceOf<ProcessCanceledException>(e)
        } catch (e: Exception) {
            fail("Expected ProcessCanceledException, but got ${e::class.simpleName}: ${e.message}")
        }
    }

    fun `test Coroutine CancellationException bubbles up gracefully`() {
        val customizer = TestCustomizer(CancellationException("Job cancelled"))
        val env = mutableMapOf<String?, String?>()

        try {
            customizer.customizeMiseEnvironment(project, "dummy", env)
            fail("Expected CancellationException to bubble up, but customization completed successfully.")
        } catch (e: CancellationException) {
            assertInstanceOf<CancellationException>(e)
        } catch (e: Exception) {
            fail("Expected CancellationException, but got ${e::class.simpleName}: ${e.message}")
        }
    }

    fun `test standard exceptions are caught and swallowed by customizer`() {
        val expectedErrorMessage = "A generic failure"
        val customizer = TestCustomizer(RuntimeException(expectedErrorMessage))
        val env = mutableMapOf<String?, String?>()

        // We expect NO exceptions to escape. It should catch the RuntimeException,
        // log it, and return false.
        var result = true

        // This helper catches ANY error logged via logger.error() and returns it
        // instead of crashing the test suite!
        val loggedError = LoggedErrorProcessor.executeAndReturnLoggedError {
            result = try {
                customizer.customizeMiseEnvironment(project, "dummy", env)
            } catch (e: Exception) {
                fail("Expected standard exceptions to be swallowed, but ${e::class.simpleName} escaped!")
                return@executeAndReturnLoggedError
            }
        }

        // Assert that the customizer returned false
        assertFalse("Customization should return false on standard exception", result)
        // Assert that the environment was not modified
        assertTrue("Environment should remain empty", env.isEmpty())

        // Optionally, assert that it logged exactly what we expected it to log
        assertEquals("Expected failure message in log", expectedErrorMessage, loggedError.message)
    }
}
