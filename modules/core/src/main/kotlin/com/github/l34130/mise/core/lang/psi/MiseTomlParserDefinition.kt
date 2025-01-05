package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.lang.MiseTomlLanguage
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.toml.lang.lexer.TomlLexer
import org.toml.lang.parse.TomlParser
import org.toml.lang.psi.TOML_COMMENTS

class MiseTomlParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = TomlLexer()

    override fun createParser(project: Project): PsiParser = TomlParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = TOML_COMMENTS

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement = throw UnsupportedOperationException()

    override fun createFile(viewProvider: FileViewProvider): PsiFile = MiseTomlFile(viewProvider)

    companion object {
        val FILE = IFileElementType(MiseTomlLanguage)
    }
}
