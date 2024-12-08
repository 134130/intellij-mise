package com.github.l34130.mise.core.notification

import com.github.l34130.mise.core.command.MiseCommandLineException
import com.intellij.openapi.project.Project

object MiseNotificationServiceUtils {
    fun notifyException(
        title: String,
        throwable: Throwable,
        project: Project? = null,
    ) {
        val notificationService = MiseNotificationService.getInstance(project)
        when (throwable) {
            is MiseCommandLineException -> notificationService.warn(title, throwable.message)
            else -> notificationService.error(title, throwable.message ?: throwable.javaClass.simpleName)
        }
    }
}
