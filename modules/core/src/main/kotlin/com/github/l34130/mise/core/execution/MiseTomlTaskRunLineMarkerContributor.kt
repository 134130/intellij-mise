package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

class MiseTomlTaskRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is LeafPsiElement) return null
        MiseTomlTableTask.resolveOrNull(element)?.let { task ->
            return Info(RunMiseTomlTaskAction(task))
        }
        return null
    }
}
