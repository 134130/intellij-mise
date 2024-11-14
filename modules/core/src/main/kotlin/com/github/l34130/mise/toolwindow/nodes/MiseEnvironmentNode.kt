package com.github.l34130.mise.toolwindow.nodes

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

class MiseEnvironmentServiceNode(
    project: Project,
    val environments: Collection<MiseEnvironmentNode>,
) : MiseNode<String>(
        project,
        "Environment variables",
        AllIcons.Nodes.ConfigFolder,
    ) {
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
