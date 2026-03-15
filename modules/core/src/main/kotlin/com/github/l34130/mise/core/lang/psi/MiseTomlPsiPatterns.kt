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
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader
import org.toml.lang.psi.TomlTokenType
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind
import org.toml.lang.psi.ext.name

object MiseTomlPsiPatterns {
    inline fun <reified I : PsiElement> tomlPsiElement(): PsiElementPattern.Capture<I> =
        psiElement<I>().inVirtualFile(
            VirtualFilePattern().ofType(TomlFileType),
        )

    val miseTomlStringLiteral: PsiElementPattern.Capture<TomlLiteral> = tomlPsiElement<TomlLiteral>().with("stringLiteral") { e, _ -> e.kind is TomlLiteralKind.String }
    val miseTomlLeafPsiElement: PsiElementPattern.Capture<LeafPsiElement> = tomlPsiElement<LeafPsiElement>().with("leafPsiElement") { e, _ -> e.elementType is TomlTokenType }

    // [tasks.foo]
    private val onTaskSpecificTable =
        tomlPsiElement<TomlTable>()
            .withChild(
                psiElement<TomlTableHeader>()
                    .with("taskSpecificCondition") { header: TomlTableHeader, _ ->
                        val headerKeySegments = header.key?.segments
                        headerKeySegments?.size == 2 && headerKeySegments[0].name == "tasks"
                    },
            )

    // [tasks]
    private val onTaskTable =
        tomlPsiElement<TomlTable>()
            .with("taskCondition") { table: TomlTable, _ ->
                val headerKeySegments = table.header.key?.segments
                headerKeySegments?.singleOrNull()?.name == "tasks"
            }

    /**
     * ```toml
     * [tasks.foo]
     * $name = []
     * #^
     *
     * [tasks]
     * foo = { $name = [] }
     *         #^
     * ```
     */
    fun onTaskProperty(name: String) =
        psiElement<TomlKeyValue>()
            .with("name") { e, _ -> e.key.name == name }
            .withParent(onTaskSpecificTable) or
            psiElement<TomlKeyValue>()
                .with("name") { e, _ -> e.key.name == name }
                .withParent(psiElement<TomlInlineTable>().withSuperParent(2, onTaskTable))

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
        psiElement<TomlArray>().withParent(onTaskProperty("depends")) or
            psiElement<TomlArray>().withParent(onTaskProperty("depends_post"))
    val inTaskDependsArray: PsiElementPattern.Capture<PsiElement> = tomlPsiElement<PsiElement>().inside(onTaskDependsArray)

    val onTaskWaitForArray: PsiElementPattern.Capture<TomlArray> =
        psiElement<TomlArray>().withParent(onTaskProperty("wait_for"))
    val inTaskWaitForArray: PsiElementPattern.Capture<PsiElement> = tomlPsiElement<PsiElement>().inside(onTaskWaitForArray)

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
        miseTomlStringLiteral.withParent(onTaskProperty("depends")) or
            miseTomlStringLiteral.withParent(onTaskProperty("depends_post"))
    val inTaskDependsString: PsiElementPattern.Capture<PsiElement> = tomlPsiElement<PsiElement>().inside(onTaskDependsString)

    val onTaskWaitForString =
        miseTomlStringLiteral.withParent(onTaskProperty("wait_for"))
    val inTaskWaitForString: PsiElementPattern.Capture<PsiElement> = tomlPsiElement<PsiElement>().inside(onTaskWaitForString)

    val onTaskRunString = miseTomlStringLiteral.withParent(onTaskProperty("run"))
    val onTaskRunStringArray = psiElement<TomlArray>().withParent(onTaskProperty("run"))
    val onTaskRunStringOrArray = onTaskRunString or onTaskRunStringArray
    val inTaskRunStringOrArray: PsiElementPattern.Capture<PsiElement> = tomlPsiElement<PsiElement>().inside(onTaskRunStringOrArray)

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
