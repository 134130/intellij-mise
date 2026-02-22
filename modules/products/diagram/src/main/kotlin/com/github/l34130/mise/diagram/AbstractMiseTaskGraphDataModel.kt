package com.github.l34130.mise.diagram

import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramEdge
import com.intellij.diagram.DiagramNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shared async data model for Mise task graph diagrams.
 *
 * This prevents EDT hangs by computing nodes on a pooled thread, and standardizes refresh/update behavior.
 * Diagrams open empty and refresh once computation finishes.
 */
abstract class AbstractMiseTaskGraphDataModel(
    project: Project,
    provider: MiseTaskGraphProvider,
) : DiagramDataModel<MiseTaskGraphable>(project, provider) {
    @Volatile
    protected var nodes: List<MiseTaskGraphNode> = emptyList()

    private val isDisposed = AtomicBoolean(false)
    private val generation = AtomicInteger(0)
    private val registeredWithBuilder = AtomicBoolean(false)

    private enum class EdgeDirection { INTO_NODE, OUT_OF_NODE }

    init {
        ApplicationManager.getApplication().invokeLater { reloadAsync() }
    }

    override fun getNodes(): Collection<DiagramNode<MiseTaskGraphable>?> = nodes

    override fun getNodeName(node: DiagramNode<MiseTaskGraphable>): String =
        when (val element = node.identifyingElement) {
            is MiseTaskGraphableTaskWrapper<*> -> element.task.name
            is MiseTaskGraphableTomlFile -> element.tomlFile.name
            DefaultMiseTaskGraphable -> "Mise Task graph"
        }

    /**
     * Builds diagram edges for the given task graph [nodes].
     *
     * This function resolves dependency references by **task name**:
     * - `depends` and `waitFor` create an edge **from the dependency to the current task**.
     * - `dependsPost` creates an edge **from the current task to the post-dependency**.
     *
     * ### Task name uniqueness
     * In most cases task names should be unique within [nodes]. If duplicates ever occur, the behavior is kept
     * consistent with the original implementation that used `nodes.find { ... }`:
     *
     * - We keep the **first** node encountered for a given task name (`putIfAbsent`), i.e. "first wins".
     * - This avoids subtle changes where a `Map` built with `associateBy` would make the **last** duplicate win.
     *
     * Unresolvable dependency names (no node with that name) are ignored.
     */
    override fun getEdges(): Collection<DiagramEdge<MiseTaskGraphable>> {
        val nodeByTaskName = buildMap {
            for (n in nodes) {
                val name = (n.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task.name
                putIfAbsent(name, n) // preserves first occurrence, matching the original `find` logic
            }
        }

        return buildList {
            for (taskGraphNode in nodes) {
                val task = (taskGraphNode.identifyingElement as MiseTaskGraphableTaskWrapper<*>).task

                val edgeSpecifications: List<Pair<List<String>, EdgeDirection>> =
                    listOf(
                        task.depends?.map { it.first() }.orEmpty() to EdgeDirection.INTO_NODE,
                        task.waitFor?.map { it.first() }.orEmpty() to EdgeDirection.INTO_NODE,
                        task.dependsPost?.map { it.first() }.orEmpty() to EdgeDirection.OUT_OF_NODE,
                    )

                for ((names, direction) in edgeSpecifications) {
                    for (name in names) {
                        val target = nodeByTaskName[name] ?: continue
                        add(
                            when (direction) {
                                EdgeDirection.OUT_OF_NODE -> MiseTaskGraphEdge(taskGraphNode, target)
                                EdgeDirection.INTO_NODE -> MiseTaskGraphEdge(target, taskGraphNode)
                            },
                        )
                    }
                }
            }
        }
    }

    override fun refreshDataModel() {
        reloadAsync()
    }

    override fun dispose() {
        isDisposed.set(true)
    }

    protected abstract fun computeNodesBlocking(): List<MiseTaskGraphNode>

    protected open fun configurePresentationBeforeRefresh() {
        // default no-op
    }

    private fun reloadAsync() {
        if (isDisposed.get() || project.isDisposed) return
        val currentGeneration = generation.incrementAndGet()
        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {
            if (isDisposed.get() || project.isDisposed) return@executeOnPooledThread
            val computedNodes = computeNodesBlocking()
            app.invokeLater {
                if (isDisposed.get() || project.isDisposed) return@invokeLater
                if (generation.get() != currentGeneration) return@invokeLater
                nodes = computedNodes

                val currentBuilder = runCatching { builder }.getOrNull() ?: return@invokeLater

                if (registeredWithBuilder.compareAndSet(false, true)) {
                    // Ensure our model is disposed when the diagram builder is disposed.
                    Disposer.register(currentBuilder, this)
                }

                configurePresentationBeforeRefresh()
                currentBuilder
                    .queryUpdate()
                    .withRelayout()
                    .withDataReload()
                    .withPresentationUpdate()
                    .run()
            }
        }
    }
}
