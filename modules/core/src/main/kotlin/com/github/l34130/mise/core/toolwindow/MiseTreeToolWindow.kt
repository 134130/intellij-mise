package com.github.l34130.mise.core.toolwindow

import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.setting.MiseConfigurable
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.Invoker
import com.intellij.util.application
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class MiseTreeToolWindow(
    private val project: Project,
    treeStructure: MiseTreeStructure,
) : SimpleToolWindowPanel(true, true),
    Disposable {
    private val treeModel =
        StructureTreeModel(treeStructure, null, Invoker.forBackgroundPoolWithoutReadAction(this), this)
    private val myTree = Tree(AsyncTreeModel(treeModel, true, this))

    init {
        val actionManager = ActionManager.getInstance()
        val actionGroup = DefaultActionGroup()
        actionGroup.add(
            object : AnAction("Refresh", "Refresh the Tree", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    invalidateCachesAndRefresh()
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

        redrawContent()

        TreeUtil.expand(myTree, 2)
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
        // Refresh SDK state from the latest mise config.
        AbstractProjectSdkSetup.runAll(project, isUserInteraction = false)
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

    companion object {
//        fun getInstance(project: Project): MiseTreeToolWindow = project.service()
    }
}
