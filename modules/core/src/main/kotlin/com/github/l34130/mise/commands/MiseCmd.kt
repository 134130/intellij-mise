package com.github.l34130.mise.commands

import com.github.l34130.mise.notifications.Notification
import com.github.l34130.mise.utils.fromJson
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationType

object MiseCmd {
    fun loadEnv(workDir: String?): Map<String, String> {
        try {
            val process =
                GeneralCommandLine("mise", "env")
                    .withWorkDirectory(workDir)
                    .createProcess()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val stderr = process.errorReader().use { it.readText() }
                Notification.notify("Failed to import Mise environment: $stderr", NotificationType.WARNING)
                return emptyMap()
            }

            val output = process.inputReader().use { it.readText() }
            return output
                .split("\n")
                .map { it.removePrefix("export ") }
                .filter { it.isNotBlank() }
                .map { it.split("=", limit = 2) }
                .associate { it[0] to it[1] }
        } catch (e: Exception) {
            Notification.notify("Failed to import Mise environment: ${e.message}", NotificationType.ERROR)
            return emptyMap()
        }
    }

    fun loadTools(workDir: String?): Map<String, List<MiseTool>> {
        try {
            val miseVersionStr =
                runCommandLine(listOf("mise", "version"), workDir).getOrNull()

            val miseVersion =
                if (miseVersionStr == null) {
                    MiseVersion(0, 0, 0)
                } else {
                    MiseVersion.from(miseVersionStr)
                }

            // https://github.com/jdx/mise/commit/6e7e4074989bda47e40900cb651b694c72d39f4d
            val supportsOfflineFlag = miseVersion >= MiseVersion(2024, 11, 4)
            val cliResult =
                if (supportsOfflineFlag) {
                    runCommandLine(listOf("mise", "ls", "--current", "--json", "--offline"), workDir)
                } else {
                    runCommandLine(listOf("mise", "ls", "--current", "--json"), workDir)
                }

            if (cliResult.isFailure) {
                Notification.notify(
                    "Failed to import Mise tools: ${cliResult.exceptionOrNull() ?: "Unknown exception"}",
                    NotificationType.WARNING,
                )
                return emptyMap()
            }

            return GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .fromJson<Map<String, List<MiseTool>>>(cliResult.getOrThrow())
        } catch (e: Exception) {
            Notification.notify("Failed to import Mise tools: ${e.message}", NotificationType.ERROR)
            return emptyMap()
        }
    }

    private fun runCommandLine(
        commandLineArgs: List<String>,
        workDir: String?,
    ): Result<String> {
        val process =
            GeneralCommandLine(commandLineArgs)
                .withWorkDirectory(workDir)
                .createProcess()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val stderr = process.errorReader().use { it.readText() }
            throw RuntimeException(stderr)
        }

        return Result.success(process.inputReader().use { it.readText() })
    }
}
