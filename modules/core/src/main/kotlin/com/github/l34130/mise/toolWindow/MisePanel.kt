package com.github.l34130.mise.toolWindow

import com.github.l34130.mise.commands.MiseCmd
import com.github.l34130.mise.commands.MiseTask
import com.github.l34130.mise.commands.MiseTool
import com.github.l34130.mise.notifications.Notification
import com.github.l34130.mise.settings.MiseConfigurable
import com.github.l34130.mise.settings.MiseSettings
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.terminal.TerminalView
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.UIManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class RefreshAction(private val panel: MisePanel) : AnAction(
    "Refresh Mise Configuration",
    "Refresh Mise configuration",
    AllIcons.Actions.Refresh
) {
    override fun actionPerformed(e: AnActionEvent) {
        FileDocumentManager.getInstance().saveAllDocuments()
        panel.refreshMiseConfiguration()
    }
}

class SettingsAction(private val project: Project) : AnAction(
    "Mise Settings",
    "Open Mise settings",
    AllIcons.General.Settings
) {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, MiseConfigurable::class.java)
    }
}

class MisePanel(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()), Disposable {
    private val rootNode = DefaultMutableTreeNode("Root")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    private val settingsConnection = project.messageBus.connect(this)

    init {
        tree.apply {
            isRootVisible = false
            showsRootHandles = true
            cellRenderer = MiseTreeCellRenderer()
            background = UIManager.getColor("Tree.background")
            isOpaque = false
            addMouseListener(TreeMouseListener())
        }

        add(JBScrollPane(tree).apply {
            border = JBUI.Borders.empty()
            viewport.isOpaque = false
            background = UIManager.getColor("Tree.background")
            isOpaque = false
        }, BorderLayout.CENTER)

        refreshMiseConfiguration()

        settingsConnection.subscribe(
            MiseSettings.MISE_SETTINGS_TOPIC,
            object : MiseSettings.SettingsChangeListener {
                override fun settingsChanged(oldState: MiseSettings.State, newState: MiseSettings.State) {
                    if (oldState.miseProfile != newState.miseProfile) {
                        refreshMiseConfiguration()
                    }
                }
            }
        )
    }

    private fun createActionGroup(): ActionGroup {
        return DefaultActionGroup().apply {
            add(object : AnAction("Run Task", "Run selected Mise task", AllIcons.Actions.Execute) {
                override fun actionPerformed(e: AnActionEvent) {
                    val task = getSelectedTask()
                    if (task != null) {
                        executeTask(task)
                    }
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = getSelectedTask() != null
                }
            })
        }
    }

    private fun getSelectedTask(): MiseTask? {
        val path = tree.selectionPath ?: return null
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return null
        return node.userObject as? MiseTask
    }

    companion object {
        private const val TYPE_TASKS = "Tasks"
        private const val TYPE_TOOLS = "Tools"
        private const val TYPE_ENV = "Environment variables"
    }

    private inner class TreeMouseListener : PopupHandler() {
        override fun mouseClicked(e: MouseEvent) {
            val row = tree.getRowForLocation(e.x, e.y)
            if (row == -1) {
                super.mouseClicked(e)
                return
            }

            val path = tree.getPathForLocation(e.x, e.y)
            val node = path?.lastPathComponent as? DefaultMutableTreeNode ?: return
            val userObject = node.userObject

            val rootCategory = getRootCategory(node)

            when (rootCategory) {
                TYPE_TASKS -> handleTaskNodeClick(e, row, path, node, userObject)
                TYPE_TOOLS, TYPE_ENV -> {
                    if (tree.getRowBounds(row)?.contains(e.point) == true) {
                        tree.selectionPath = path
                    }
                }
            }

            super.mouseClicked(e)
        }

        private fun handleTaskNodeClick(
            e: MouseEvent,
            row: Int,
            path: TreePath,
            node: DefaultMutableTreeNode,
            userObject: Any
        ) {
            if (userObject !is MiseTask) return
            val rowBounds = tree.getRowBounds(row) ?: return
            when {
                e.clickCount == 2 && rowBounds.contains(e.point) -> {
                    executeTask(userObject)
                }

                rowBounds.contains(e.point) -> {
                    tree.selectionPath = path
                }
            }
        }

        override fun invokePopup(comp: Component?, x: Int, y: Int) {
            val path = tree.getPathForLocation(x, y)
            if (path != null) {
                tree.selectionPath = path

                val node = path.lastPathComponent as DefaultMutableTreeNode
                if (getRootCategory(node) == TYPE_TASKS && node.userObject is MiseTask) {
                    val actionGroup = createActionGroup()
                    val popupMenu = ActionManager.getInstance().createActionPopupMenu("MiseTasksPopup", actionGroup)
                    popupMenu.component.show(comp, x, y)
                }
            }
        }

        private fun getRootCategory(node: DefaultMutableTreeNode): String? {
            var current = node
            while (current.parent != null && current.parent != rootNode) {
                current = current.parent as DefaultMutableTreeNode
            }
            return if (current.parent == rootNode) current.toString() else null
        }
    }

    private class MiseTreeCellRenderer : DefaultTreeCellRenderer() {
        companion object {
            private const val ICON_WIDTH = 16
            private const val ICON_PADDING = 4
        }

