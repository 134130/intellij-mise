package com.github.l34130.mise.commands

sealed class MiseCmdException(
    message: String,
    throwable: Throwable? = null,
) : RuntimeException(message, throwable) {
    companion object {
        fun parseFromStderr(stderr: String): MiseCmdException? =
            MiseCmdNotTrustedConfigFileException.parseFromStderr(stderr)
                ?: MiseCmdErrorParsingConfigFileException.parseFromStderr(stderr)
    }
}

class MiseCmdNotTrustedConfigFileException(
    val configFilePath: String,
) : MiseCmdException("Config file $configFilePath is not trusted. Trust it with `mise trust`.") {
    companion object {
        private val stderrRegex = Regex("Config file (.+) is not trusted.")

        fun parseFromStderr(stderr: String): MiseCmdNotTrustedConfigFileException? {
            val result = stderrRegex.find(stderr) ?: return null

            check(result.groupValues.size >= 2) {
                "Failed to parse config file path from stderr: $stderr"
            }

            val configFilePath = result.groupValues[1]
            return MiseCmdNotTrustedConfigFileException(configFilePath)
        }
    }
}

class MiseCmdErrorParsingConfigFileException(
    val configFilePath: String,
) : MiseCmdException("error parsing config file: $configFilePath") {
    companion object {
        private val stderrRegex = Regex("error parsing config file: (.+)")

        fun parseFromStderr(stderr: String): MiseCmdErrorParsingConfigFileException? {
            val result = stderrRegex.find(stderr) ?: return null

            check(result.groupValues.size >= 2) {
                "Failed to parse config file path from stderr: $stderr"
            }

            val configFilePath = result.groupValues[1]
            return MiseCmdErrorParsingConfigFileException(configFilePath)
        }
    }
}
