package com.github.l34130.mise.core.notification

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.l34130.mise.core.icon.MiseIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service(Service.Level.PROJECT)
class MiseNotificationService(
    private val project: Project,
) {
    fun info(
        title: String,
        htmlText: String,
        actionProvider: (() -> NotificationAction)? = null,
    ) {
        if (actionProvider == null) {
            showNotification(title, htmlText, NotificationType.INFORMATION)
        } else {
            showNotification(title, htmlText, NotificationType.INFORMATION) { notification ->
                notification.addAction(actionProvider())
            }
        }
    }

    fun info(
        title: String,
        htmlText: String,
        actionConfigurer: (Notification) -> Unit,
    ) {
        // Use this overload when multiple actions are needed.
        showNotification(title, htmlText, NotificationType.INFORMATION, actionConfigurer)
    }

    fun warn(
        title: String,
        htmlText: String,
        actionProvider: (() -> NotificationAction)? = null,
    ) {
        if (actionProvider == null) {
            showNotification(title, htmlText, NotificationType.WARNING)
        } else {
            showNotification(title, htmlText, NotificationType.WARNING) { notification ->
                notification.addAction(actionProvider())
            }
        }
    }

    fun warn(
        title: String,
        htmlText: String,
        actionConfigurer: (Notification) -> Unit,
    ) {
        // Use this overload when multiple actions are needed.
        showNotification(title, htmlText, NotificationType.WARNING, actionConfigurer)
    }

    fun error(
        title: String,
        htmlText: String,
        actionProvider: (() -> NotificationAction)? = null,
    ) {
        if (actionProvider == null) {
            showNotification(title, htmlText, NotificationType.ERROR)
        } else {
            showNotification(title, htmlText, NotificationType.ERROR) { notification ->
                notification.addAction(actionProvider())
            }
        }
    }

    fun error(
        title: String,
        htmlText: String,
        actionConfigurer: (Notification) -> Unit,
    ) {
        // Use this overload when multiple actions are needed.
        showNotification(title, htmlText, NotificationType.ERROR, actionConfigurer)
    }

    private fun showNotification(
        title: String,
        htmlText: String,
        type: NotificationType,
        actionConfigurer: ((Notification) -> Unit)? = null,
    ) {
        if (debounceMap.getIfPresent(title) != null) {
            // Debounce duplicate notifications
            return
        }
        debounceMap.put(title, Any())

        val notification =
            NotificationGroupManager
                .getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, htmlText, type)

        actionConfigurer?.invoke(notification)

        notification.icon = MiseIcons.DEFAULT
        notification.notify(project)
    }

    companion object {
        private const val NOTIFICATION_GROUP_ID = "Mise"
        private val debounceMap: Cache<String, Any> =
            Caffeine
                .newBuilder()
                .expireAfterWrite(2.seconds.toJavaDuration())
                .build()

        fun getInstance(project: Project): MiseNotificationService = project.service()
    }
}
