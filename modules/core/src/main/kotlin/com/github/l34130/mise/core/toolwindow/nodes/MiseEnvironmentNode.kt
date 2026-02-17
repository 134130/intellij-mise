package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.toolwindow.NonProjectPathDisplay
import com.github.l34130.mise.core.toolwindow.displayPath
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project

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
) : MiseLeafNode<Pair<String, String>>(
        project,
        Pair(key, value),
        null,
    ) {
    override fun displayName(): String = "$key = $value"
}
