package com.github.l34130.mise.core.toolwindow

import com.intellij.openapi.actionSystem.AnAction

interface ActionOnRightClick {
    fun actions(): List<AnAction>
}
