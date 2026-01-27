package com.github.l34130.mise.core

import com.github.l34130.mise.core.model.MiseShellScriptTask
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.reflect.KClass

@Suppress("ktlint:standard:function-naming")
@RunWith(Parameterized::class)
class MiseServiceTest(
    private val taskName: String,
    private val taskSource: String,
    private val taskType: KClass<*>,
    private val taskEnv: String?,
    private val environment: String?,
    @Suppress("unused") private val description: String,
) : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData"

    @org.junit.Test
    fun `test task`() {
        myFixture.configureByFiles(*allTestFiles())

        val settings = project.service<MiseProjectSettings>()
        settings.state.miseConfigEnvironment = environment ?: ""

        val service = project.service<MiseTaskResolver>()

        VirtualFileManager.getInstance().findFileByUrl("temp:///src") ?: error("Base directory not found")
        val tasks = runBlocking { service.getMiseTasks() }
        val task = tasks.find { it.name == taskName }

        if (taskEnv == null || taskEnv == environment) {
            // Task should be present
            assertNotNull("Task '$taskName' not found for environment '$environment' ${tasks.joinToString(", ") { it.name }}", task)
            val nonNullTask = task ?: error("Task '$taskName' not found")
            assertEquals("Task '$taskName' has wrong source", taskSource, nonNullTask.source)
            assertEquals("Task '$taskName' has wrong type", taskType, nonNullTask::class)
        } else {
            // Task should NOT be present
            assertNull("Task '$taskName' should not be present for environment '$environment'", task)
        }
    }

    companion object {
        private val allTasks = listOf(
            TestResult("default-inline-table-task", "/src/mise.toml", MiseTomlTableTask::class, env = null),
            TestResult("default-table-task", "/src/mise.toml", MiseTomlTableTask::class, env = null),
            TestResult("lint", "/src/mise.toml", MiseTomlTableTask::class, env = null),
            TestResult("lint:test1", "/src/xtasks/lint/test1", MiseShellScriptTask::class, env = null),
            TestResult("lint:test2", "/src/xtasks/lint/test2", MiseShellScriptTask::class, env = null),
            TestResult("xtask", "/src/xtasks/xtask.sh", MiseShellScriptTask::class, env = null),
            TestResult("task-in-tasks-toml", "/src/tasks.toml", MiseTomlTableTask::class, env = null),
            TestResult("task-in-test-config", "/src/mise.test.toml", MiseTomlTableTask::class, env = "test"),
        )

        private val environments = listOf(null, "test", "dev")

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {5} in environment {4}")
        fun data(): Collection<Array<Any?>> {
            return allTasks.flatMap { task ->
                environments.map { env ->
                    val shouldBePresent = task.env == null || task.env == env
                    val description = if (shouldBePresent) "is present" else "is not present"
                    arrayOf(task.name, task.source, task.type, task.env, env, description)
                }
            }
        }
    }

    private data class TestResult(
        val name: String,
        val source: String,
        val type: KClass<*>,
        val env: String? = null,
    )

    private fun allTestFiles(): Array<String> =
        File(testDataPath)
            .walk()
            .filter { it.isFile }
            .map { it.relativeTo(File(testDataPath)).path.replace('\\', '/') }
            .toList()
            .toTypedArray()
}
