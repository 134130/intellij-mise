package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.lang.MiseTomlFileType
import com.intellij.patterns.ObjectPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.VirtualFilePattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind
import org.toml.lang.psi.ext.name

object MiseTomlPsiPatterns {
    private inline fun <reified I : PsiElement> miseTomlPsiElement(): PsiElementPattern.Capture<I> =
        psiElement<I>().inVirtualFile(
            VirtualFilePattern().ofType(MiseTomlFileType),
        )

    fun miseTomlStringLiteral() = miseTomlPsiElement<TomlLiteral>().with("stringLiteral") { e, _ -> e.kind is TomlLiteralKind.String }

    private val onSpecificTaskTable =
        miseTomlPsiElement<TomlTable>()
            .withChild(
                psiElement<TomlTableHeader>()
                    .with("specificTaskCondition") { header, _ ->
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
    private val onTaskDependsArray =
        StandardPatterns.or(
            psiElement<TomlArray>()
                .withParent(taskProperty("depends")),
            psiElement<TomlArray>()
                .withParent(taskProperty("depends_post")),
        )
    val inTaskDependsArray = miseTomlPsiElement<PsiElement>().inside(onTaskDependsArray)

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
    private val onTaskDependsString =
        StandardPatterns.or(
            miseTomlStringLiteral()
                .withParent(taskProperty("depends")),
            miseTomlStringLiteral()
                .withParent(taskProperty("depends_post")),
        )
    val inTaskDependsString = miseTomlPsiElement<PsiElement>().inside(onTaskDependsString)

    inline fun <reified I : PsiElement> psiElement() = PlatformPatterns.psiElement(I::class.java)

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
