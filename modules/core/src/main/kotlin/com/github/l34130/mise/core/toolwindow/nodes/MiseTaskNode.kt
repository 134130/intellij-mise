package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.command.MiseRunAction
import com.github.l34130.mise.core.command.MiseTask
import com.github.l34130.mise.core.toolwindow.DoubleClickable
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import java.awt.event.MouseEvent

class MiseTaskServiceNode(
    project: Project,
    val tasks: Collection<MiseTaskNode>,
) : MiseNode<String>(
        project,
        "Tasks",
        AllIcons.Nodes.ConfigFolder,
    ) {
    override fun getChildren(): Collection<MiseTaskNode> = tasks
}

class MiseTaskNode(
    project: Project,
    val taskInfo: MiseTask,
) : MiseLeafNode<MiseTask>(
        project,
        taskInfo,
        AllIcons.Debugger.Console,
    ),
    DoubleClickable {
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

    override fun onDoubleClick(event: MouseEvent) {
        MiseRunAction.executeTask(project, taskInfo.name)
    }
}
