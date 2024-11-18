package com.github.l34130.mise.core.toolwindow

import com.intellij.ide.ActivityTracker
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.Invoker
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class MiseTreeToolWindow(
    treeStructure: MiseTreeStructure,
) : SimpleToolWindowPanel(true, true),
    DataProvider,
    Disposable {
    private val treeModel =
        StructureTreeModel(treeStructure, null, Invoker.forBackgroundPoolWithoutReadAction(this), this)
    private val tree = Tree(AsyncTreeModel(treeModel, true, this))

    init {
        background = UIUtil.getTreeBackground()
        size.width = Int.MAX_VALUE
        tree.size.width = Int.MAX_VALUE

        TreeUIHelper.getInstance().installTreeSpeedSearch(tree)
        tree.isRootVisible = false
        tree.autoscrolls = true
        tree.cellRenderer =
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
                val path = tree.getPathForLocation(event.x, event.y)
                ((path?.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? DoubleClickable)?.onDoubleClick(
                    event,
                )
                return true
            }
        }.installOn(tree)

        tree.addMouseListener(
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
//                        if (node is ActionGroupOnRightClick) {
//                            val actionGroupName = node.actionGroupName()
//
//                            (actionGroupName.let { groupName -> actionManager.getAction(groupName) } as? ActionGroup)?.let { group ->
//                                val context =
//                                    comp?.let { DataManager.getInstance().getDataContext(it, x, y) } ?: return@let
//                                val event = AnActionEvent.createFromDataContext(actionPlace, null, context)
//                                totalActions.addAll(group.getChildren(event))
//                            }
//                        }

                        val actionGroup = DefaultActionGroup(totalActions)
                        if (actionGroup.childrenCount > 0) {
                            // val popupMenu = actionManager.createActionPopupMenu(actionPlace, actionGroup)
//                            popupMenu.setTargetComponent(this@AbstractExplorerTreeToolWindow)
//                            popupMenu.component.show(comp, x, y)
                        }
                    }
                }
            ),
        )

        fun redraw() {
            // redraw toolbars
            ActivityTracker.getInstance().inc()
            runInEdt { redrawContent() }
        }

        // subscribe something and redraw
        // ApplicationManager.getApplication().messageBus.connect(this).subscribe()

        redrawContent()

        TreeUtil.expand(tree, 2)
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
        tree.selectionPaths
            ?.asSequence()
            ?.map { it.lastPathComponent }
            ?.filterIsInstance<DefaultMutableTreeNode>()
            ?.map { it.userObject }
            ?.filterIsInstance<T>()
            ?.toList()
            .orEmpty()

    fun redrawContent() {
        setContent(
            ScrollPaneFactory.createScrollPane(tree),
        )
        // required for refresh
        val state = TreeState.createOn(tree)
        treeModel.invalidateAsync()
        state.applyTo(tree)
    }

    companion object {
//        fun getInstance(project: Project): MiseTreeToolWindow = project.service()
    }
}
