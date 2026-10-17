package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MiseTomlFileListenerTest : BasePlatformTestCase() {
    fun `test package json creation edit rename and deletion broadcast changes`() {
        val root = myFixture.addFileToProject("README.md", "").virtualFile.parent
        project.service<MiseTomlFileListener>()
        var changes = 0
        MiseProjectEventListener.subscribe(project, testRootDisposable) {
            if (it.kind == MiseProjectEvent.Kind.TOML_CHANGED) changes++
        }

        WriteAction.run<Throwable> {
            val file = root.createChildData(this, "package.json")
            assertTrue("Creation must be detected before mise tracks the file", changes > 0)
            var previous = changes
            file.setBinaryContent("{}".toByteArray())
            assertTrue(changes > previous)
            previous = changes
            file.rename(this, "package.json.bak")
            assertTrue("Renaming away must invalidate the old configuration", changes > previous)
            previous = changes
            file.rename(this, "package.json")
            assertTrue(changes > previous)
            previous = changes
            file.delete(this)
            assertTrue(changes > previous)
        }
    }

    fun `test package manager inputs invalidate mise commands`() {
        assertTrue(isLikelyMiseRelatedFileName("package.json"))
        assertTrue(isLikelyMiseRelatedFileName("pnpm-lock.yaml"))
        assertFalse(isLikelyMiseRelatedFileName("package-lock.json"))
        assertFalse(isLikelyMiseRelatedFileName("package.json.bak"))
    }

    fun `test likely mise related file names include tool versions files`() {
        assertTrue(isLikelyMiseRelatedFileName(".tool-versions"))
        assertTrue(isLikelyMiseRelatedFileName(".tool-versions.local"))
        assertFalse(isLikelyMiseRelatedFileName(".tool-version"))
    }
}
