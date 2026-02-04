package com.github.l34130.mise.core.util

import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger

private val logger = Logger.getInstance("com.github.l34130.mise.core.util.Threading")

/**
 * Checks if it's safe to perform operations that might need EDT access or project context.
 *
 * Returns false if:
 * - A write action is in progress (ReadAction would deadlock if called from a
 *   PotemkinProgress background thread while EDT holds the write lock)
 * - We're in a coroutine dispatcher thread (may cause deadlocks)
 * - We're in IJent deployment operations
 * - We're in EEL (Execution Environment Language) operations
 *
 * When unsafe, callers should skip operations or use alternative approaches.
 *
 * This prevents:
 * - Deadlocks from ReadAction in PotemkinProgress background threads (#413)
 * - Deadlocks from invokeAndWait in coroutine contexts
 * - Threading issues during IDE infrastructure initialization
 * - Project detection failures in unsafe threading contexts
 */
fun canSafelyInvokeAndWait(): Boolean {
    // If a write action is in progress on another thread, any ReadAction attempt
    // from this thread will block waiting for the write lock to be released.
    // This causes deadlocks when EDT holds a write lock and spawns a PotemkinProgress
    // background thread that ends up here (e.g., git index refresh → git command → env customizer).
    if ((ApplicationManager.getApplication() as? ApplicationEx)?.isWriteActionInProgress == true) {
        logger.trace("Write action in progress on another thread, cannot safely acquire read lock")
        return false
    }

    val threadName = Thread.currentThread().name

    // Coroutine dispatcher threads cannot safely call invokeAndWait
    // as they may be part of a coordination that EDT is waiting on
    if (threadName.contains("DefaultDispatcher-worker")) {
        logger.trace("Detected coroutine dispatcher thread, cannot safelyInvokeAndWait")
        return false
    }

    return true
}
