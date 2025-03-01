package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.model.MiseTask
import com.intellij.diagram.DiagramEdgeBase
import com.intellij.diagram.DiagramRelationshipInfo
import com.intellij.diagram.DiagramRelationshipInfoAdapter
import com.intellij.diagram.presentation.DiagramLineType

class MiseTaskGraphEdge(
    source: MiseTaskGraphNode,
    target: MiseTaskGraphNode,
) : DiagramEdgeBase<MiseTaskGraphable>(
        source,
        target,
        DiagramRelationshipInfoAdapter
            .Builder()
            .setBottomSourceLabel(resolveSourceLabel(source, target))
            .setBottomTargetLabel(resolveTargetLabel(source, target))
            .setLineType(DiagramLineType.SOLID)
            .setSourceArrow(DiagramRelationshipInfo.NONE)
            .setTargetArrow(DiagramRelationshipInfo.STANDARD)
            .create(),
    ) {
    companion object {
        private fun resolveSourceLabel(
            source: MiseTaskGraphNode,
            target: MiseTaskGraphNode,
        ): String? {
            val sourceTask = (source.identifyingElement as MiseTaskGraphableTaskWrapper<MiseTask>).task
            val targetTask = (target.identifyingElement as MiseTaskGraphableTaskWrapper<MiseTask>).task

            if (targetTask.depends?.contains(sourceTask.name) == true) return "depends"
            if (targetTask.waitFor?.contains(sourceTask.name) == true) return "wait_for"
            return null
        }

        private fun resolveTargetLabel(
            source: MiseTaskGraphNode,
            target: MiseTaskGraphNode,
        ): String? {
            val sourceTask = (source.identifyingElement as MiseTaskGraphableTaskWrapper<MiseTask>).task
            val targetTask = (target.identifyingElement as MiseTaskGraphableTaskWrapper<MiseTask>).task

            if (sourceTask.dependsPost?.contains(targetTask.name) == true) return "depends_post"
            return null
        }
    }
}
