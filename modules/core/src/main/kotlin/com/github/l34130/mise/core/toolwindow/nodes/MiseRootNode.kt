package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.command.MiseCmd
import com.github.l34130.mise.core.command.MiseTool
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
        val toolsByToolNames = MiseCmd.loadTools(nodeProject.basePath, settings.state.miseProfile, project)

        val toolsBySourcePaths = mutableMapOf<String, MutableList<Pair<String, MiseTool>>>()
        for ((toolName, toolInfos) in toolsByToolNames.entries) {
            for (toolInfo in toolInfos) {
                val sourcePath = toolInfo.source?.path ?: continue
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
        val envs = MiseCmd.loadEnv(nodeProject.basePath, settings.state.miseProfile, project)

        return envs.map { (key, value) ->
            MiseEnvironmentNode(
                project = nodeProject,
                key = key,
                value = value,
            )
        }
    }

    private fun getTaskNodes(settings: MiseSettings): Collection<MiseTaskNode> {
        val tasks = MiseCmd.loadTasks(nodeProject.basePath, settings.state.miseProfile, project)

        return tasks.map { task ->
            MiseTaskNode(
                project = nodeProject,
                taskInfo = task,
            )
        }
    }
}
