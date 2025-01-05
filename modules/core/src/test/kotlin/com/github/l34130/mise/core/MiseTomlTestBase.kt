package com.github.l34130.mise.core

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language

abstract class MiseTomlTestBase : BasePlatformTestCase() {
    protected fun InlineFile(
        @Language("TOML") text: String,
        name: String = "mise.toml",
    ) {
        myFixture.configureByText(name, text.trimIndent())
    }
}
