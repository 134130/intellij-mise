package com.github.l34130.mise.notifications

import com.github.l34130.mise.icons.PluginIcons
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import kotlin.reflect.KClass

object Notification {
    fun notify(content: String, type: NotificationType, project: Project? = null) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(content.replace("\n", "<br>"), type)

        notification.icon = PluginIcons.Default
        notification.notify(project)
    }

    fun <T : Configurable> notifyWithLinkToSettings(
        content: String,
        configurableClass: KClass<out T>,
        type: NotificationType,
        project: Project? = null,
    ) {
        val notification =
            NotificationGroupManager
                .getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content.replace("\n", "<br>"), type)
                .addAction(
                    NotificationAction.createSimple("Configure") {
                        ShowSettingsUtil.getInstance().showSettingsDialog(
                            project,
                            configurableClass.javaObjectType,
                        )
                    },
                )

        notification.icon = PluginIcons.Default
        notification.notify(project)
    }

    private const val NOTIFICATION_GROUP_ID = "Mise"
}
