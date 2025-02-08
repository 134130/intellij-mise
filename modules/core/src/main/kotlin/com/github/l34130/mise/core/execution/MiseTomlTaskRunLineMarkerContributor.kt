package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.model.MiseTask
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.toml.lang.psi.TomlTable

class MiseTomlTaskRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element !is LeafPsiElement) return null
        MiseTask.TomlTable.resolveOrNull(element)?.let { task ->
            return Info(RunMiseTomlTaskAction(task))
        }
        return null
    }
}
