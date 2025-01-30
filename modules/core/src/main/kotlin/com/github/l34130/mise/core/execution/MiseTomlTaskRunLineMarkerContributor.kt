package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.lang.psi.isSpecificTaskTableHeader
import com.github.l34130.mise.core.lang.psi.miseTomlTask
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlFileType
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlTableHeader

class MiseTomlTaskRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.containingFile.fileType !is TomlFileType) return null
        val tomlKey = element.parent.parent as? TomlKey ?: return null
        val header = tomlKey.parent as? TomlTableHeader ?: return null
        val miseTomlTask =
            if (header.isSpecificTaskTableHeader) {
                header.miseTomlTask.takeIf { header.key?.segments?.getOrNull(1) == element.parent }
            } else {
                header.miseTomlTask
            } ?: return null
        return Info(AllIcons.Actions.Execute, { "" }, RunMiseTomlTaskAction(miseTomlTask))
    }
}
