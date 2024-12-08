package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.command.MiseCommandLineException
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.NotificationService
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
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
        val notificationService = nodeProject.service<NotificationService>()

        val toolsByToolNames = MiseCommandLineHelper.getDevTools(
            workDir = nodeProject.basePath,
            profile = settings.state.miseProfile
        ).fold(
            onSuccess = { tools -> tools },
            onFailure = {
                when (it) {
                    is MiseCommandLineException -> {
                        notificationService.warn("Failed to load dev tools", it.message)
                    }

                    else -> {
                        notificationService.error("Failed to load dev tools", it.message ?: it.javaClass.simpleName)
                    }
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
        val envs =
            MiseCommandLine(project).loadEnvironmentVariables(settings.state.miseProfile)

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
            profile = settings.state.miseProfile
        ).fold(
            onSuccess = { tasks -> tasks },
            onFailure = {
                val notificationService = nodeProject.service<NotificationService>()

                when (it) {
                    is MiseCommandLineException -> {
                        notificationService.warn("Failed to load tasks", it.message)
                    }

                    else -> {
                        notificationService.error("Failed to load tasks", it.message ?: it.javaClass.simpleName)
                    }
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
