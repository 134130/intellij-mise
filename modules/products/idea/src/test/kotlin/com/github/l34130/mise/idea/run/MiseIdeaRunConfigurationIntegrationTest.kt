package com.github.l34130.mise.idea.run

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.markProjectCacheReady
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.absolutePathString

class MiseIdeaRunConfigurationIntegrationTest : BasePlatformTestCase() {
    private val extension = TestExtension()
    private lateinit var originalExecutablePath: String
    private lateinit var projectDir: Path

    override fun setUp() {
        super.setUp()

        val settings = project.service<MiseProjectSettings>().state
        originalExecutablePath = settings.executablePath
        projectDir = Files.createTempDirectory("mise-idea-run-integration")
        settings.executablePath = createFakeMise(projectDir).absolutePathString()
        settings.useMiseDirEnv = true
        settings.useMiseInRunConfigurations = true

        project.markProjectCacheReady()
        project.service<MiseCacheService>().invalidateAllCommands()
        project.service<MiseCacheService>().invalidateAllExecutables()
        MiseIdeaRunConfigurationEnvironmentStore.clear()
    }

    override fun tearDown() {
        try {
            project.service<MiseProjectSettings>().state.executablePath = originalExecutablePath
            project.service<MiseCacheService>().invalidateAllCommands()
            project.service<MiseCacheService>().invalidateAllExecutables()
            MiseIdeaRunConfigurationEnvironmentStore.clear()
        } finally {
            super.tearDown()
        }
    }

    fun `test application configuration receives mise env from fake executable`() {
        val configuration = ApplicationConfiguration("main", project)
        val params =
            JavaParameters().apply {
                workingDirectory = projectDir.absolutePathString()
                env = mapOf("EXISTING_ENV" to "original")
            }

        extension.updateJavaParameters(configuration, params, null)

        assertEquals("true", params.env["MISE_KOTLIN"])
        assertEquals("idea", params.env["MISE_INTEGRATION"])
        assertEquals("from-mise", params.env["EXISTING_ENV"])
    }

    fun `test external system configuration receives and restores mise env from fake executable`() {
        val configuration = externalSystemRunConfiguration()
        configuration.settings.env = mapOf("EXISTING_ENV" to "original")
        val params =
            JavaParameters().apply {
                env = mapOf("PARAM_ENV" to "original")
            }
        val handler = TestProcessHandler()

        extension.updateJavaParameters(configuration, params, null)
        extension.attachToProcessForTest(configuration, handler)
        handler.startNotify()

        assertEquals("true", params.env["MISE_KOTLIN"])
        assertEquals("idea", params.env["MISE_INTEGRATION"])
        assertEquals("true", configuration.settings.env["MISE_KOTLIN"])
        assertEquals("idea", configuration.settings.env["MISE_INTEGRATION"])
        assertEquals("from-mise", configuration.settings.env["EXISTING_ENV"])

        handler.terminate()

        assertEquals(mapOf("EXISTING_ENV" to "original"), configuration.settings.env)
        assertFalse(MiseIdeaRunConfigurationEnvironmentStore.hasSnapshot(configuration))
    }

    fun `test external system configuration restores env between repeated runs`() {
        val configuration = externalSystemRunConfiguration()
        configuration.settings.env = mapOf("EXISTING_ENV" to "original")

        repeat(2) {
            val handler = TestProcessHandler()

            extension.updateJavaParameters(configuration, externalSystemJavaParameters(), null)
            extension.attachToProcessForTest(configuration, handler)
            handler.startNotify()

            assertEquals("true", configuration.settings.env["MISE_KOTLIN"])

            handler.terminate()

            assertEquals(mapOf("EXISTING_ENV" to "original"), configuration.settings.env)
            assertFalse(MiseIdeaRunConfigurationEnvironmentStore.hasSnapshot(configuration))
        }
    }

    fun `test processNotStarted restores external system env`() {
        val configuration = externalSystemRunConfiguration()
        configuration.settings.env = mapOf("EXISTING_ENV" to "original")

        extension.updateJavaParameters(configuration, externalSystemJavaParameters(), null)

        assertEquals("true", configuration.settings.env["MISE_KOTLIN"])

        val environment =
            ExecutionEnvironmentBuilder(project, DefaultRunExecutor.getRunExecutorInstance())
                .runProfile(configuration)
                .build()
        MiseIdeaExecutionListener().processNotStarted(DefaultRunExecutor.EXECUTOR_ID, environment)

        assertEquals(mapOf("EXISTING_ENV" to "original"), configuration.settings.env)
        assertFalse(MiseIdeaRunConfigurationEnvironmentStore.hasSnapshot(configuration))
    }

    private fun externalSystemJavaParameters(): JavaParameters =
        JavaParameters().apply {
            env = mapOf("PARAM_ENV" to "original")
        }

    private fun externalSystemRunConfiguration(): ExternalSystemRunConfiguration {
        val type = TestExternalSystemTaskConfigurationType()
        return ExternalSystemRunConfiguration(TEST_SYSTEM_ID, project, type.factory, "external").apply {
            settings.externalProjectPath = projectDir.absolutePathString()
        }
    }

    private fun createFakeMise(directory: Path): Path {
        val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
        val fakeMise = directory.resolve(if (isWindows) "mise.bat" else "mise")
        val script =
            if (isWindows) {
                """
                @echo off
                if "%1"=="version" (
                  echo MISE_BIN: %~f0
                  echo 2025.1.0
                  exit /b 0
                )
                if "%1"=="env" (
                  echo {"MISE_KOTLIN":"true","MISE_INTEGRATION":"idea","EXISTING_ENV":"from-mise"}
                  exit /b 0
                )
                echo unexpected mise args: %* 1>&2
                exit /b 1
                """.trimIndent()
            } else {
                """
                #!/usr/bin/env sh
                if [ "${'$'}1" = "version" ]; then
                  echo "MISE_BIN: ${'$'}0"
                  echo "2025.1.0"
                  exit 0
                fi
                if [ "${'$'}1" = "env" ]; then
                  echo '{"MISE_KOTLIN":"true","MISE_INTEGRATION":"idea","EXISTING_ENV":"from-mise"}'
                  exit 0
                fi
                echo "unexpected mise args: ${'$'}*" >&2
                exit 1
                """.trimIndent()
            }

        Files.writeString(fakeMise, script)
        if (!isWindows) {
            Files.setPosixFilePermissions(fakeMise, PosixFilePermissions.fromString("rwxr-xr-x"))
        }
        return fakeMise
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
