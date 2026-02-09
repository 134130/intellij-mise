package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.openapi.project.Project

// Resolves tool config using mise output and reports non-mise errors via notifications.
internal class ToolResolutionService {
    fun resolve(
        project: Project,
        configEnvironment: String?,
        devToolName: MiseDevToolName,
    ): ToolResolution {
        val toolsResult =
            MiseCommandLineHelper.getDevTools(
                project = project,
                workDir = project.guessMiseProjectPath(),
                configEnvironment = configEnvironment,
            )
        val tools =
            toolsResult.fold(
                onSuccess = { tools -> tools[devToolName] },
                onFailure = {
                    if (it !is MiseCommandLineNotFoundException) {
                        MiseNotificationServiceUtils.notifyException("Failed to load dev tools", it, project)
                    }
                    emptyList()
                },
            )

        if (tools.isNullOrEmpty()) {
            return ToolResolution.None
        }

        if (tools.size > 1) {
            return ToolResolution.Multiple
        }

        return ToolResolution.Single(tools.first())
    }
}

internal sealed interface ToolResolution {
    data object None : ToolResolution

    data object Multiple : ToolResolution

    data class Single(
        val tool: MiseDevTool,
    ) : ToolResolution
}
