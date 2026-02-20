package com.github.l34130.mise.core.command

import com.intellij.testFramework.LightPlatformTestCase

class MiseCommandLineHelperTest : LightPlatformTestCase() {

    fun `test needsCustomization true for fresh env`() {
        val env = mutableMapOf("PATH" to "x")
        assertTrue(MiseCommandLineHelper.environmentNeedsCustomization(env))
    }

    fun `test hasBeenCustomized flips state without mutating env contents`() {
        val env = mutableMapOf("PATH" to "x")
        val snapshot = env.toMap()

        MiseCommandLineHelper.environmentHasBeenCustomized(env)

        assertFalse(MiseCommandLineHelper.environmentNeedsCustomization(env))
        assertEquals("Env map must not be polluted with any marker keys/values", snapshot, env, )
    }

    fun `test tracking is by identity not by contents`() {
        val env1 = mutableMapOf("PATH" to "x")
        val env2 = mutableMapOf("PATH" to "x") // same content, different instance

        MiseCommandLineHelper.environmentHasBeenCustomized(env1)

        assertFalse(
            "The env map should not require customization when it has already been customized",
            MiseCommandLineHelper.environmentNeedsCustomization(env1)
        )
        assertTrue(
            "A different env map instance must not be treated as customized",
            MiseCommandLineHelper.environmentNeedsCustomization(env2)
        )
    }

    fun `test customizationFailed clears state allowing retry`() {
        val env = mutableMapOf("PATH" to "x")
        MiseCommandLineHelper.environmentHasBeenCustomized(env)

        MiseCommandLineHelper.environmentCustomizationFailed(env)

        assertTrue(MiseCommandLineHelper.environmentNeedsCustomization(env))
    }

    fun `test skipCustomization blocks customization for same env map instance`() {
        val env = mutableMapOf("PATH" to "x")

        MiseCommandLineHelper.environmentSkipCustomization(env)

        assertFalse(MiseCommandLineHelper.environmentNeedsCustomization(env))
    }

    fun `test mutating the map does not clear the flag`() {
        val env = mutableMapOf("PATH" to "x")
        MiseCommandLineHelper.environmentHasBeenCustomized(env)
        env["PATH"] = "y"
        assertFalse(MiseCommandLineHelper.environmentNeedsCustomization(env))
    }
}
