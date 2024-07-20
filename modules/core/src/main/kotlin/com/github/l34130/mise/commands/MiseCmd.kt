package com.github.l34130.mise.commands

import com.github.l34130.mise.extensions.fromJson
import com.github.l34130.mise.notifications.Notification
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
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
            val process =
                GeneralCommandLine("mise", "ls", "--current", "--json")
                    .withWorkDirectory(workDir)
                    .createProcess()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val stderr = process.errorReader().use { it.readText() }
                Notification.notify("Failed to import Mise tools: $stderr", NotificationType.WARNING)
                return emptyMap()
            }

            return GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .fromJson<Map<String, List<MiseTool>>>(process.inputReader())
        } catch (e: Exception) {
            Notification.notify("Failed to import Mise tools: ${e.message}", NotificationType.ERROR)
            return emptyMap()
        }
    }
}
