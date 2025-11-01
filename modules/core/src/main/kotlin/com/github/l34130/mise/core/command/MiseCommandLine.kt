package com.github.l34130.mise.core.command

import com.fasterxml.jackson.core.type.TypeReference
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.l34130.mise.core.setting.MiseApplicationSettings
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.application
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class MiseCommandLine(
    private val workDir: String? = null,
    private val configEnvironment: String? = null,
) {
    @RequiresBackgroundThread
    inline fun <reified T> runCommandLine(params: List<String>): Result<T> {
        val typeReference = object : TypeReference<T>() {}
        return runCommandLine(params, typeReference)
    }

    suspend inline fun <reified T> runCommandLineAsync(params: List<String>): Result<T> {
        val typeReference = object : TypeReference<T>() {}
        return runCommandLineAsync(params, typeReference)
    }

    @RequiresBackgroundThread
    fun <T> runCommandLine(
        params: List<String>,
        typeReference: TypeReference<T>,
    ): Result<T> {
        val rawResult = runRawCommandLine(params)
        return rawResult.fold(
            onSuccess = { output -> Result.success(MiseCommandLineOutputParser.parse(output, typeReference)) },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun <T> runCommandLineAsync(
        params: List<String>,
        typeReference: TypeReference<T>,
    ): Result<T> =
        withContext(Dispatchers.IO) {
            val rawResult = runRawCommandLine(params)
            rawResult.fold(
                onSuccess = { output -> Result.success(MiseCommandLineOutputParser.parse(output, typeReference)) },
                onFailure = { Result.failure(it) },
            )
        }

    @RequiresBackgroundThread
    fun runRawCommandLine(params: List<String>): Result<String> {
        val miseVersion = getMiseVersion()

        val executablePath = application.service<MiseApplicationSettings>().state.executablePath
        val commandLineArgs = executablePath.split(' ').toMutableList()

        if (!configEnvironment.isNullOrBlank()) {
            if (miseVersion >= MiseVersion(2024, 12, 2)) {
                commandLineArgs.add("--env")
                commandLineArgs.add(configEnvironment)
            } else {
                commandLineArgs.add("--profile")
                commandLineArgs.add(configEnvironment)
            }
        }

        commandLineArgs.addAll(params)
        return runCommandLineInternal(commandLineArgs)
    }

    @RequiresBackgroundThread
    private fun runCommandLineInternal(commandLineArgs: List<String>): Result<String> {
        val generalCommandLine = GeneralCommandLine(commandLineArgs).withWorkDirectory(workDir)
        val processOutput =
            try {
                logger.debug("Running command: $commandLineArgs")
                ExecUtil.execAndGetOutput(generalCommandLine, 3000)
            } catch (e: ExecutionException) {
                logger.info("Failed to execute command. (command=$generalCommandLine)", e)
                return Result.failure(
                    MiseCommandLineNotFoundException(
                        generalCommandLine,
                        e.message ?: "Failed to execute command.",
                        e,
                    ),
                )
            }

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
                logger.info("Failed to parse error from stderr. (stderr=$stderr)")
                return Result.failure(Throwable(stderr))
            } else {
                logger.debug("Parsed error from stderr. (error=$parsedError)")
                return Result.failure(parsedError)
            }
        }

        logger.debug("Command executed successfully. (command=$generalCommandLine)")
        return Result.success(processOutput.stdout)
    }

    companion object {
        private val commandCache =
            Caffeine
                .newBuilder()
                .expireAfterWrite(5.seconds.toJavaDuration())
                .build<String, Any>()

        @RequiresBackgroundThread
        fun getMiseVersion(): MiseVersion {
            val cached: MiseVersion? = commandCache.getIfPresent("version") as? MiseVersion
            if (cached != null) return cached

            val miseCommandLine = MiseCommandLine()
            val miseExecutable = application.service<MiseApplicationSettings>().state.executablePath
            val versionString = miseCommandLine.runCommandLineInternal(listOf(miseExecutable, "version"))

            val miseVersion =
                versionString.fold(
                    onSuccess = {
                        MiseVersion.parse(it)
                    },
                    onFailure = { _ ->
                        MiseVersion(0, 0, 0)
                    },
                )

            commandCache.put("version", miseVersion)
            return miseVersion
        }

        private val logger = Logger.getInstance(MiseCommandLine::class.java)
    }
}
