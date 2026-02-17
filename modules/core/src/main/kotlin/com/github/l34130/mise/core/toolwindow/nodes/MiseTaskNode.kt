package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.execution.RunMiseTomlTaskAction
import com.github.l34130.mise.core.model.MiseShellScriptTask
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.github.l34130.mise.core.model.MiseUnknownTask
import com.github.l34130.mise.core.toolwindow.ActionOnRightClick
import com.github.l34130.mise.core.toolwindow.DoubleClickable
import com.github.l34130.mise.core.toolwindow.NonProjectPathDisplay
import com.github.l34130.mise.core.toolwindow.displayPath
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.InplaceCommentAppender
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.OpenSourceUtil
import kotlinx.coroutines.Dispatchers
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap

class MiseTaskServiceNode(
    project: Project,
    val nodes: Collection<AbstractTreeNode<*>>,
) : MiseNode<String>(
        project,
        "Tasks",
        AllIcons.Nodes.ConfigFolder,
    ) {
    override fun getChildren(): Collection<AbstractTreeNode<*>> = nodes
}

class MiseTaskDirectoryNode(
    project: Project,
    val directoryPath: String,
    val directoryName: String,
    private val nonProjectPathDisplay: NonProjectPathDisplay,
    val parent: MiseTaskDirectoryNode?,
    val children: MutableList<MiseTaskDirectoryNode>,
    val tasks: MutableList<MiseTaskNode>,
) : MiseNode<String>(
        project,
        directoryPath,
        AllIcons.Nodes.Folder,
    ) {
    override fun displayName(): String {
        if (parent != null) return directoryName
        return displayPath(project, directoryPath, nonProjectPathDisplay)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> = children + tasks
}

class MiseTaskNode(
    project: Project,
    val parent: MiseTaskDirectoryNode?,
    private val nonProjectPathDisplay: NonProjectPathDisplay,
    val taskInfo: MiseTask,
) : MiseLeafNode<MiseTask>(
        project,
        taskInfo,
        AllIcons.Debugger.Console,
    ),
    DoubleClickable,
    ActionOnRightClick {
    override fun displayName(): String = taskInfo.name

    override fun appendInplaceComments(appender: InplaceCommentAppender) {
        val parentDirectory = parent?.directoryPath
        val pathText =
            if (parentDirectory != null) {
                val source = taskInfo.source
                if (source.startsWith("$parentDirectory/") || source.startsWith("$parentDirectory\\")) {
                    source.replace("$parentDirectory/", "").replace("$parentDirectory\\", "")
                } else {
                    displayPath(project, source, nonProjectPathDisplay)
                }
            } else {
                displayPath(project, taskInfo.source, nonProjectPathDisplay)
            }
        appender.append(" $pathText", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }

    override fun createPresentation(): PresentationData =
        PresentationData().apply {
            tooltip = taskInfo.description ?: "<i>No description provided.</i>"
        }

    override fun onDoubleClick(event: MouseEvent) {
        if (taskInfo is MiseUnknownTask) return

        val action = RunMiseTomlTaskAction(taskInfo)
        val actionEvent =
            AnActionEvent.createEvent(
                action,
                DataManager.getInstance().getDataContext(event.component),
                null,
                ActionPlaces.TOOLWINDOW_CONTENT,
                ActionUiKind.NONE,
                event,
            )

        runBackgroundableTask("Executing task: ${taskInfo.name}", project, false) {
            action.actionPerformed(actionEvent)
        }
    }

    override fun actions(): List<AnAction> {
        if (taskInfo is MiseUnknownTask) return emptyList()

        return listOfNotNull(
            RunMiseTomlTaskAction(taskInfo),
            when (taskInfo) {
                is MiseShellScriptTask -> {
                    object : AnAction("Go to Declaration", "Go to Declaration", AllIcons.General.Locate) {
                        override fun actionPerformed(e: AnActionEvent) {
                            OpenSourceUtil.navigateToSource(false, true, taskInfo.file.findPsiFile(project))
                        }
                    }
                }

                is MiseTomlTableTask -> {
                    object : AnAction("Go to Declaration", "Go to Declaration", AllIcons.General.Locate) {
                        override fun actionPerformed(e: AnActionEvent) {
                            OpenSourceUtil.navigateToSource(false, true, taskInfo.keySegment)
                        }
                    }
                }

                is MiseUnknownTask -> null
            },
        ).toMutableList().apply {
            addAll(EP_NAME.flatMap { it.contributeActions(taskInfo) })
        }
    }

    companion object {
        val EP_NAME = ConcurrentHashMap.newKeySet<MiseTaskNodeActionsContributor>()
    }

    interface MiseTaskNodeActionsContributor {
        fun contributeActions(task: MiseTask): List<AnAction>
    }
}
