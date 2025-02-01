package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.collapsePath
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.or
import com.github.l34130.mise.core.lang.psi.stringArray
import com.github.l34130.mise.core.lang.psi.stringValue
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightVirtualFile
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
            @Suppress("ktlint:standard:chain-method-continuation")
            fun resolveOrNull(psiElement: PsiElement): TomlTable? {
                if (!(MiseTomlPsiPatterns.miseTomlStringLiteral or MiseTomlPsiPatterns.miseTomlLeafPsiElement)
                        .accepts(psiElement)
                ) {
                    return null
                }

                val tomlKey = psiElement.parent.parent as? TomlKey ?: return null

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