        init {
            isOpaque = false
            backgroundNonSelectionColor = UIUtil.getTreeBackground()
            backgroundSelectionColor = UIUtil.getTreeSelectionBackground(true)
            textNonSelectionColor = UIUtil.getTreeForeground()
            textSelectionColor = UIUtil.getTreeSelectionForeground(true)
        }

        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

            val node = value as DefaultMutableTreeNode
            val userObject = node.userObject
            val rootCategory = getRootCategory(node)

            when {
                node.parent == tree.model.root -> {
                    icon = AllIcons.Nodes.Folder
                    text = userObject.toString()
                    toolTipText = null
                }

                rootCategory == TYPE_TASKS && userObject is MiseTask -> {
                    icon = AllIcons.Actions.Execute
                    text = buildTaskLabel(userObject)
                    toolTipText = buildTaskTooltip(userObject)
                    border = JBUI.Borders.empty(0, 0, 0, ICON_WIDTH + ICON_PADDING * 2)
                }

                rootCategory == TYPE_TOOLS && userObject is MiseToolNode -> {
                    icon = AllIcons.Nodes.Plugin
                    text = userObject.toString()
                    toolTipText = buildToolTooltip(userObject)
                    border = null
                }

                rootCategory == TYPE_ENV -> {
                    icon = AllIcons.Nodes.Variable
                    text = userObject.toString()
                    toolTipText = null
                    border = null
                }

                else -> {
                    icon = null
                    text = userObject.toString()
                    toolTipText = null
                    border = null
                }
            }

            return this
        }

        private fun getRootCategory(node: DefaultMutableTreeNode): String? {
            var current = node
            while (current.parent != null && current.parent is DefaultMutableTreeNode && (current.parent as DefaultMutableTreeNode).parent != null) {
                current = current.parent as DefaultMutableTreeNode
            }
            return current.toString()
        }

        private fun buildTaskLabel(task: MiseTask): String = task.name

        private fun buildTaskTooltip(task: MiseTask): String = buildString {
            append("<html>")
            append("<b>${task.name}</b>")
            if (!task.description.isNullOrEmpty()) {
                append("<br>${task.description}")
            }
            append("<br>Source: ${task.source}")
            append("</html>")
        }

        private fun buildToolTooltip(tool: MiseToolNode): String = buildString {
            append("<html>")
            append("<b>${tool.name}</b>")
            append("<br>Version: ${tool.toolInfo.version}")
            append("<br>Requested version: ${tool.toolInfo.requestedVersion ?: "N/A"}")
            append("<br>Install path: ${tool.toolInfo.installPath}")
            append("<br>Installed: ${tool.toolInfo.installed}")
            append("<br>Active: ${tool.toolInfo.active}")
            append("<br>Source: ${tool.toolInfo.source}")
            append("</html>")
        }
    }

    data class MiseToolNode(
        val name: String,
        val toolInfo: MiseTool
    ) {
        override fun toString(): String =
            name + " (${toolInfo.version})"
    }

    fun refreshMiseConfiguration() {
        rootNode.removeAllChildren()

        try {
            val tasksNode = DefaultMutableTreeNode("Tasks")
            rootNode.add(tasksNode)

            val miseProfile = MiseSettings.instance.state.miseProfile
            val workDir = project.basePath ?: return
            val tasks = MiseCmd.loadTasks(workDir, miseProfile)

            val tasksBySource = tasks.groupBy { it.source }

            tasksBySource.forEach { task ->
                val sourceNode = DefaultMutableTreeNode(task.key)
                tasksNode.add(sourceNode)

                task.value.forEach { t ->
                    sourceNode.add(DefaultMutableTreeNode(t))
                }
            }

            val tools = MiseCmd.loadTools(workDir, miseProfile)
            val toolsNode = DefaultMutableTreeNode("Tools")
            rootNode.add(toolsNode)

            val toolsBySource = tools.flatMap { (name, toolInfos) ->
                toolInfos.map { MiseToolNode(name, it) }
            }.groupBy { it.toolInfo.source }

            toolsBySource.forEach { (source, tools) ->
                val sourceNode = DefaultMutableTreeNode(source?.path ?: "Unknown source")
                toolsNode.add(sourceNode)

                tools.forEach { tool ->
                    sourceNode.add(DefaultMutableTreeNode(tool))
                }
            }

            val env = MiseCmd.loadEnv(workDir, miseProfile)
            val envNode = DefaultMutableTreeNode("Environment variables")
            rootNode.add(envNode)
            env.forEach { (key, value) ->
                envNode.add(DefaultMutableTreeNode("$key=$value"))
            }

            treeModel.reload()

            for (i in 0 until rootNode.childCount) {
                tree.expandPath(tree.getPathForRow(i))
            }
        } catch (e: Exception) {
            Notification.notify(
                "Failed to load tasks: ${e.message}",
                type = NotificationType.ERROR,
                project = project
            )
        }
    }

    private fun executeTask(task: MiseTask) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val terminalToolWindow = toolWindowManager.getToolWindow("Terminal")

        terminalToolWindow?.show {
            val profile = MiseSettings.instance.state.miseProfile
            val profileArg = if (profile.isNotEmpty()) "--profile \"$profile\"" else ""
            val command = "mise run $profileArg \"${task.name}\""

            val terminal = TerminalView.getInstance(project)
            terminal.createLocalShellWidget(project.basePath ?: "", command).executeCommand(command)
        }
    }

    override fun dispose() {
        settingsConnection.disconnect()
    }
}