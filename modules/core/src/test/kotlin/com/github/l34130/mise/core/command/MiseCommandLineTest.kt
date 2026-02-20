package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.cache.MiseCacheService
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MiseCommandLineTest : BasePlatformTestCase() {
    private fun processOutput(stdout: String = "", stderr: String = "", exitCode: Int = 0): ProcessOutput {
        val output = ProcessOutput()
        output.appendStdout(stdout)
        output.appendStderr(stderr)
        output.exitCode = exitCode
        return output
    }

    private fun <T> withCommandLineExecutor(
        executor: (GeneralCommandLine, Int) -> ProcessOutput,
        block: () -> T
    ): T {
        val previous = MiseCommandLine.commandLineExecutor
        MiseCommandLine.commandLineExecutor = MiseCommandLineExecutor { generalCommandLine, timeout ->
            executor(generalCommandLine, timeout)
        }
        return try {
            block()
        } finally {
            MiseCommandLine.commandLineExecutor = previous
        }
    }

    private fun seedExecutableInfo(path: String = "mise", version: MiseVersion = MiseVersion(2026, 1, 1)) {
        val cacheService = project.service<MiseCacheService>()
        cacheService.invalidateAllExecutables()
        cacheService.getOrComputeExecutable(MiseExecutableManager.EXECUTABLE_KEY) {
            MiseExecutableInfo(path = path, version = version)
        }
    }

    fun `test runRawCommandLine injects stable mise env cache session key`() {
        seedExecutableInfo()
        val environments = mutableListOf<Map<String, String?>>()

        withCommandLineExecutor(
            executor = { commandLine, _ ->
                environments += commandLine.environment.toMap()
                processOutput(stdout = "ok")
            },
        ) {
            val commandLine = MiseCommandLine(project, project.basePath, null)
            commandLine.runRawCommandLine(listOf("env", "--json")).getOrThrow()
            commandLine.runRawCommandLine(listOf("ls", "--local", "--json")).getOrThrow()
        }

        assertEquals(2, environments.size)
        val first = environments[0]
        val second = environments[1]

        assertFalse(first["__MISE_ENV_CACHE_KEY"].isNullOrBlank())
        assertEquals(first["__MISE_ENV_CACHE_KEY"], second["__MISE_ENV_CACHE_KEY"])
    }
}
