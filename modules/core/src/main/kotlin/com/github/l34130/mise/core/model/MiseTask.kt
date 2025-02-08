package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.collapsePath
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.or
import com.github.l34130.mise.core.lang.psi.stringArray
import com.github.l34130.mise.core.lang.psi.stringValue
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.currentOrDefaultProject
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.sh.psi.ShFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.containers.nullize
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTableHeader
import java.io.File

sealed interface MiseTask {
    val name: String
    val aliases: List<String>?
    val depends: List<String>?
    val description: String?

    /**
     * Relative path from the project root. (without ./)
     */
    val source: String?

    companion object {
        val DATA_KEY = DataKey.create<MiseTask>(javaClass.simpleName)
    }

    class Unknown internal constructor(
        override val name: String,
        override val aliases: List<String>? = null,
        override val depends: List<String>? = null,
        override val description: String? = null,
        override val source: String? = null,
    ) : MiseTask

    class ShellScript internal constructor(
        override val name: String,
        override val aliases: List<String>? = null,
        override val depends: List<String>? = null,
        override val description: String? = null,
        override val source: String? = null,
        val file: VirtualFile,
    ) : MiseTask {
        companion object {
            fun resolveOrNull(
                baseDir: VirtualFile,
                file: VirtualFile,
            ): ShellScript? =
                ShellScript(
                    name = baseDir.toNioPath().relativize(file.toNioPath()).joinToString(":"),
                    file = file,
                    source = FileUtil.getRelativePath(baseDir.presentableUrl, file.presentableUrl, File.separatorChar),
                )
        }
    }

    class TomlTable internal constructor(
        override val name: String,
        override val aliases: List<String>? = null,
        override val depends: List<String>? = null,
        override val description: String? = null,
        override val source: String? = null,
        val keySegment: TomlKeySegment,
    ) : MiseTask {
        companion object {
            /**
             * If the given [psiElement] is a key segment(or literal) of a task table, returns the [TomlTable] instance.
             */
            @Suppress("ktlint:standard:chain-method-continuation")
            fun resolveOrNull(psiElement: PsiElement): TomlTable? {
                if (!(
                        MiseTomlPsiPatterns.miseTomlStringLiteral or
                            MiseTomlPsiPatterns.miseTomlLeafPsiElement or
                            MiseTomlPsiPatterns.tomlPsiElement<TomlKeySegment>()
                    ).accepts(psiElement)
                ) {
                    return null
                }

                val tomlKey =
                    if (psiElement is TomlKeySegment) {
                        psiElement.parent as? TomlKey
                    } else {
                        psiElement.parent.parent as? TomlKey
                    } ?: return null

                // CASE: [tasks.<TASK_NAME>]
                (tomlKey.parent as? TomlTableHeader)?.let { tomlTableHeader ->
                    val keySegments = tomlTableHeader.key?.segments ?: return null
                    if (keySegments.size != 2) return null
                    if (keySegments[0].name != "tasks") return null
                    if (keySegments[0] == psiElement.parent) return null // escape self (tasks)

                    val table = tomlTableHeader.parent as? org.toml.lang.psi.TomlTable ?: return null
                    val keySegment = keySegments[1]
                    return TomlTable(
                        name = keySegment.name ?: return null,
                        description = table.getValueWithKey("description")?.stringValue,
                        depends = table.getValueWithKey("depends")?.stringArray,
                        aliases = table.getValueWithKey("alias")?.stringArray,
                        source = collapsePath(psiElement.containingFile, psiElement.project),
                        keySegment = keySegment,
                    )
                }

                // CASE: [tasks]
                //       <TASK_NAME> = {}
                (tomlKey.parent.parent as? org.toml.lang.psi.TomlTable)?.let { tomlTable ->
                    val tomlTableHeader = tomlTable.header
                    if (tomlTableHeader == tomlKey.parent) return null // escape self (tasks)
                    if (tomlTableHeader.key
                            ?.segments
                            ?.singleOrNull()
                            ?.textMatches("tasks") != true
                    ) {
                        return null
                    }

                    val keySegment = tomlKey.segments.singleOrNull() ?: return null
                    val table = (tomlKey.parent as? TomlKeyValue)?.value as? TomlInlineTable ?: return null
                    return TomlTable(
                        name = keySegment.name ?: return null,
                        description = table.getValueWithKey("description")?.stringValue,
                        depends = table.getValueWithKey("depends")?.stringArray,
                        aliases = table.getValueWithKey("alias")?.stringArray,
                        source = collapsePath(psiElement.containingFile, psiElement.project),
                        keySegment = keySegment,
                    )
                }

                return null
            }
        }

        private val PsiElement.originalContainingFile: UserDataHolder?
            get() {
                val containingFile = this.containingFile
                return (containingFile.viewProvider.virtualFile as? LightVirtualFile)?.originalFile
                    ?: containingFile
            }
    }
}

fun MiseTask.documentationString(): String {
    val task = this

    fun StringBuilder.appendKeyValueSection(
        key: String,
        value: String?,
    ) {
        append(DocumentationMarkup.SECTION_HEADER_START)
        append(key)
        append(DocumentationMarkup.SECTION_SEPARATOR)
        append("<p>")
        append(value ?: DocumentationMarkup.GRAYED_ELEMENT.addText("None"))
        append(DocumentationMarkup.SECTION_END)
    }

    return buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append(task.name)
        append(DocumentationMarkup.DEFINITION_END)
        append(DocumentationMarkup.CONTENT_START)
        append(task.description ?: DocumentationMarkup.GRAYED_ELEMENT.addText("No description provided."))
        append(DocumentationMarkup.CONTENT_END)
        append(DocumentationMarkup.SECTIONS_START)
        appendKeyValueSection("Alias:", task.aliases.nullize()?.joinToString(", "))
        appendKeyValueSection("Depends:", task.depends.nullize()?.joinToString(", "))
        appendKeyValueSection(
            "File:",
            when (task) {
                is MiseTask.ShellScript -> collapsePath(task.file as ShFile, task.file.project)
                is MiseTask.TomlTable -> collapsePath(task.keySegment.containingFile, task.keySegment.project)
                is MiseTask.Unknown -> task.source?.let { collapsePath(it, currentOrDefaultProject(null)) } ?: "unknown"
            },
        )
        append(DocumentationMarkup.SECTIONS_END)
    }
}
