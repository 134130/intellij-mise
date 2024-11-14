package com.github.l34130.mise.toolwindow.nodes

import com.github.l34130.mise.commands.MiseTask
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project

class MiseTaskServiceNode(
    project: Project,
    val tasks: Collection<AbstractTreeNode<*>>,
) : MiseNode<String>(
        project,
        "Tasks",
        AllIcons.Nodes.ConfigFolder,
    ) {
    override fun getChildren(): Collection<AbstractTreeNode<*>> = tasks
}

class MiseTaskNode(
    project: Project,
    val taskInfo: MiseTask,
) : MiseLeafNode<MiseTask>(
        project,
        taskInfo,
        AllIcons.Debugger.Console,
    ) {
    override fun displayName(): String = taskInfo.name

    override fun createPresentation(): PresentationData =
        PresentationData().apply {
            tooltip =
                buildString {
                    append(taskInfo.description)
                    taskInfo.depends?.let {
                        append("<br>${it.joinToString(prefix = "[", postfix = "]")}")
                    }
                }
        }
}
