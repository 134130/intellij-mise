package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project

class MiseRootNode(
    private val nodeProject: Project,
) : AbstractTreeNode<Any>(nodeProject, Object()) {
    override fun update(presentation: PresentationData) {
        presentation.presentableText = "Mise"
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val settings = MiseSettings.getService(nodeProject)

        return listOf(
            MiseToolServiceNode(project, getToolNodes(settings)),
            MiseTaskServiceNode(project, getTaskNodes(settings)),
            MiseEnvironmentServiceNode(project, getEnvironmentNodes(settings)),
        )
    }

    private fun getToolNodes(settings: MiseSettings): Collection<MiseToolConfigDirectoryNode> {
        val toolsByToolNames = MiseCommandLineHelper.getDevTools(
            workDir = nodeProject.basePath,
            configEnvironment = settings.state.miseConfigEnvironment
        ).fold(
            onSuccess = { tools -> tools },
            onFailure = {
                if (it !is MiseCommandLineNotFoundException) {
                    MiseNotificationServiceUtils.notifyException("Failed to load dev tools", it)
                }
                emptyMap()
            }
        )

        val toolsBySourcePaths = mutableMapOf<String, MutableList<Pair<MiseDevToolName, MiseDevTool>>>()
        for ((toolName, toolInfos) in toolsByToolNames.entries) {
            for (toolInfo in toolInfos) {
                val sourcePath = toolInfo.source?.absolutePath ?: continue
                val tools = toolsBySourcePaths.getOrPut(sourcePath) { mutableListOf() }
                tools.add(toolName to toolInfo)
            }
        }

        return toolsBySourcePaths.map { (sourcePath, tools) ->
            MiseToolConfigDirectoryNode(
                project = nodeProject,
                configDirPath = sourcePath,
                tools = tools,
            )
        }
    }

    private fun getEnvironmentNodes(settings: MiseSettings): Collection<MiseEnvironmentNode> {
        val envs = MiseCommandLineHelper.getEnvVars(
            workDir = nodeProject.basePath,
            configEnvironment = settings.state.miseConfigEnvironment
        ).fold(
            onSuccess = { envs -> envs },
            onFailure = {
                if (it !is MiseCommandLineNotFoundException) {
                    MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it)
                }
                emptyMap()
            }
        )

        return envs.map { (key, value) ->
            MiseEnvironmentNode(
                project = nodeProject,
                key = key,
                value = value,
            )
        }
    }

    private fun getTaskNodes(settings: MiseSettings): Collection<MiseTaskNode> {
        val tasks = MiseCommandLineHelper.getTasks(
            workDir = nodeProject.basePath,
            configEnvironment = settings.state.miseConfigEnvironment
        ).fold(
            onSuccess = { tasks -> tasks },
            onFailure = {
                if (it !is MiseCommandLineNotFoundException) {
                    MiseNotificationServiceUtils.notifyException("Failed to load tasks", it)
                }
                emptyList()
            }
        )

        return tasks.map { task ->
            MiseTaskNode(
                project = nodeProject,
                taskInfo = task,
            )
        }
    }
}
