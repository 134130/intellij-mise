package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.wsl.WslPathUtils.resolveUserHomeAbbreviations
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MiseExecutableManagerTest : BasePlatformTestCase() {
    private val parseVersionMethod = MiseExecutableManager::class.java
        .getDeclaredMethod("parseVersionFromOutput", String::class.java)
        .apply { isAccessible = true }

    private fun seedExecutableInfo(path: String, version: MiseVersion? = null) {
        val cacheService = project.service<MiseCacheService>()
        cacheService.invalidateAllExecutables()
        cacheService.getOrComputeExecutable(MiseExecutableManager.EXECUTABLE_KEY) {
            MiseExecutableInfo(path = path, version = version)
        }
    }

    private fun processOutput(stdout: String, stderr: String = "", exitCode: Int = 0): ProcessOutput {
        val output = ProcessOutput()
        output.appendStdout(stdout)
        output.appendStderr(stderr)
        output.exitCode = exitCode
        return output
    }

    private fun parseVersionFromOutput(output: String): MiseVersion? {
        return parseVersionMethod.invoke(project.service<MiseExecutableManager>(), output) as MiseVersion?
    }

    private inline fun <T> withCommandLineExecutor(
        executor: MiseCommandLineExecutor,
        block: () -> T
    ): T {
        val previous = MiseCommandLine.commandLineExecutor
        MiseCommandLine.commandLineExecutor = executor
        return try {
            block()
        } finally {
            MiseCommandLine.commandLineExecutor = previous
        }
    }

    fun `test getExecutableParts returns cached parts`() {
        seedExecutableInfo("/usr/bin/mise")
        val manager = project.service<MiseExecutableManager>()

        val parts = manager.getExecutableParts()

        assertEquals(listOf("/usr/bin/mise"), parts)
    }

    fun `test matchesMiseExecutablePath returns true for matching prefix`() {
        seedExecutableInfo("/usr/bin/mise")
        val manager = project.service<MiseExecutableManager>()
        val commandLine = GeneralCommandLine(listOf("/usr/bin/mise", "env"))

        assertTrue(manager.matchesMiseExecutablePath(commandLine))
    }

    fun `test matchesMiseExecutablePath returns false for mismatched prefix`() {
        seedExecutableInfo("/usr/bin/mise")
        val manager = project.service<MiseExecutableManager>()
        val commandLine = GeneralCommandLine(listOf("mise", "env"))

        assertFalse(manager.matchesMiseExecutablePath(commandLine))
    }

    fun `test getExecutableVersion returns cached version`() {
        val version = MiseVersion(1, 2, 3)
        seedExecutableInfo("mise", version)
        val manager = project.service<MiseExecutableManager>()

        assertEquals(version, manager.getExecutableVersion())
    }

    fun `test getExecutableInfo parses detected version`() {
        val settings = project.service<MiseProjectSettings>()
        val configuredPath = "/opt/mise/bin/mise"
        settings.state.executablePath = configuredPath

        val samples = listOf(
            OutputSample(
                output = """
              _                                        __              
   ____ ___  (_)_______        ___  ____        ____  / /___ _________
  / __ `__ \/ / ___/ _ \______/ _ \/ __ \______/ __ \/ / __ `/ ___/ _ \
 / / / / / / (__  )  __/_____/  __/ / / /_____/ /_/ / / /_/ / /__/  __/
/_/ /_/ /_/_/____/\___/      \___/_/ /_/     / .___/_/\__,_/\___/\___/
                                            /_/                 by @jdx
2026.1.2 windows-x64 (2026-01-13)
mise WARN  mise version 2026.1.8 available
mise WARN  To update, run mise self-update
""".trimIndent(),
                expectedVersion = MiseVersion(2026, 1, 2),
                resolvedPath = null
            ),

            OutputSample(
                output = """
TRACE  1 [src\cli\mod.rs:594] migrate start
TRACE  1 [src\cli\mod.rs:594] migrate done
DEBUG  1 [src\cli\mod.rs:599] ARGS: C:\Users\jdx\scoop\apps\mise\current\bin\mise.exe version -vv
TRACE  1 [src\cli\mod.rs:600] MISE_BIN: C:\Users\jdx\scoop\apps\mise\current\bin\mise.exe
TRACE  1 [src\cli\mod.rs:606] run version start
              _                                       __              
   ____ ___  (_)_______        ___  ____        ____  / /___ _________                                                                                                               
  / __ `__ \/ / ___/ _ \______/ _ \/ __ \______/ __ \/ / __ `/ ___/ _ \                                                                                                              
 / / / / / / (__  )  __/_____/  __/ / / /_____/ /_/ / / /_/ / /__/  __/                                                                                                              
/_/ /_/ /_/_/____/\___/      \___/_/ /_/     / .___/_/\__,_/\___/\___/                                                                                                               
                                            /_/                 by @jdx                                                                                                              
2026.1.2 windows-x64 (2026-01-13)
TRACE  1 [src\file.rs:191] cat C:\Users\jdx\AppData\Local\Temp\mise\latest-version
WARN   1 [src\cli\version.rs:123] mise version 2026.1.8 available
WARN   1 [src\cli\version.rs:126] To update, run mise self-update
TRACE  1 [src\cli\mod.rs:606] run version done
""".trimIndent(),
                expectedVersion = MiseVersion(2026, 1, 2),
                resolvedPath = """C:\Users\jdx\scoop\apps\mise\current\bin\mise.exe"""
            ),

            OutputSample(
                output = """
              _                                        __
   ____ ___  (_)_______        ___  ____        ____  / /___ _________
  / __ `__ \/ / ___/ _ \______/ _ \/ __ \______/ __ \/ / __ `/ ___/ _ \
 / / / / / / (__  )  __/_____/  __/ / / /_____/ /_/ / / /_/ / /__/  __/
/_/ /_/ /_/_/____/\___/      \___/_/ /_/     / .___/_/\__,_/\___/\___/
                                            /_/                 by @jdx
2025.12.1 linux-x64 (2025-12-08)
mise WARN  mise version 2026.1.8 available
mise WARN  To update, run mise self-update
""".trimIndent(),
                expectedVersion = MiseVersion(2025, 12, 1),
                resolvedPath = null
            ),

            OutputSample(
                output = """
TRACE  1 [src/cli/mod.rs:467] migrate start
TRACE  1 [src/cli/mod.rs:467] migrate done
DEBUG  1 [src/cli/mod.rs:472] ARGS: /home/jdx/.local/bin/mise version -vv
TRACE  1 [src/cli/mod.rs:473] MISE_BIN: ~/.local/bin/mise
TRACE  1 [src/cli/mod.rs:479] run version start
              _                                        __
   ____ ___  (_)_______        ___  ____        ____  / /___ _________
  / __ `__ \/ / ___/ _ \______/ _ \/ __ \______/ __ \/ / __ `/ ___/ _ \
 / / / / / / (__  )  __/_____/  __/ / / /_____/ /_/ / / /_/ / /__/  __/
/_/ /_/ /_/_/____/\___/      \___/_/ /_/     / .___/_/\__,_/\___/\___/
                                            /_/                 by @jdx
2025.12.1 linux-x64 (2025-12-08)
TRACE  1 [src/file.rs:191] cat ~/.cache/mise/latest-version
WARN   1 [src/cli/version.rs:123] mise version 2026.1.8 available
WARN   1 [src/cli/version.rs:126] To update, run mise self-update
TRACE  1 [src/cli/mod.rs:479] run version done
        """.trimIndent(),
                expectedVersion = MiseVersion(2025, 12, 1),
                resolvedPath = "~/.local/bin/mise"
            )
        )

        val manager = project.service<MiseExecutableManager>()

        for (sample in samples) {
            project.service<MiseCacheService>().invalidateAllExecutables()

            val info = withCommandLineExecutor(
                { _, _ ->
                    processOutput(sample.output)
                }
            ) {
                manager.getExecutableInfo()
            }

            assertEquals(sample.expectedVersion, parseVersionFromOutput(sample.output))

            val expectedPath = if (sample.resolvedPath != null) {
                resolveUserHomeAbbreviations(sample.resolvedPath, project).toString()
            } else {
                configuredPath
            }
            assertEquals(expectedPath, info.path)

            if (sample.resolvedPath != null) {
                assertEquals(sample.expectedVersion, info.version)
            } else {
                assertNull(info.version)
            }
        }
    }

    private data class OutputSample(
        val output: String,
        val expectedVersion: MiseVersion,
        val resolvedPath: String?
    )
}
