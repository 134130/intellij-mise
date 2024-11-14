package com.github.l34130.mise.core.notification

import com.github.l34130.mise.core.icon.MiseIcons
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class NotificationService(
    private val project: Project,
) {
    fun info(
        title: String,
        htmlText: String,
        action: NotificationAction? = null,
    ) {
        showNotification(title, htmlText, NotificationType.INFORMATION, action)
    }

    fun warn(
        title: String,
        htmlText: String,
        action: NotificationAction? = null,
    ) {
        showNotification(title, htmlText, NotificationType.WARNING, action)
    }

    fun error(
        title: String,
        htmlText: String,
        action: NotificationAction? = null,
    ) {
        showNotification(title, htmlText, NotificationType.ERROR, action)
    }

    private fun showNotification(
        title: String,
        htmlText: String,
        type: NotificationType,
        action: NotificationAction? = null,
    ) {
        val notification =
            NotificationGroupManager
                .getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, htmlText, type)

        action?.let { notification.addAction(it) }

        notification.icon = MiseIcons.DEFAULT
        notification.notify(project)
    }

    companion object {
        private const val NOTIFICATION_GROUP_ID = "Mise"
    }
}
