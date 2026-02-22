package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.command.MiseCommandLineExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput

internal fun processOutput(
    stdout: String = "",
    stderr: String = "",
    exitCode: Int = 0,
): ProcessOutput {
    val output = ProcessOutput()
    output.appendStdout(stdout)
    output.appendStderr(stderr)
    output.exitCode = exitCode
    return output
}

internal fun <T> withMiseCommandLineExecutor(
    executor: (GeneralCommandLine, Int) -> ProcessOutput = { _, _ -> processOutput() },
    block: () -> T,
): T {
    val previous = MiseCommandLine.commandLineExecutor
    MiseCommandLine.commandLineExecutor =
        MiseCommandLineExecutor { generalCommandLine, timeout ->
            executor(generalCommandLine, timeout)
        }
    return try {
        block()
    } finally {
        MiseCommandLine.commandLineExecutor = previous
    }
}
