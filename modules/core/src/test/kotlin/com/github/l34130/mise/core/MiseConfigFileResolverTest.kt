package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.command.MiseCommandLineExecutor
import com.github.l34130.mise.core.command.MiseExecutableInfo
import com.github.l34130.mise.core.command.MiseExecutableManager
import com.github.l34130.mise.core.command.MiseVersion
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class MiseConfigFileResolverTest : BasePlatformTestCase() {
    private fun processOutput(stdout: String = "", stderr: String = "", exitCode: Int = 0): ProcessOutput {
        val output = ProcessOutput()
        output.appendStdout(stdout)
        output.appendStderr(stderr)
        output.exitCode = exitCode
        return output
    }

    private fun <T> withCommandLineExecutor(
        executor: (GeneralCommandLine, Int) -> ProcessOutput,
        block: () -> T,
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

    fun `test resolveTrackedFiles falls back to manual scan and tracks external env files`() {
        seedExecutableInfo()
        myFixture.configureByText(
            "mise.toml",
            """
            env_file = ".env"

            [env._]
            file = [".env.yaml"]
            source = ["env.sh"]

            [vars._]
            file = "vars.env"
            source = "vars.sh"
            """.trimIndent(),
        )
        myFixture.configureByText(".env", "FOO=BAR")
        myFixture.configureByText(".env.yaml", "FOO: BAR")
        myFixture.configureByText("env.sh", "export FOO=BAR")
        myFixture.configureByText("vars.env", "A=B")
        myFixture.configureByText("vars.sh", "export A=B")

        val baseDirVf = VirtualFileManager.getInstance().findFileByUrl("temp:///src") ?: error("Base directory not found")
        val resolver = project.service<MiseConfigFileResolver>()

        val snapshot =
            withCommandLineExecutor(
                executor = { _, _ -> processOutput(stderr = "command failed", exitCode = 1) },
            ) {
                runBlocking {
                    resolver.resolveTrackedFiles(baseDirVf, refresh = true, configEnvironment = "test")
                }
            }

        val configPaths = snapshot.configTomlFiles.map { it.path }.toSet()
        val externalPaths = snapshot.externalTrackedFiles.map { it.path }.toSet()

        assertEquals(setOf("/src/mise.toml"), configPaths)
        assertEquals(
            setOf("/src/.env", "/src/.env.yaml", "/src/env.sh", "/src/vars.env", "/src/vars.sh"),
            externalPaths,
        )
    }

    fun `test resolveConfigFiles includes parent config when tracked-configs returns parent`() {
        seedExecutableInfo()
        val rootDir = Files.createTempDirectory("mise-parent-config-test")
        val parentToml = rootDir.resolve("mise.toml").also { it.writeText("[tools]\nnode = '20'\n") }
        val projectDir = rootDir.resolve("project").createDirectories()
        val childToml = projectDir.resolve("mise.toml").also { it.writeText("[tools]\nnode = '22'\n") }

        val baseDirVf =
            LocalFileSystem.getInstance().refreshAndFindFileByPath(projectDir.toString().replace('\\', '/'))
                ?: error("Failed to resolve project dir in VFS")
        val resolver = project.service<MiseConfigFileResolver>()

        val configs =
            withCommandLineExecutor(
                executor = { commandLine, _ ->
                    val isTrackedConfigs = commandLine.commandLineString.contains("config --tracked-configs")
                    if (isTrackedConfigs) {
                        processOutput(
                            stdout =
                                listOf(parentToml, childToml)
                                    .joinToString("\n") { it.toString().replace('\\', '/') } + "\n",
                        )
                    } else {
                        processOutput()
                    }
                },
            ) {
                runBlocking {
                    resolver.resolveConfigFiles(baseDirVf, refresh = true, configEnvironment = "test")
                }
            }

        val configPaths = configs.map { it.path }.toSet()
        assertTrue(configPaths.contains(parentToml.toString().replace('\\', '/')))
        assertTrue(configPaths.contains(childToml.toString().replace('\\', '/')))
    }
}
