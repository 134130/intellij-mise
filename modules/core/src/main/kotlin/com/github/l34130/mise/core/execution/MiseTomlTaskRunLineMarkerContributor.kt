package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.github.l34130.mise.core.lang.psi.isSpecificTaskTableHeader
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlTableHeader

class MiseTomlTaskRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.containingFile !is MiseTomlFile) return null
        val header = element as? TomlTableHeader ?: return null
        if (!header.isSpecificTaskTableHeader) return null

        val miseTomlTask = header.key?.segments?.getOrNull(1) ?: return null
        return Info(AllIcons.Actions.Execute, { "" }, RunMiseTomlTaskAction(miseTomlTask))
    }
}
