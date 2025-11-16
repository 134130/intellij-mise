package com.github.l34130.mise.nx.run

import com.github.l34130.mise.core.MiseHelper
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CommandLineEnvCustomizer
import java.nio.file.Paths
import kotlin.io.path.pathString

@Suppress("UnstableApiUsage")
class MiseNxCommandLineEnvCustomizer : CommandLineEnvCustomizer {
    override fun customizeEnv(
        commandLine: GeneralCommandLine,
        environment: MutableMap<String, String>,
    ) {
        if (Paths.get(commandLine.exePath).fileName.toString() in NX_EXECUTABLES) {
            val envvar = MiseHelper.getMiseEnvVarsOrNotify(null, commandLine.workingDirectory?.pathString, null)
            environment.putAll(envvar)
        }
    }

    companion object {
        private val NX_EXECUTABLES = listOf("nx", "nx.cmd")
    }
}
