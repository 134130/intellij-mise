@file:Suppress("ktlint")
package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.FileTestBase
import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.command.MiseExecutableInfo
import com.github.l34130.mise.core.command.MiseExecutableManager
import com.github.l34130.mise.core.execution.configuration.MiseTomlTaskRunConfiguration
import com.github.l34130.mise.core.execution.configuration.MiseTomlTaskRunConfigurationFactory
import com.github.l34130.mise.core.execution.configuration.MiseTomlTaskRunConfigurationProducer
import com.github.l34130.mise.core.execution.configuration.MiseTomlTaskRunConfigurationType
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlFile

class MiseTomlTaskRunConfigurationProducerTest : FileTestBase() {
    fun `test configuration producer with chained table format`() {
        @Language("TOML")
        val tomlText = """
            [tasks.foo]
            #^
            run = "echo foo"
        """.trimIndent()

        inlineFile(tomlText, "mise.toml") as TomlFile
        val element = findElementInEditor<PsiElement>()

        val context = createConfigurationContext(element)
        val producer = MiseTomlTaskRunConfigurationProducer()

        val configurationFromContext = producer.findOrCreateConfigurationFromContext(context)
        assertNotNull("Configuration should be created from context", configurationFromContext)
        assertEquals("Run foo", configurationFromContext?.configurationSettings?.name)
    }

    fun `test configuration producer with inline table format`() {
        @Language("TOML")
        val tomlText = """
            [tasks]
            "bar" = { run = "echo bar" }
            #^
        """.trimIndent()

        inlineFile(tomlText, "mise.toml") as TomlFile
        val element = findElementInEditor<PsiElement>()

        val context = createConfigurationContext(element)
        val producer = MiseTomlTaskRunConfigurationProducer()

        val configurationFromContext = producer.findOrCreateConfigurationFromContext(context)
        assertNotNull("Configuration should be created from context", configurationFromContext)
        assertEquals("Run bar", configurationFromContext?.configurationSettings?.name)
    }

    fun `test task command line uses MISE_CD instead of cd flag`() {
        seedExecutableInfo()
        val configuration = createRunConfiguration().apply {
            miseConfigEnvironment = "development"
            miseTaskName = "show"
            workingDirectory = "${'$'}PROJECT_DIR${'$'}/subdir"
            taskParams = "--flag value"
            envVars = EnvironmentVariablesData.create(mapOf("MISE_CD" to "/user/value"), true)
        }

        val commandLine = configuration.createCommandLine()

        assertEquals(project.basePath, commandLine.workDirectory?.path)
        assertEquals(project.basePath + "/subdir", commandLine.environment["MISE_CD"])
        assertFalse(commandLine.parametersList.parameters.contains("-C"))
        assertEquals(
            listOf("--env", "development", "run", "show", "--", "--flag", "value"),
            commandLine.parametersList.parameters,
        )
    }

    fun `test task command line converts relative MISE_CD to absolute path`() {
        seedExecutableInfo()
        val configuration = createRunConfiguration().apply {
            miseTaskName = "show"
            workingDirectory = "subdir"
        }

        val commandLine = configuration.createCommandLine()

        assertEquals(project.basePath + "/subdir", commandLine.environment["MISE_CD"])
        assertEquals(listOf("run", "show"), commandLine.parametersList.parameters)
    }

    private fun createConfigurationContext(element: PsiElement): ConfigurationContext {
        return ConfigurationContext.createEmptyContextForLocation(PsiLocation.fromPsiElement(myFixture.project, element))
    }

    private fun seedExecutableInfo(path: String = "mise") {
        val cacheService = project.service<MiseCacheService>()
        cacheService.invalidateAllExecutables()
        cacheService.getOrComputeExecutable(MiseExecutableManager.EXECUTABLE_KEY) {
            MiseExecutableInfo(path = path, version = null)
        }
    }

    private fun createRunConfiguration(): MiseTomlTaskRunConfiguration {
        val configurationType = MiseTomlTaskRunConfigurationType()
        val factory = MiseTomlTaskRunConfigurationFactory(configurationType)
        return MiseTomlTaskRunConfiguration(project, factory, "Run show")
    }
}
