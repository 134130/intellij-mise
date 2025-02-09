package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.execution.RunMiseTomlTaskAction
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.toolwindow.ActionOnRightClick
import com.github.l34130.mise.core.toolwindow.DoubleClickable
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.util.OpenSourceUtil
import kotlinx.coroutines.runBlocking
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
    DoubleClickable,
    ActionOnRightClick {
    override fun displayName(): String = taskInfo.name

    override fun createPresentation(): PresentationData =
        runBlocking {
            readAction {
                PresentationData().apply {
                    tooltip = taskInfo.description ?: "<i>No description provided.</i>"
                }
            }
        }

    override fun onDoubleClick(event: MouseEvent) {
        val action = RunMiseTomlTaskAction(taskInfo)
        val actionEvent =
            AnActionEvent.createFromAnAction(
                action,
                event,
                ActionPlaces.TOOLWINDOW_CONTENT,
                DataManager.getInstance().getDataContext(event.getComponent()),
            )

        action.actionPerformed(actionEvent)
    }

    override fun actions(): List<AnAction> =
        listOfNotNull(
            RunMiseTomlTaskAction(taskInfo),
            when (taskInfo) {
                is MiseTask.ShellScript -> {
                    object : AnAction("Go to Declaration", "Go to Declaration", AllIcons.General.Locate) {
                        override fun actionPerformed(e: AnActionEvent) {
                            OpenSourceUtil.navigateToSource(false, true, taskInfo.file.findPsiFile(project))
                        }
                    }
                }
                is MiseTask.TomlTable -> {
                    object : AnAction("Go to Declaration", "Go to Declaration", AllIcons.General.Locate) {
                        override fun actionPerformed(e: AnActionEvent) {
                            OpenSourceUtil.navigateToSource(false, true, taskInfo.keySegment)
                        }
                    }
                }
                is MiseTask.Unknown -> null
            },
        )
}
