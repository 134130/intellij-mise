package com.github.l34130.mise.core.lang.psi

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.ObjectPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.VirtualFilePattern
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlFileType
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader
import org.toml.lang.psi.TomlTokenType
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind
import org.toml.lang.psi.ext.name

object MiseTomlPsiPatterns {
    private inline fun <reified I : PsiElement> tomlPsiElement(): PsiElementPattern.Capture<I> =
        psiElement<I>().inVirtualFile(
            VirtualFilePattern().ofType(TomlFileType),
        )

    val miseTomlStringLiteral = tomlPsiElement<TomlLiteral>().with("stringLiteral") { e, _ -> e.kind is TomlLiteralKind.String }
    val miseTomlLeafPsiElement = tomlPsiElement<LeafPsiElement>().with("leafPsiElement") { e, _ -> e.elementType is TomlTokenType }

    val onSpecificTaskTable =
        tomlPsiElement<TomlTable>()
            .withChild(
                psiElement<TomlTableHeader>()
                    .with("specificTaskCondition") { header: TomlTableHeader, _ ->
                        header.isSpecificTaskTableHeader
                    },
            )

    /**
     * ```
     * [tasks]
     * foo = { $name = [] }
     *         #^
     * ```
     *
     * ```
     * [tasks.foo]
     * $name = []
     * #^
     * ```
     */
    private fun taskProperty(name: String) =
        psiElement<TomlKeyValue>()
            .with("name") { e, _ -> e.key.name == name }
            .withParent(
                onSpecificTaskTable,
//                onSpecificTaskTable.andOr(
//                    psiElement<TomlInlineTable>().withSuperParent(2, onTaskTable),
//                ),
            )

    /**
     * ```
     * [tasks]
     * foo = { version = "*", depends = [] }
     *                                 #^
     * ```
     *
     * ```
     * [tasks.foo]
     * depends = []
     *          #^
     * ```
     */
    val onTaskDependsArray =
        psiElement<TomlArray>().withParent(taskProperty("depends")) or
            psiElement<TomlArray>().withParent(taskProperty("depends_post"))
    val inTaskDependsArray = tomlPsiElement<PsiElement>().inside(onTaskDependsArray)

    val onTaskWaitForArray =
        psiElement<TomlArray>().withParent(taskProperty("wait_for"))
    val inTaskWaitForArray = tomlPsiElement<PsiElement>().inside(onTaskWaitForArray)

    /**
     * ```
     * [tasks]
     * foo = { version = "*", depends = "" }
     *                                 #^
     * ```
     *
     * ```
     * [tasks.foo]
     * depends = ""
     *          #^
     * ```
     */
    val onTaskDependsString =
        miseTomlStringLiteral.withParent(taskProperty("depends")) or
            miseTomlStringLiteral.withParent(taskProperty("depends_post"))
    val inTaskDependsString = tomlPsiElement<PsiElement>().inside(onTaskDependsString)

    val onTaskWaitForString =
        miseTomlStringLiteral.withParent(taskProperty("wait_for"))
    val inTaskWaitForString = tomlPsiElement<PsiElement>().inside(onTaskWaitForString)

    val onTaskRunString = miseTomlStringLiteral.withParent(taskProperty("run"))
    val inTaskRunString = tomlPsiElement<PsiElement>().inside(onTaskRunString)

    fun <T : Any, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.with(
        name: String,
        cond: (T, ProcessingContext?) -> Boolean,
    ): Self =
        with(
            object : PatternCondition<T>(name) {
                override fun accepts(
                    t: T,
                    context: ProcessingContext?,
                ): Boolean = cond(t, context)
            },
        )
}

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> = PlatformPatterns.psiElement(I::class.java)

inline infix fun <reified I : PsiElement> ElementPattern<out I>.or(pattern: ElementPattern<out I>): PsiElementPattern.Capture<I> =
    psiElement<I>().andOr(this, pattern)
