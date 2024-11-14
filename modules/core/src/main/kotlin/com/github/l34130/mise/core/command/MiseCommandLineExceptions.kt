package com.github.l34130.mise.core.command

sealed class MiseCommandLineException(
    override val message: String,
    throwable: Throwable? = null,
) : RuntimeException(throwable) {
    companion object {
        fun parseFromStderr(stderr: String): MiseCommandLineException? =
            MiseCommandLineNotTrustedConfigFileException.parseFromStderr(stderr)
                ?: MiseCommandLineErrorParsingConfigFileException.parseFromStderr(stderr)
    }
}

class MiseCommandLineNotTrustedConfigFileException(
    val configFilePath: String,
) : MiseCommandLineException(
        "Config file $configFilePath is not trusted. Trust it with `mise trust`.",
    ) {
    companion object {
        private val stderrRegex = Regex("Config file (.+) is not trusted.")

        fun parseFromStderr(stderr: String): MiseCommandLineNotTrustedConfigFileException? {
            val result = stderrRegex.find(stderr) ?: return null

            check(result.groupValues.size >= 2) {
                "Failed to parse config file path from stderr: $stderr"
            }

            val configFilePath = result.groupValues[1]
            return MiseCommandLineNotTrustedConfigFileException(configFilePath)
        }
    }
}

class MiseCommandLineErrorParsingConfigFileException(
    val configFilePath: String,
) : MiseCommandLineException("error parsing config file: $configFilePath") {
    companion object {
        private val stderrRegex = Regex("error parsing config file: (.+)")

        fun parseFromStderr(stderr: String): MiseCommandLineErrorParsingConfigFileException? {
            val result = stderrRegex.find(stderr) ?: return null

            check(result.groupValues.size >= 2) {
                "Failed to parse config file path from stderr: $stderr"
            }

            val configFilePath = result.groupValues[1]
            return MiseCommandLineErrorParsingConfigFileException(configFilePath)
        }
    }
}
