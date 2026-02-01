@file:Suppress("ktlint")
package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.FileTestBase
import com.github.l34130.mise.core.execution.configuration.MiseTomlTaskRunConfigurationProducer
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
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

    private fun createConfigurationContext(element: PsiElement): ConfigurationContext {
        return ConfigurationContext.createEmptyContextForLocation(PsiLocation.fromPsiElement(myFixture.project, element))
    }
}
