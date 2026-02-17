package com.github.l34130.mise.core.toolwindow

import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MiseToolWindowContextResolver(
    private val project: Project,
) {
    fun resolve(): MiseToolWindowContext {
        val state = project.service<MiseToolWindowState>().state

        val workDir = project.guessMiseProjectPath()

        val configEnvironment =
            state.envOverride

        return MiseToolWindowContext(
            workDir = workDir,
            configEnvironment = configEnvironment,
        )
    }
}

data class MiseToolWindowContext(
    val workDir: String,
    val configEnvironment: String,
)
