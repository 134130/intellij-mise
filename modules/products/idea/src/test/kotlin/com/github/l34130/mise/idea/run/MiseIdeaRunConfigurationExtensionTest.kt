package com.github.l34130.mise.idea.run

import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.OutputStream

class MiseIdeaRunConfigurationExtensionTest : BasePlatformTestCase() {
    private val extension = TestExtension()
    private lateinit var originalEnvProvider: (RunConfigurationBase<*>, String?) -> Map<String, String>

    override fun setUp() {
        super.setUp()
        originalEnvProvider = envProvider
        envProvider = { _, _ ->
            mapOf(
                "MISE_TEST_ENV" to "from-mise",
                "EXISTING_ENV" to "from-mise",
            )
        }
        MiseIdeaRunConfigurationEnvironmentStore.clear()
    }

    override fun tearDown() {
        try {
            envProvider = originalEnvProvider
            MiseIdeaRunConfigurationEnvironmentStore.clear()
        } finally {
            super.tearDown()
        }
    }

    fun `test Java parameters receive mise environment`() {
        val configuration = ApplicationConfiguration("main", project)
        val params =
            JavaParameters().apply {
                workingDirectory = project.basePath
                env = mapOf("EXISTING_ENV" to "original")
            }

        extension.updateJavaParameters(configuration, params, null)

        assertEquals("from-mise", params.env["MISE_TEST_ENV"])
        assertEquals("from-mise", params.env["EXISTING_ENV"])
    }

    fun `test external system environment is restored after process termination`() {
        val configuration = externalSystemRunConfiguration()
        configuration.settings.env = mapOf("EXISTING_ENV" to "original")
        val params = javaParameters()
        val handler = TestProcessHandler()

        extension.updateJavaParameters(configuration, params, null)
        extension.attachToProcessForTest(configuration, handler)
        handler.startNotify()

        assertEquals("from-mise", configuration.settings.env["MISE_TEST_ENV"])
        assertEquals("from-mise", configuration.settings.env["EXISTING_ENV"])
        assertTrue(MiseIdeaRunConfigurationEnvironmentStore.hasSnapshot(configuration))

        handler.terminate()

        assertEquals(mapOf("EXISTING_ENV" to "original"), configuration.settings.env)
        assertFalse(MiseIdeaRunConfigurationEnvironmentStore.hasSnapshot(configuration))
    }

    fun `test external system environment does not leak between consecutive runs`() {
        val configuration = externalSystemRunConfiguration()
        configuration.settings.env = mapOf("EXISTING_ENV" to "original")

        repeat(2) {
            val handler = TestProcessHandler()
            extension.updateJavaParameters(configuration, javaParameters(), null)
            extension.attachToProcessForTest(configuration, handler)
            handler.startNotify()

            assertEquals("from-mise", configuration.settings.env["MISE_TEST_ENV"])

            handler.terminate()

            assertEquals(mapOf("EXISTING_ENV" to "original"), configuration.settings.env)
            assertFalse(MiseIdeaRunConfigurationEnvironmentStore.hasSnapshot(configuration))
        }
    }

    fun `test external system environment can be restored when process never starts`() {
        val configuration = externalSystemRunConfiguration()
        configuration.settings.env = mapOf("EXISTING_ENV" to "original")

        extension.updateJavaParameters(configuration, javaParameters(), null)

        assertEquals("from-mise", configuration.settings.env["MISE_TEST_ENV"])

        MiseIdeaRunConfigurationEnvironmentStore.restore(configuration)

        assertEquals(mapOf("EXISTING_ENV" to "original"), configuration.settings.env)
        assertFalse(MiseIdeaRunConfigurationEnvironmentStore.hasSnapshot(configuration))
    }

    private fun javaParameters(): JavaParameters =
        JavaParameters().apply {
            workingDirectory = project.basePath
            env = mapOf("EXISTING_ENV" to "original")
        }

    private fun externalSystemRunConfiguration(): ExternalSystemRunConfiguration {
        val type = TestExternalSystemTaskConfigurationType()
        return ExternalSystemRunConfiguration(TEST_SYSTEM_ID, project, type.factory, "external").apply {
            settings.externalProjectPath = project.basePath
        }
    }

    private class TestExternalSystemTaskConfigurationType : AbstractExternalSystemTaskConfigurationType(TEST_SYSTEM_ID)

    private class TestExtension : MiseIdeaRunConfigurationExtension() {
        fun attachToProcessForTest(
            configuration: RunConfigurationBase<*>,
            handler: ProcessHandler,
        ) {
            attachToProcess(configuration, handler, null)
        }
    }

    private class TestProcessHandler : ProcessHandler() {
        fun terminate() {
            notifyProcessTerminated(0)
        }

        override fun destroyProcessImpl() {
            terminate()
        }

        override fun detachProcessImpl() {
            notifyProcessDetached()
        }

        override fun detachIsDefault(): Boolean = false

        override fun getProcessInput(): OutputStream? = null
    }

    private companion object {
        val TEST_SYSTEM_ID = ProjectSystemId("MISE_TEST")
    }
}
