package com.github.l34130.mise.core.toolwindow

import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.setting.MiseConfigurable
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.application
import com.intellij.util.concurrency.Invoker
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Component
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class MiseTreeToolWindow(
    private val project: Project,
    treeStructure: MiseTreeStructure,
) : SimpleToolWindowPanel(true, true),
    Disposable {
    private val toolWindowState = project.service<MiseToolWindowState>()
    private val treeModel =
        StructureTreeModel(treeStructure, null, Invoker.forBackgroundPoolWithoutReadAction(this), this)
    private val myTree = Tree(AsyncTreeModel(treeModel, true, this))

    init {
        initializeEnvFromProjectSettingsIfNeeded()

        val actionManager = ActionManager.getInstance()
        val actionGroup = DefaultActionGroup()
        actionGroup.add(
            object : AnAction("Refresh", "Refresh the Tree", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    invalidateCachesAndRefresh()
                }
            },
        )
        actionGroup.add(
            createViewOptionsAction(),
        )
        actionGroup.add(
            object : AnAction(), CustomComponentAction {
                override fun actionPerformed(e: AnActionEvent) = Unit

                override fun createCustomComponent(
                    presentation: Presentation,
                    place: String,
                ): JComponent {
                    val textField = JBTextField(toolWindowState.state.envOverride)
                    textField.columns = 6
                    textField.toolTipText = "Mise Environment"

                    fun applyEnvValue(raw: String) {
                        val value = raw.trim()
                        toolWindowState.state.envOverride = value
                        toolWindowState.state.envInitialized = true
                        scheduleRefresh()
                    }
                    textField.addActionListener {
                        applyEnvValue(textField.text)
                    }
                    textField.addFocusListener(
                        object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent?) {
                                applyEnvValue(textField.text)
                            }
                        },
                    )
                    return textField
                }
            },
        )
        actionGroup.addSeparator()
        actionGroup.add(
            object : AnAction("Settings", "Open Mise settings", AllIcons.General.Settings) {
                override fun actionPerformed(e: AnActionEvent) {
                    runInEdt {
                        ShowSettingsUtil
                            .getInstance()
                            .showSettingsDialog(project, MiseConfigurable::class.java)
                    }
                }
            },
        )

        val actionToolbar = actionManager.createActionToolbar("Mise View Toolbar", actionGroup, true)
        actionToolbar.targetComponent = this
        toolbar = actionToolbar.component
        setContent(ScrollPaneFactory.createScrollPane(myTree))

        background = UIUtil.getTreeBackground()
        size.width = Int.MAX_VALUE
        myTree.size.width = Int.MAX_VALUE

        TreeUIHelper.getInstance().installTreeSpeedSearch(myTree)
        myTree.isRootVisible = false
        myTree.autoscrolls = true
        myTree.cellRenderer =
            object : NodeRenderer() {
                override fun customizeCellRenderer(
                    tree: JTree,
                    value: Any?,
                    selected: Boolean,
                    expanded: Boolean,
                    leaf: Boolean,
                    row: Int,
                    hasFocus: Boolean,
                ) {
                    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
                    if (value is DefaultMutableTreeNode) {
                        val userObject = value.userObject
                        if (userObject is NodeDescriptor<*>) {
                            icon = userObject.icon
                        }
                    }
                }
            }

        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val path = myTree.getPathForLocation(event.x, event.y)
                ((path?.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? DoubleClickable)?.onDoubleClick(
                    event,
                )
                return true
            }
        }.installOn(myTree)

        myTree.addMouseListener(
            (
                    object : PopupHandler() {
                        override fun invokePopup(
                            comp: Component?,
                            x: Int,
                            y: Int,
                        ) {
                            val node = getSelectedNodesSameType<AbstractTreeNode<*>>()?.get(0) ?: return
                            val actionManager = ActionManager.getInstance()
                            val totalActions = mutableListOf<AnAction>()
                            val actionPlace = ActionPlaces.TOOLWINDOW_CONTENT
                            if (node is ActionOnRightClick) {
                                val actions = node.actions()
                                totalActions.addAll(actions)
                            }

                            val actionGroup = DefaultActionGroup(totalActions)
                            if (actionGroup.childrenCount > 0) {
                                val popupMenu = actionManager.createActionPopupMenu(actionPlace, actionGroup)
                                popupMenu.setTargetComponent(this@MiseTreeToolWindow)
                                popupMenu.component.show(comp, x, y)
                            }
                        }
                    }
                    ),
        )

        // Subscribe to cache invalidation events for automatic refresh
        MiseProjectEventListener.subscribe(project, this) { event ->
            when (event.kind) {
                MiseProjectEvent.Kind.SETTINGS_CHANGED,
                MiseProjectEvent.Kind.EXECUTABLE_CHANGED,
                MiseProjectEvent.Kind.TOML_CHANGED -> {
                    scheduleRefresh()
                }
                else -> Unit
            }
        }

        TreeUtil.expand(myTree, 2)

        scheduleRefresh()
    }

    override fun dispose() {
    }

    private inline fun <reified T : AbstractTreeNode<*>> getSelectedNodesSameType(): List<T>? {
        val selectedNodes = getSelectedNodes<T>()
        if (selectedNodes.isEmpty()) {
            return null
        }

        val firstClass = selectedNodes.first()::class.java
        return if (selectedNodes.all { firstClass.isInstance(it) }) {
            selectedNodes
        } else {
            null
        }
    }

    private inline fun <reified T : AbstractTreeNode<*>> getSelectedNodes() =
        myTree.selectionPaths
            ?.asSequence()
            ?.map { it.lastPathComponent }
            ?.filterIsInstance<DefaultMutableTreeNode>()
            ?.map { it.userObject }
            ?.filterIsInstance<T>()
            ?.toList()
            .orEmpty()

    /**
     * Invalidate all caches and refresh the tree.
     * This ensures the reload button actually fetches fresh data.
     */
    private fun invalidateCachesAndRefresh() {
        // Invalidate MiseTaskResolver cache (used by tool window)
        project.service<MiseTaskResolver>().invalidateCache()
        project.service<MiseCacheService>().invalidateAllCommands()
        // Redraw the tree
        runInEdt { redrawContent() }
    }

    private fun scheduleRefresh() {
        if (project.isDisposed) return
        application.invokeLater {
            if (!project.isDisposed) {
                redrawContent()
            }
        }
    }

    fun redrawContent() {
        setContent(
            ScrollPaneFactory.createScrollPane(myTree),
        )
        // required for refresh
        val state = TreeState.createOn(myTree)
        treeModel.invalidateAsync()
        state.applyTo(myTree)
    }

    private fun createViewOptionsAction(): ActionGroup {
        val group = DefaultActionGroup("View Options", true)
        group.templatePresentation.icon = AllIcons.General.InspectionsEye
        group.addSeparator("Group By")
        group.add(
            object : ToggleAction("Config Path", "Group nodes by source config path", AllIcons.Nodes.ConfigFolder) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

                override fun isSelected(e: AnActionEvent): Boolean = toolWindowState.state.groupByConfigPath

                override fun setSelected(
                    e: AnActionEvent,
                    state: Boolean,
                ) {
                    toolWindowState.state.groupByConfigPath = state
                    scheduleRefresh()
                }
            },
        )
        return group
    }

    private fun initializeEnvFromProjectSettingsIfNeeded() {
        val state = toolWindowState.state
        if (state.envInitialized) return
        state.envOverride = project.service<MiseProjectSettings>().state.miseConfigEnvironment
        state.envInitialized = true
    }
}
