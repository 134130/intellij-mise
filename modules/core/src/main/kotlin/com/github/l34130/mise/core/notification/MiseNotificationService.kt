package com.github.l34130.mise.core.notification

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.l34130.mise.core.icon.MiseIcons
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
        showNotification(title, htmlText, NotificationType.INFORMATION, actionProvider)
    }

    fun warn(
        title: String,
        htmlText: String,
        actionProvider: (() -> NotificationAction)? = null,
    ) {
        showNotification(title, htmlText, NotificationType.WARNING, actionProvider)
    }

    fun error(
        title: String,
        htmlText: String,
        actionProvider: (() -> NotificationAction)? = null,
    ) {
        showNotification(title, htmlText, NotificationType.ERROR, actionProvider)
    }

    private fun showNotification(
        title: String,
        htmlText: String,
        type: NotificationType,
        actionProvider: (() -> NotificationAction)? = null,
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

        actionProvider?.let { notification.addAction(it()) }

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
