package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.toolwindow.NonProjectPathDisplay
import com.github.l34130.mise.core.toolwindow.displayPath
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes

class MiseEnvironmentServiceNode(
    project: Project,
    val environments: Collection<AbstractTreeNode<*>>,
) : MiseNode<String>(
        project,
        "Environment variables",
        AllIcons.Nodes.ConfigFolder,
    ) {
    override fun getChildren(): Collection<AbstractTreeNode<*>> = environments
}

class MiseEnvironmentConfigDirectoryNode(
    project: Project,
    private val configDirPath: String,
    private val nonProjectPathDisplay: NonProjectPathDisplay,
    private val environments: Collection<MiseEnvironmentNode>,
) : MiseNode<String>(
        project,
        configDirPath,
        AllIcons.Nodes.Folder,
    ) {
    override fun displayName(): String = displayPath(project, configDirPath, nonProjectPathDisplay)

    override fun getChildren(): Collection<MiseEnvironmentNode> = environments
}

class MiseEnvironmentNode(
    project: Project,
    val key: String,
    val value: String,
    private val source: String? = null,
    private val tool: String? = null,
) : MiseLeafNode<Pair<String, String>>(
        project,
        Pair(key, value),
        null,
    ) {
    override fun displayName(): String = key

    override fun update(presentation: PresentationData) {
        presentation.addText(key, SimpleTextAttributes.REGULAR_ATTRIBUTES)

        val compactValue =
            if (value.length > 120) {
                "${value.take(64)}...${value.takeLast(24)}"
            } else {
                value
            }
        presentation.addText(" = $compactValue", SimpleTextAttributes.GRAYED_ATTRIBUTES)

        val metaParts = listOfNotNull(source?.takeIf { it.isNotBlank() }, tool?.takeIf { it.isNotBlank() })
        if (metaParts.isNotEmpty()) {
            presentation.addText("  [${metaParts.joinToString(" | ")}]", SimpleTextAttributes.GRAY_ATTRIBUTES)
        }
        presentation.tooltip = value
    }
}
