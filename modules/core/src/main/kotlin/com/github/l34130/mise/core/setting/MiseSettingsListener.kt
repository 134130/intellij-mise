package com.github.l34130.mise.core.setting

import com.intellij.util.messages.Topic

interface MiseSettingsListener {
    companion object {
        val TOPIC: Topic<MiseSettingsListener> = Topic(MiseSettingsListener::class.java, Topic.BroadcastDirection.NONE)
    }
}
