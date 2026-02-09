package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.util.presentablePath
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project

class MiseToolServiceNode(
    project: Project,
    val tools: Collection<MiseToolConfigDirectoryNode>,
) : MiseNode<String>(
        project,
        "Tools",
        AllIcons.Nodes.ConfigFolder,
    ) {
    override fun getChildren(): Collection<MiseToolConfigDirectoryNode> = tools
}

class MiseToolConfigDirectoryNode(
    project: Project,
    val configDirPath: String,
    val tools: List<Pair<MiseDevToolName, MiseDevTool>>,
) : MiseNode<String>(
        project,
        configDirPath,
        null,
    ) {
    override fun displayName(): String = presentablePath(project, configDirPath)

    override fun getChildren(): Collection<MiseToolNode> =
        tools
            .map {
                MiseToolNode(
                    project = project,
                    toolName = it.first,
                    toolInfo = it.second,
                )
            }
}

class MiseToolNode(
    project: Project,
    val toolName: MiseDevToolName,
    val toolInfo: MiseDevTool,
) : MiseLeafNode<MiseDevTool>(
        project,
        toolInfo,
        AllIcons.General.Gear,
    ) {
    override fun displayName(): String = "${toolName.value}@${toolInfo.displayVersion}"

    override fun isActive(): Boolean = toolInfo.active

    override fun createPresentation(): PresentationData =
        PresentationData().apply {
            tooltip =
                buildString {
                    if (!toolInfo.installed && !toolInfo.active) {
                        append("${toolName.canonicalName()} is inactivated because it is not installed.")
                    }
                }
        }
}
