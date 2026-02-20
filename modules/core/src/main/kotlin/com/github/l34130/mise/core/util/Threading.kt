package com.github.l34130.mise.core.util

import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.ApplicationManager

fun <T> tryComputeReadAction(block: () -> T): T? {
    val app = ApplicationManager.getApplication() as? ApplicationEx ?: return null

    var result: T? = null
    val ok = app.tryRunReadAction { result = block() }
    return if (ok) result else null
}
