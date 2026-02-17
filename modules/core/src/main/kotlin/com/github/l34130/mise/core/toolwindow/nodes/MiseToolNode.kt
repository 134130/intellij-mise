package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.toolwindow.NonProjectPathDisplay
import com.github.l34130.mise.core.toolwindow.displayPath
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.InplaceCommentAppender
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes

class MiseToolServiceNode(
    project: Project,
    val tools: Collection<AbstractTreeNode<*>>,
) : MiseNode<String>(
        project,
        "Tools",
        AllIcons.Nodes.ConfigFolder,
    ) {
    override fun getChildren(): Collection<AbstractTreeNode<*>> = tools
}

class MiseToolConfigDirectoryNode(
    project: Project,
    val configDirPath: String,
    private val nonProjectPathDisplay: NonProjectPathDisplay,
    val tools: List<Pair<MiseDevToolName, MiseDevTool>>,
) : MiseNode<String>(
        project,
        configDirPath,
        null,
    ) {
    override fun displayName(): String = displayPath(project, configDirPath, nonProjectPathDisplay)

    override fun getChildren(): Collection<MiseToolNode> =
        tools
            .map {
                MiseToolNode(
                    project = project,
                    toolName = it.first,
                    nonProjectPathDisplay = nonProjectPathDisplay,
                    toolInfo = it.second,
                )
            }
}

class MiseToolNode(
    project: Project,
    val toolName: MiseDevToolName,
    private val nonProjectPathDisplay: NonProjectPathDisplay,
    val toolInfo: MiseDevTool,
) : MiseLeafNode<MiseDevTool>(
        project,
        toolInfo,
        AllIcons.General.Gear,
) {
    override fun displayName(): String = "${toolName.value}@${toolInfo.shimsVersion()}"

    override fun appendInplaceComments(appender: InplaceCommentAppender) {
        val sourcePath = toolInfo.source?.absolutePath ?: return
        appender.append(" ${displayPath(project, sourcePath, nonProjectPathDisplay)}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }

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

class MiseToolResolvedContextNode(
    project: Project,
    private val workDir: String,
    private val nonProjectPathDisplay: NonProjectPathDisplay,
    private val tools: List<Pair<MiseDevToolName, MiseDevTool>>,
) : MiseNode<String>(
        project,
        workDir,
        AllIcons.Nodes.Folder,
    ) {
    override fun displayName(): String = "Resolved from ${displayPath(project, workDir, nonProjectPathDisplay)}"

    override fun getChildren(): Collection<MiseToolNode> =
        tools.map { (toolName, toolInfo) ->
            MiseToolNode(
                project = project,
                toolName = toolName,
                nonProjectPathDisplay = nonProjectPathDisplay,
                toolInfo = toolInfo,
            )
        }
}
