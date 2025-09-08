package com.github.l34130.mise.core

import com.github.l34130.mise.core.model.MiseShellScriptTask
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.reflect.KClass

@Suppress("ktlint:standard:function-naming")
class MiseServiceTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData"

    fun `test tasks`() {
        myFixture.configureByFiles(*allTestFiles())

        val service = project.service<MiseProjectService>()
        runBlocking { service.refresh() }

        val tasks = service.getTasks()

        listOf<TestResult>(
            TestResult("default-inline-table-task", "mise.toml", MiseTomlTableTask::class),
            TestResult("default-table-task", "mise.toml", MiseTomlTableTask::class),
            TestResult("lint", "mise.toml", MiseTomlTableTask::class),
            TestResult("lint:test1", "xtasks/lint/test1", MiseShellScriptTask::class),
            TestResult("lint:test2", "xtasks/lint/test2", MiseShellScriptTask::class),
            TestResult("xtask", "xtasks/xtask.sh", MiseShellScriptTask::class),
            TestResult("task-in-test-config", "mise.test.toml", MiseTomlTableTask::class),
        ).forEach { (name, source, type) ->
            val task = tasks.find { it.name == name }
            assertNotNull("Task '$name' not found", task)
            assertEquals("Task '$name' has wrong source", source, task!!.source)
            assertEquals("Task '$name' has wrong type", type, task::class)
        }
    }

    private data class TestResult(
        val name: String,
        val source: String,
        val type: KClass<*>,
    )

    private fun allTestFiles(): Array<String> =
        File(testDataPath)
            .walk()
            .filter { it.isFile }
            .map { it.path.substringAfter("$testDataPath/") }
            .toList()
            .toTypedArray()
}
