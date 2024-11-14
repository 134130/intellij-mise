package com.github.l34130.mise.toolwindow.nodes

import com.github.l34130.mise.commands.MiseTool
import com.github.l34130.mise.utils.ToolUtils
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil

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
    val tools: List<Pair<String, MiseTool>>,
) : MiseNode<String>(
        project,
        configDirPath,
        null,
    ) {
    override fun displayName(): String = FileUtil.getLocationRelativeToUserHome(configDirPath)

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
    val toolName: String,
    val toolInfo: MiseTool,
) : MiseLeafNode<MiseTool>(
        project,
        toolInfo,
        AllIcons.General.Gear,
    ) {
    override fun displayName(): String = "$toolName@${toolInfo.version}"

    override fun isActive(): Boolean = toolInfo.active

    override fun createPresentation(): PresentationData =
        PresentationData().apply {
            tooltip =
                buildString {
                    if (!toolInfo.installed && !toolInfo.active) {
                        append("${ToolUtils.getCanonicalName(toolName)} is inactivated because it is not installed.")
                    }
                }
        }
}
