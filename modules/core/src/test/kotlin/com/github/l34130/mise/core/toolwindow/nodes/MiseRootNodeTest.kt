package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.command.MiseExecutableInfo
import com.github.l34130.mise.core.command.MiseExecutableManager
import com.github.l34130.mise.core.command.MiseVersion
import com.github.l34130.mise.core.processOutput
import com.github.l34130.mise.core.util.markProjectCacheReady
import com.github.l34130.mise.core.withMiseCommandLineExecutor
import com.intellij.notification.impl.NotificationGroupEP
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertThrows

class MiseRootNodeTest : BasePlatformTestCase() {
    override fun runInDispatchThread(): Boolean = false

    override fun setUp() {
        super.setUp()
        project.markProjectCacheReady()
        val cache = project.service<MiseCacheService>()
        cache.invalidateAllCommands()
        cache.invalidateAllExecutables()
        cache.getOrComputeExecutable(MiseExecutableManager.EXECUTABLE_KEY) {
            MiseExecutableInfo("mise", MiseVersion(2026, 1, 1))
        }
    }

    override fun tearDown() {
        try {
            project.service<MiseCacheService>().invalidateAllCommands()
            project.service<MiseCacheService>().invalidateAllExecutables()
        } finally {
            super.tearDown()
        }
    }

    fun `test tool cancellation propagates`() = assertCancellationPropagates("ls")

    fun `test task cancellation propagates`() = assertCancellationPropagates("config")

    fun `test environment cancellation propagates`() = assertCancellationPropagates("env")

    private fun assertCancellationPropagates(command: String) {
        for (cancellation in listOf(ProcessCanceledException(), CancellationException("cancelled"))) {
            project.service<MiseCacheService>().invalidateAllCommands()
            withMiseCommandLineExecutor(
                executor = { commandLine, _ ->
                    val params = commandLine.parametersList.list
                    if (params.first() == command) throw cancellation
                    processOutput(
                        stdout = when (params) {
                            listOf("config", "ls", "--json") -> "[]"
                            listOf("config", "--tracked-configs") -> ""
                            else -> "{}"
                        },
                    )
                },
            ) {
                assertThrows(cancellation.javaClass) { MiseRootNode(project).children }
            }
        }
    }

    fun `test ordinary failure still creates an error node`() {
        // The core module's test sandbox does not load the root plugin's notification group.
        val group = XmlSerializer.deserialize(
            JDOMUtil.load("""<notificationGroup id="Mise" displayType="BALLOON"/>"""),
            NotificationGroupEP::class.java,
        )
        ExtensionPointName.create<NotificationGroupEP>("com.intellij.notificationGroup")
            .point.registerExtension(group, testRootDisposable)
        val failure = IllegalStateException("config lookup failed")
        withMiseCommandLineExecutor(
            executor = { commandLine, _ ->
                if (commandLine.parametersList.list.first() == "config") throw failure
                processOutput(stdout = "{}")
            },
        ) {
            val children = MiseRootNode(project).children.toList()
            assertEquals(3, children.size)
            assertInstanceOf(children[0], MiseToolServiceNode::class.java)
            assertInstanceOf(children[1], MiseErrorNode::class.java)
            assertSame(failure, children[1].value)
            assertInstanceOf(children[2], MiseEnvironmentServiceNode::class.java)
        }
    }
}
