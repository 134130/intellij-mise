package com.github.l34130.mise.core.notifications

import com.github.l34130.mise.core.icons.MiseIcons
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import kotlin.reflect.KClass

object Notification {
    fun notify(
        content: String,
        type: NotificationType,
        project: Project? = null,
    ) {
        val notification =
            NotificationGroupManager
                .getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content.replace("\n", "<br>"), type)

        notification.icon = MiseIcons.DEFAULT
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

        notification.icon = MiseIcons.DEFAULT
        notification.notify(project)
    }

    fun notifyWithAction(
        content: String,
        type: NotificationType,
        project: Project? = null,
        actionName: String,
        action: Runnable,
    ) {
        val notification =
            NotificationGroupManager
                .getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(content.replace("\n", "<br>"), type)
                .addAction(
                    NotificationAction.createSimple(actionName) {
                        action.run()
                    },
                )

        notification.icon = MiseIcons.DEFAULT
        notification.notify(project)
    }

    private const val NOTIFICATION_GROUP_ID = "Mise"
}
