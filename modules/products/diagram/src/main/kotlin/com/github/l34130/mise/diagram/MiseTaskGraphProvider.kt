package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.model.MiseTomlFile
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.diagram.AbstractDiagramElementManager
import com.intellij.diagram.BaseDiagramProvider
import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramElementManager
import com.intellij.diagram.DiagramPresentationModel
import com.intellij.diagram.DiagramVfsResolver
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.Nls
import org.toml.lang.psi.TomlFile

class MiseTaskGraphProvider : BaseDiagramProvider<MiseTaskGraphable>() {
    private val elementManager: DiagramElementManager<MiseTaskGraphable> =
        object : AbstractDiagramElementManager<MiseTaskGraphable>() {
            init {
                setUmlProvider(this@MiseTaskGraphProvider)
            }

            override fun findInDataContext(o: DataContext): MiseTaskGraphable {
                CommonDataKeys.PSI_ELEMENT.getData(o)?.let { psiElement ->
                    MiseTomlTableTask.resolveFromInlineTableInTaskTable(psiElement)?.let { return MiseTaskGraphableTaskWrapper(it) }
                    MiseTomlTableTask.resolveFromTaskChainedTable(psiElement)?.let { return MiseTaskGraphableTaskWrapper(it) }
                    MiseTomlTableTask.resolveOrNull(psiElement)?.let { return MiseTaskGraphableTaskWrapper(it) }
                }
                CommonDataKeys.PSI_FILE.getData(o)?.let { psiFile ->
                    (psiFile as? TomlFile)?.let {
                        val isMiseTomlFile = MiseTomlFile.isMiseTomlFile(psiFile.project, psiFile.viewProvider.virtualFile)
                        if (isMiseTomlFile) return MiseTaskGraphableTomlFile(it)
                    }
                }
                return DefaultMiseTaskGraphable
            }

            override fun isContainerFor(
                container: MiseTaskGraphable?,
                element: MiseTaskGraphable?,
            ): Boolean = false

            override fun canCollapse(element: MiseTaskGraphable?): Boolean = false

            override fun isAcceptableAsNode(o: Any?): Boolean = o is MiseTaskGraphable

            override fun getElementTitle(o: MiseTaskGraphable?): @Nls String? =
                when (o) {
                    is MiseTaskGraphableTaskWrapper<*> -> o.task.name
                    is MiseTaskGraphableTomlFile -> o.tomlFile.name
                    DefaultMiseTaskGraphable, null -> "Mise Task graph"
                }

            override fun getEditorTitle(
                element: MiseTaskGraphable?,
                additionalElements: Collection<MiseTaskGraphable?>,
            ): @Nls String? =
                when (element) {
                    is MiseTaskGraphableTaskWrapper<*> -> "Task: ${element.task.name}"
                    is MiseTaskGraphableTomlFile -> "File: ${element.tomlFile.name}"
                    DefaultMiseTaskGraphable, null -> "Mise Task graph"
                }

            override fun getNodeTooltip(o: MiseTaskGraphable?): @Nls String? = null

            override fun getNodeItems(o: MiseTaskGraphable?): Array<Any> = emptyArray()

            override fun canBeBuiltFrom(element: Any?): Boolean = element is MiseTaskGraphable
        }
    private val vfsResolver: DiagramVfsResolver<MiseTaskGraphable> =
        object : DiagramVfsResolver<MiseTaskGraphable> {
            override fun getQualifiedName(element: MiseTaskGraphable?): String? = null

            override fun resolveElementByFQN(
                element: String,
                project: Project,
            ): MiseTaskGraphable? = null
        }

    override fun getID(): String = ID

    override fun getPresentableName(): @NlsContexts.Label String = "Mise Task graph"

    override fun createDataModel(
        project: Project,
        element: MiseTaskGraphable?,
        file: VirtualFile?,
        presentationModel: DiagramPresentationModel,
    ): DiagramDataModel<MiseTaskGraphable> {
        val model =
            when (element) {
                is MiseTaskGraphableTaskWrapper<*> -> MiseSingleTaskGraphDataModel(project, element, this)
                is MiseTaskGraphableTomlFile -> MiseFileTaskGraphDataModel(project, element, this)
                is DefaultMiseTaskGraphable, null -> MiseFullTaskGraphDataModel(project, this)
            }
        return model
    }

    override fun getElementManager(): DiagramElementManager<MiseTaskGraphable> = elementManager

    override fun getVfsResolver(): DiagramVfsResolver<MiseTaskGraphable> = vfsResolver

    companion object {
        const val ID = "MiseTaskGraphProvider"
    }
}
