package com.github.l34130.mise.core.command

import com.fasterxml.jackson.core.type.TypeReference
import com.github.l34130.mise.core.command.MiseCommandLineHelper.environmentSkipCustomization
import com.github.l34130.mise.core.command.MiseEnvCacheKeyService.Companion.MISE_ENV_CACHE_KEY
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.execution.ParametersListUtil

internal fun interface MiseCommandLineExecutor {
    @Throws(ExecutionException::class)
    fun execute(generalCommandLine: GeneralCommandLine, timeout: Int): ProcessOutput
}

internal class MiseCommandLine(
    private val project: Project,
    workDir: String? = null,
    private val configEnvironment: String? = null,
) {
    private val workDir: String = workDir?.takeIf { it.isNotBlank() } ?: project.guessMiseProjectPath()

    @RequiresBackgroundThread
    inline fun <reified T> runCommandLine(
        params: List<String>,
        noinline parser: ((String) -> T)? = null,
    ): Result<T> {
        val typeReference = object : TypeReference<T>() {}
        return runCommandLine(params, typeReference, parser)
    }

    @RequiresBackgroundThread
    inline fun <reified T> runCommandLine(
        params: List<String>,
        typeReference: TypeReference<T>,
        noinline parser: ((String) -> T)? = null,
    ): Result<T> {
        val rawResult = runRawCommandLine(params)
        return rawResult.fold(
            onSuccess = { output ->
                if (T::class == Unit::class) {
                    Result.success(Unit as T)
                } else {
                    val parsed = parser?.invoke(output)
                        ?: MiseCommandLineOutputParser.parse(output, typeReference)
                    Result.success(parsed)
                }
            },
            onFailure = { Result.failure(it) },
        )
    }

    @RequiresBackgroundThread
    fun runRawCommandLine(params: List<String>): Result<String> {
        logger.debug("==> [COMMAND] Starting command execution (workDir: $workDir, params: $params)")

        // Determine the executable path with project override support
        val executablePath = determineExecutablePath()

        // Build command line arguments
        val commandLineArgs = mutableListOf<String>()

        // Handle executable path that may contain spaces.
        commandLineArgs.addAll(ParametersListUtil.parse(executablePath))

        // Add mise configuration environment parameter
        if (!configEnvironment.isNullOrBlank()) {
            val miseVersion = project.service<MiseExecutableManager>().getExecutableVersion() ?: MiseVersion(0, 0, 0)
            if (miseVersion >= MiseVersion(2024, 12, 2)) {
                commandLineArgs.add("--env")
                commandLineArgs.add(configEnvironment)
            } else {
                commandLineArgs.add("--profile")
                commandLineArgs.add(configEnvironment)
            }
        }

        // Add user-provided parameters
        commandLineArgs.addAll(params)

        return runCommandLineInternal(commandLineArgs, workDir)
    }

    /**
     * Determine which mise executable to use.
     * Delegates to MiseExecutableManager, which is the single source of truth.
     */
    private fun determineExecutablePath(): String {
        val executableManager = project.service<MiseExecutableManager>()
        val path = executableManager.getExecutablePath()
        logger.debug("==> [EXECUTABLE] Using path: $path (workDir: $workDir)")
        return path
    }

    @RequiresBackgroundThread
    private fun runCommandLineInternal(
        commandLineArgs: List<String>,
        workingDir: String = this.workDir,
    ): Result<String> {
        val generalCommandLine = GeneralCommandLine(commandLineArgs).withWorkDirectory(workingDir.ifBlank { null })
        environmentSkipCustomization(generalCommandLine.environment)
        injectMiseEnvCacheEnvironment(generalCommandLine.environment)
        return runCommandLineInternal(generalCommandLine)
    }

    private fun injectMiseEnvCacheEnvironment(environment: MutableMap<String?, String?>) {
        if (environment[MISE_ENV_CACHE_KEY].isNullOrBlank()) {
            environment[MISE_ENV_CACHE_KEY] = project.service<MiseEnvCacheKeyService>().sessionKey
        }
    }

    @RequiresBackgroundThread
    private fun runCommandLineInternal(
        generalCommandLine: GeneralCommandLine,
    ): Result<String> {
        return executeCommandLine(generalCommandLine, allowedToFail = false, timeout = 3000)
            .map { it.stdout }
    }

    companion object {
        private val logger = Logger.getInstance(MiseCommandLine::class.java)
        private object DefaultCommandLineExecutor : MiseCommandLineExecutor {
            override fun execute(generalCommandLine: GeneralCommandLine, timeout: Int): ProcessOutput {
                return ExecUtil.execAndGetOutput(generalCommandLine, timeout)
            }
        }

        @Volatile
        internal var commandLineExecutor: MiseCommandLineExecutor = DefaultCommandLineExecutor

        /**
         * Execute a GeneralCommandLine and return the ProcessOutput.
         * This is the single source of truth for command execution.
         *
         * @param generalCommandLine The command line to execute
         * @param allowedToFail If true, failures are logged at debug level (for detection); if false, at info level
         * @param timeout Timeout in milliseconds (default 3000ms)
         * @return Result containing ProcessOutput on success, or exception on failure
         */
        @RequiresBackgroundThread
        fun executeCommandLine(
            generalCommandLine: GeneralCommandLine,
            allowedToFail: Boolean = false,
            timeout: Int = 3000
        ): Result<ProcessOutput> {
            logger.debug("==> [EXEC] ${generalCommandLine.commandLineString} (workDir: ${generalCommandLine.workDirectory})")

            val processOutput =
                try {
                    commandLineExecutor.execute(generalCommandLine, timeout)
                } catch (e: ExecutionException) {
                    if (!allowedToFail) {
                        logger.warn("Failed to execute command. (command=$generalCommandLine)", e)
                    } else {
                        logger.debug("Command failed, but is allowed to fail. (command=$generalCommandLine)", e)
                    }
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
                    if (!allowedToFail) {
                        logger.warn("Failed to parse error from stderr. (command=$generalCommandLine, stderr=$stderr)")
                    } else {
                        logger.debug("Command failed, but is allowed to fail. (command=$generalCommandLine, stderr=$stderr)")
                    }
                    return Result.failure(Throwable(stderr))
                } else {
                    if (!allowedToFail) {
                        logger.warn("Parsed error from stderr. (command=$generalCommandLine, error=$parsedError)")
                    } else {
                        logger.debug("Command failed, but is allowed to fail. (command=$generalCommandLine, error=$parsedError)")
                    }
                    return Result.failure(parsedError)
                }
            }

            logger.debug("Command executed successfully. (command=$generalCommandLine)")
            return Result.success(processOutput)
        }
    }
}
