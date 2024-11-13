package com.github.l34130.mise.toolwindow.nodes

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.commands.MiseTool
import com.github.l34130.mise.settings.MiseSettings
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

        return getToolNodes(settings)
    }

    private fun getToolNodes(settings: MiseSettings): Collection<AbstractTreeNode<*>> {
        val toolsByToolNames = MiseCmd.loadTools(nodeProject.basePath, settings.state.miseProfile)

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
}
