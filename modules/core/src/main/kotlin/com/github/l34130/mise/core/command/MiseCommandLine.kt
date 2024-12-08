package com.github.l34130.mise.core.command

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.Logger

private val LOG = Logger.getInstance(MiseCommandLine::class.java)

internal class MiseCommandLine(
    private val workDir: String? = null,
) {

    fun <T> runCommandLine(vararg commandLineArgs: String): Result<T> = runCommandLine(commandLineArgs.toList())

    fun <T> runCommandLine(commandLineArgs: List<String>): Result<T> {
        val generalCommandLine = GeneralCommandLine(commandLineArgs).withWorkDirectory(workDir)
        val processOutput = ExecUtil.execAndGetOutput(generalCommandLine, 1000)

        if (!processOutput.isExitCodeSet) {
            when {
                processOutput.isTimeout -> {
                    return Result.failure(Throwable("Command timed out. (command=$generalCommandLine)"))
                }

                processOutput.isCancelled -> {
                    return Result.failure(Throwable("Command was cancelled. (command=$generalCommandLine)"))
                }
            }
        }

        if (processOutput.exitCode != 0) {
            val stderr = processOutput.stderr
            val parsedError = MiseCommandLineException.parseFromStderr(generalCommandLine, stderr)
            if (parsedError == null) {
                LOG.error("Failed to parse error. (exitCode=${processOutput.exitCode}, command=$generalCommandLine)\n$stderr")
                return Result.failure(Throwable(stderr))
            } else {
                return Result.failure(parsedError)
            }
        }

        val result = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
            .fromJson(processOutput.stdout, object : TypeToken<T>() {})

        return Result.success(result)
    }
}
