package com.github.l34130.mise.core.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Integration tests that download a real mise binary and validate
 * actual CLI output for `config ls --json` and `config --tracked-configs`.
 *
 * These tests verify the real-world behavior of mise commands
 * that [MiseCommandLineHelper.getProjectTrackedConfigs] depends on.
 */
@RunWith(JUnit4::class)
class MiseProjectConfigsIntegrationTest {
    private lateinit var miseBinary: Path

    @Before
    fun setUp() {
        // Skip tests if we can't determine the platform
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        assumeTrue(
            "Skipping on unsupported platform: $os/$arch",
            (os.contains("mac") || os.contains("linux")) &&
                (arch == "aarch64" || arch == "arm64" || arch == "amd64" || arch == "x86_64"),
        )

        miseBinary = MiseBinaryFixture.getBinary(MISE_VERSION)
        assumeTrue("Mise binary should exist", Files.exists(miseBinary))
    }

    private fun runMise(
        vararg args: String,
        workDir: Path,
        env: Map<String, String> = emptyMap(),
    ): String {
        val miseEnv = isolatedMiseEnv(workDir) + env

        // Automatically trust the workDir to avoid prompt errors
        val trustProcess = ProcessBuilder(miseBinary.toString(), "trust", ".")
            .directory(workDir.toFile())
            .redirectErrorStream(true)
            .also { pb -> configureMiseEnvironment(pb, miseEnv) }
            .start()
        trustProcess.waitFor()

        val process =
            ProcessBuilder(miseBinary.toString(), *args)
                .directory(workDir.toFile())
                .redirectErrorStream(false)
                .also { pb -> configureMiseEnvironment(pb, miseEnv) }
                .start()

        val stdout = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            val stderr = process.errorStream.bufferedReader().readText()
            "mise ${args.joinToString(" ")} exited with code $exitCode.\nstderr: $stderr\nstdout: $stdout"
        }
        return stdout
    }

    private fun configureMiseEnvironment(processBuilder: ProcessBuilder, env: Map<String, String>) {
        val processEnv = processBuilder.environment()
        processEnv.keys.removeIf { it.startsWith("MISE_") }
        processEnv.putAll(env)
    }

    private fun isolatedMiseEnv(workDir: Path): Map<String, String> {
        val rootDir = workDir.resolve(".mise-test-state").createDirectories()
        val configDir = rootDir.resolve("config").createDirectories()
        val dataDir = rootDir.resolve("data").createDirectories()
        val stateDir = rootDir.resolve("state").createDirectories()
        val cacheDir = rootDir.resolve("cache").createDirectories()
        val homeDir = rootDir.resolve("home").createDirectories()
        val globalConfigFile = configDir.resolve("config.toml")
        globalConfigFile.writeText("")

        return mapOf(
            "HOME" to homeDir.toString(),
            "XDG_CONFIG_HOME" to configDir.toString(),
            "XDG_DATA_HOME" to dataDir.toString(),
            "XDG_STATE_HOME" to stateDir.toString(),
            "XDG_CACHE_HOME" to cacheDir.toString(),
            "MISE_GLOBAL_CONFIG_FILE" to globalConfigFile.toString(),
            "MISE_CONFIG_DIR" to configDir.resolve("mise").createDirectories().toString(),
            "MISE_DATA_DIR" to dataDir.resolve("mise").createDirectories().toString(),
            "MISE_STATE_DIR" to stateDir.resolve("mise").createDirectories().toString(),
            "MISE_CACHE_DIR" to cacheDir.resolve("mise").createDirectories().toString(),
        )
    }

    @Test
    fun `config ls --json returns only active configs for current directory`() {
        val rootDir = Files.createTempDirectory("mise-integration-config-ls")
        val projectDir = rootDir.resolve("my-project").createDirectories()
        projectDir.resolve("mise.toml").writeText("[tools]\nnode = '22'\n")

        val otherDir = rootDir.resolve("other-project").createDirectories()
        otherDir.resolve("mise.toml").writeText("[tools]\npython = '3.12'\n")

        val output = runMise("config", "ls", "--json", workDir = projectDir)
        val parsed: List<MiseConfigLsOutput> = MiseCommandLineOutputParser.parse(output)
        val paths = parsed.map { it.path }

        assertTrue(
            "Should include project mise.toml",
            paths.any { it.contains("my-project/mise.toml") },
        )
        assertFalse(
            "Should NOT include other-project mise.toml",
            paths.any { it.contains("other-project/mise.toml") },
        )
    }

    @Test
    fun `config --tracked-configs returns configs from current directory`() {
        val rootDir = Files.createTempDirectory("mise-integration-tracked")
        val projectDir = rootDir.resolve("my-project").createDirectories()
        projectDir.resolve("mise.toml").writeText("[tools]\nnode = '22'\n")

        runMise("config", "ls", "--json", workDir = projectDir)
        val output = runMise("config", "--tracked-configs", workDir = projectDir)
        val paths = output.lines().map { it.trim() }.filter { it.isNotEmpty() }

        assertTrue(
            "Should include project mise.toml",
            paths.any { it.contains("my-project/mise.toml") },
        )
    }

    @Test
    fun `mergeProjectConfigs with real CLI output excludes unrelated projects`() {
        val rootDir = Files.createTempDirectory("mise-integration-merge")
        val projectDir = rootDir.resolve("my-project").createDirectories()
        projectDir.resolve("mise.toml").writeText("[tools]\nnode = '22'\n")

        val otherDir = rootDir.resolve("other-project").createDirectories()
        otherDir.resolve("mise.toml").writeText("[tools]\npython = '3.12'\n")

        // Get real output from both commands
        val configLsOutput = runMise("config", "ls", "--json", workDir = projectDir)
        val activeConfigs: List<String> =
            MiseCommandLineOutputParser.parse<List<MiseConfigLsOutput>>(configLsOutput).map { it.path }

        val trackedOutput = runMise("config", "--tracked-configs", workDir = projectDir)
        val trackedConfigs = trackedOutput.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Now also run tracked-configs from the other project to simulate OS-wide results
        runMise("config", "ls", "--json", workDir = otherDir)
        val otherTrackedOutput = runMise("config", "--tracked-configs", workDir = otherDir)
        val otherTrackedConfigs = otherTrackedOutput.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val allTrackedConfigs = (trackedConfigs + otherTrackedConfigs).distinct()

        // Apply our mergeProjectConfigs logic
        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = activeConfigs,
            trackedConfigs = allTrackedConfigs,
            workDir = projectDir.toString(),
        )

        assertTrue(
            "Should include project mise.toml",
            result.any { it.contains("my-project/mise.toml") },
        )
        assertFalse(
            "Should NOT include other-project mise.toml",
            result.any { it.contains("other-project/mise.toml") },
        )
    }

    @Test
    fun `config ls --json output is parseable by MiseConfigLsOutput`() {
        val rootDir = Files.createTempDirectory("mise-integration-parse")
        rootDir.resolve("mise.toml").writeText("[tools]\nnode = '22'\n")

        val output = runMise("config", "ls", "--json", workDir = rootDir)
        val parsed: List<MiseConfigLsOutput> = MiseCommandLineOutputParser.parse(output)

        assertTrue("Should parse at least one config", parsed.isNotEmpty())
        assertTrue(
            "Should contain the project mise.toml",
            parsed.any { it.path.endsWith("mise.toml") },
        )
    }

    @Test
    fun `config ls --json returns configs ordered from specific to general`() {
        val rootDir = Files.createTempDirectory("mise-integration-order")
        val projectDir = rootDir.resolve("project").createDirectories()
        projectDir.resolve("mise.toml").writeText("[tools]\nnode = '22'\n")

        val output = runMise("config", "ls", "--json", workDir = projectDir)
        val parsed: List<MiseConfigLsOutput> = MiseCommandLineOutputParser.parse(output)
        val paths = parsed.map { it.path }

        // The first entry should be the most specific (project-level)
        if (paths.isNotEmpty()) {
            assertTrue(
                "First config should be the project mise.toml",
                paths.first().contains("project/mise.toml"),
            )
        }
    }

    companion object {
        /**
         * The mise version to use for integration tests.
         * This should be updated when new mise features are needed.
         */
        private const val MISE_VERSION = "2025.11.1"
    }
}
