package com.github.l34130.mise.commands

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

class MiseEnvCmd(
    private val workDir: String?,
) {
    fun load(): Map<String, String> {
        try {
            val process =
                GeneralCommandLine("mise", "env")
                    .withWorkDirectory(workDir)
                    .createProcess()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val stderr = process.errorReader().use { it.readText() }
                Notifications.Bus.notify(
                    Notification(
                        GROUP_DISPLAY_ID,
                        "Mise warning",
                        "Failed to import Mise environment: $stderr",
                        NotificationType.WARNING,
                    ),
                )
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
            Notifications.Bus.notify(
                Notification(GROUP_DISPLAY_ID, "Mise error", "Failed to import Mise environment", NotificationType.ERROR),
            )
            return emptyMap()
        }
    }

    companion object {
        private const val GROUP_DISPLAY_ID = "Mise"
    }
}
