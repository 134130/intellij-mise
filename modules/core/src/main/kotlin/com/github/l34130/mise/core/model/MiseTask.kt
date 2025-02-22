package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.collapsePath
import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.or
import com.github.l34130.mise.core.lang.psi.stringArray
import com.github.l34130.mise.core.lang.psi.stringValue
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
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
}

class MiseUnknownTask internal constructor(
    override val name: String,
    override val aliases: List<String>? = null,
    override val depends: List<String>? = null,
    override val description: String? = null,
    /**
     * Absolute path
     */
    override val source: String? = null,
) : MiseTask

class MiseShellScriptTask internal constructor(
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
        ): MiseShellScriptTask? =
            MiseShellScriptTask(
                name = baseDir.toNioPath().relativize(file.toNioPath()).joinToString(":"),
                file = file,
                source = FileUtil.getRelativePath(baseDir.presentableUrl, file.presentableUrl, File.separatorChar),
            )
    }
}

class MiseTomlTableTask internal constructor(
    override val name: String,
    override val aliases: List<String>? = null,
    override val depends: List<String>? = null,
    override val description: String? = null,
    override val source: String? = null,
    val keySegment: TomlKeySegment,
) : MiseTask {
    companion object {
        private val acceptPattern =
            MiseTomlPsiPatterns.miseTomlStringLiteral or
                MiseTomlPsiPatterns.miseTomlLeafPsiElement or
                MiseTomlPsiPatterns.tomlPsiElement<TomlKeySegment>()

        /**
         * If the given [psiElement] is a [TomlKeySegment] or [TomlLiteral] of a task table, returns the [MiseTomlTableTask] instance.
         *
         *   ```toml
         *   [tasks.<foo>]
         *   run = "echo foo"
         *   ```
         */
        fun resolveFromTaskChainedTable(psiElement: PsiElement): MiseTomlTableTask? {
            if (!acceptPattern.accepts(psiElement)) return null
            val tomlKey =
                if (psiElement is TomlKeySegment) {
                    psiElement.parent as? TomlKey
                } else {
                    psiElement.parent.parent as? TomlKey
                } ?: return null

            val tomlTableHeader = tomlKey.parent as? TomlTableHeader ?: return null
            val keySegments = tomlTableHeader.key?.segments ?: return null
            if (keySegments.size != 2) return null
            if (keySegments[0].name != "tasks") return null
            if (keySegments[0] == psiElement.parent) return null // escape self (tasks)

            val table = tomlTableHeader.parent as? org.toml.lang.psi.TomlTable ?: return null
            val keySegment = keySegments[1]
            return MiseTomlTableTask(
                name = keySegment.name ?: return null,
                description = table.getValueWithKey("description")?.stringValue,
                depends = table.getValueWithKey("depends")?.stringArray,
                aliases = table.getValueWithKey("alias")?.stringArray,
                source = collapsePath(psiElement.containingFile, psiElement.project),
                keySegment = keySegment,
            )
        }

        /**
         * If the given [psiElement] is a key segment(or literal) of a task table, returns the [MiseTomlTableTask] instance.
         *
         *   ```toml
         *   [tasks]
         *   <foo> = { run = "echo foo" }
         *   ```
         */
        fun resolveFromInlineTableInTaskTable(psiElement: PsiElement): MiseTomlTableTask? {
            val tomlKey =
                if (psiElement is TomlKeySegment) {
                    psiElement.parent as? TomlKey
                } else {
                    psiElement.parent.parent as? TomlKey
                } ?: return null

            val tomlTable = tomlKey.parent.parent as? org.toml.lang.psi.TomlTable ?: return null
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
            return MiseTomlTableTask(
                name = keySegment.name ?: return null,
                description = table.getValueWithKey("description")?.stringValue,
                depends = table.getValueWithKey("depends")?.stringArray,
                aliases = table.getValueWithKey("alias")?.stringArray,
                source = collapsePath(psiElement.containingFile, psiElement.project),
                keySegment = keySegment,
            )
        }
    }
}
