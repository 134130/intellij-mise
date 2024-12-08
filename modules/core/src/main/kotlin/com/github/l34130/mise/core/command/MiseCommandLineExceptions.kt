package com.github.l34130.mise.core.command

import com.intellij.execution.configurations.GeneralCommandLine

sealed class MiseCommandLineException(
    val generalCommandLine: GeneralCommandLine,
    override val message: String,
    throwable: Throwable? = null,
) : RuntimeException(throwable) {
    companion object {
        fun parseFromStderr(generalCommandLine: GeneralCommandLine, stderr: String): MiseCommandLineException? =
            MiseCommandLineNotTrustedConfigFileException.parseFromStderr(generalCommandLine, stderr)
                ?: MiseCommandLineErrorParsingConfigFileException.parseFromStderr(generalCommandLine, stderr)
    }
}

class MiseCommandLineNotTrustedConfigFileException(
    generalCommandLine: GeneralCommandLine,
    val configFilePath: String,
) : MiseCommandLineException(
    generalCommandLine = generalCommandLine,
    message = "Config file $configFilePath is not trusted. Trust it with `mise trust`.",
    ) {
    companion object {
        private val stderrRegex = Regex("Config file (.+) is not trusted.")

        fun parseFromStderr(
            generalCommandLine: GeneralCommandLine,
            stderr: String
        ): MiseCommandLineNotTrustedConfigFileException? {
            val result = stderrRegex.find(stderr) ?: return null

            check(result.groupValues.size >= 2) {
                "Failed to parse config file path from stderr: $stderr"
            }

            val configFilePath = result.groupValues[1]
            return MiseCommandLineNotTrustedConfigFileException(generalCommandLine, configFilePath)
        }
    }
}

class MiseCommandLineErrorParsingConfigFileException(
    generalCommandLine: GeneralCommandLine,
    val configFilePath: String,
) : MiseCommandLineException(
    generalCommandLine = generalCommandLine,
    message = "error parsing config file: $configFilePath"
) {
    companion object {
        private val stderrRegex = Regex("error parsing config file: (.+)")

        fun parseFromStderr(
            generalCommandLine: GeneralCommandLine,
            stderr: String
        ): MiseCommandLineErrorParsingConfigFileException? {
            val result = stderrRegex.find(stderr) ?: return null

            check(result.groupValues.size >= 2) {
                "Failed to parse config file path from stderr: $stderr"
            }

            val configFilePath = result.groupValues[1]
            return MiseCommandLineErrorParsingConfigFileException(generalCommandLine, configFilePath)
        }
    }
}
