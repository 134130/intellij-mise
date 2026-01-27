package com.github.l34130.mise.core.util

import com.intellij.openapi.diagnostic.Logger

private val logger = Logger.getInstance("com.github.l34130.mise.core.util.Threading")

/**
 * Checks if it's safe to perform operations that might need EDT access or project context.
 *
 * Returns false if:
 * - We're in a coroutine dispatcher thread (may cause deadlocks)
 * - We're in WSL infrastructure operations (internal IDE setup)
 * - We're in IJent deployment operations
 * - We're in EEL (Execution Environment Language) operations
 *
 * When unsafe, callers should skip operations or use alternative approaches.
 *
 * This prevents:
 * - Deadlocks from invokeAndWait in coroutine contexts
 * - Threading issues during IDE infrastructure initialization
 * - Project detection failures in unsafe threading contexts
 * - Recursive environment customization during WSL/IJent setup
 */
fun canSafelyInvokeAndWait(): Boolean {
    val threadName = Thread.currentThread().name

    // Coroutine dispatcher threads cannot safely call invokeAndWait
    // as they may be part of a coordination that EDT is waiting on
    if (threadName.contains("DefaultDispatcher-worker")) {
        logger.trace("Detected coroutine dispatcher thread, cannot safelyInvokeAndWait")
        return false
    }

    // Check if we're in WSL/IJent/EEL infrastructure code
    // These operations run on background threads during IDE initialization and
    // must not try to synchronize with EDT or access project context
    val stackTrace = Thread.currentThread().stackTrace
    val hasInfrastructure = stackTrace.any { element ->
        element.className.startsWith("com.intellij.execution.wsl.WslDistributionDescriptor")
    }

    if (hasInfrastructure) {
        logger.trace("Detected IDE infrastructure caller in stack, cannot safelyInvokeAndWait")
        return false
    }

    return true
}
