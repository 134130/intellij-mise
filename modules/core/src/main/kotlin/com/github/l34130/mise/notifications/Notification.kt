package com.github.l34130.mise.notifications

import com.github.l34130.mise.icons.PluginIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object Notification {
    fun notify(content :String, type: NotificationType, project: Project? = null) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(content, type)

        notification.icon = PluginIcons.Default
        notification.notify(project)
    }

    private const val NOTIFICATION_GROUP_ID = "Mise"
}
