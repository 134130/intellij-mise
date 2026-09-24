package com.github.l34130.mise.core

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.Collections

class MiseTomlFileListenerVfsChangeTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        // Ensure the VFS listener service is initialized
        project.service<MiseTomlFileListener>()
    }

    fun `test change to mise toml broadcasts toml change`() {
        val miseToml = myFixture.addFileToProject("mise.toml", "[tools]\n").virtualFile

        assertTrue(broadcastsTomlChange { changeContent(miseToml) })
    }

    fun `test change to mise toml in excluded folder does not broadcast toml change`() {
        val miseToml = myFixture.addFileToProject("templates/mise.toml", "[tools]\n").virtualFile
        excludeFolder(miseToml.parent)

        assertFalse(broadcastsTomlChange { changeContent(miseToml) })
    }

    fun `test change to mise toml in source root nested inside excluded folder broadcasts toml change`() {
        val miseToml = myFixture.addFileToProject("build/generated/mise.toml", "[tools]\n").virtualFile
        val sourceRoot = miseToml.parent
        excludeFolder(sourceRoot.parent)
        addSourceRoot(sourceRoot)

        assertTrue(broadcastsTomlChange { changeContent(miseToml) })
    }

    fun `test change to env file in library root nested inside excluded folder broadcasts toml change`() {
        val envFile = myFixture.addFileToProject("vendor/dependency/.env", "FOO=BAR\n").virtualFile
        val libraryRoot = envFile.parent
        excludeFolder(libraryRoot.parent)
        addModuleLibrary(libraryRoot)

        assertTrue(broadcastsTomlChange { changeContent(envFile) })
    }

    fun `test change to unrelated file does not broadcast toml change`() {
        val sourceFile = myFixture.addFileToProject("Main.java", "class Main {}\n").virtualFile

        assertFalse(broadcastsTomlChange { changeContent(sourceFile) })
    }

    fun `test change to unrelated file skips the exclusion check`() {
        val sourceFile = myFixture.addFileToProject("Main.java", "class Main {}\n").virtualFile
        val filesCheckedForExclusion = installExclusionCheckRecorder()

        changeContent(sourceFile)

        assertDoesntContain(filesCheckedForExclusion, sourceFile)
    }

    fun `test change to mise toml runs the exclusion check`() {
        val miseToml = myFixture.addFileToProject("mise.toml", "[tools]\n").virtualFile
        val filesCheckedForExclusion = installExclusionCheckRecorder()

        changeContent(miseToml)

        assertContainsElements(filesCheckedForExclusion, miseToml)
    }

    private fun broadcastsTomlChange(action: () -> Unit): Boolean {
        var wasTomlChangeBroadcast = false
        val subscription = Disposer.newDisposable()
        try {
            MiseProjectEventListener.subscribe(project, subscription) { event ->
                if (event.kind == MiseProjectEvent.Kind.TOML_CHANGED) wasTomlChangeBroadcast = true
            }
            action()
        } finally {
            Disposer.dispose(subscription)
        }
        return wasTomlChangeBroadcast
    }

    private fun changeContent(file: VirtualFile) {
        WriteAction.run<Throwable> { VfsUtil.saveText(file, VfsUtil.loadText(file) + "\n") }
    }

    /**
     * Returns the files the mise VFS listener passes to [ProjectFileIndex.isExcluded] until the test ends.
     * The installed index forwards every call to the real one.
     */
    private fun installExclusionCheckRecorder(): List<VirtualFile> {
        val realIndex = ProjectFileIndex.getInstance(project)
        val filesCheckedForExclusion = Collections.synchronizedList(mutableListOf<VirtualFile>())
        val recordingIndex =
            Proxy.newProxyInstance(
                ProjectFileIndex::class.java.classLoader,
                arrayOf(ProjectFileIndex::class.java),
            ) { _, method, args ->
                if (method.name == "isExcluded" && isCalledByMiseTomlFileListener()) {
                    filesCheckedForExclusion += args[0] as VirtualFile
                }
                try {
                    method.invoke(realIndex, *(args ?: emptyArray()))
                } catch (e: InvocationTargetException) {
                    throw e.targetException
                }
            } as ProjectFileIndex
        project.replaceService(ProjectFileIndex::class.java, recordingIndex, testRootDisposable)
        return filesCheckedForExclusion
    }

    private fun isCalledByMiseTomlFileListener(): Boolean =
        Throwable().stackTrace.any { it.className.startsWith(MiseTomlFileListener::class.java.name) }

    private fun excludeFolder(folder: VirtualFile) {
        PsiTestUtil.addExcludedRoot(module, folder)
        undoWhenTestEnds { PsiTestUtil.removeExcludedRoot(module, folder) }
    }

    private fun addSourceRoot(root: VirtualFile) {
        PsiTestUtil.addSourceRoot(module, root)
        undoWhenTestEnds { PsiTestUtil.removeSourceRoot(module, root) }
    }

    private fun addModuleLibrary(root: VirtualFile) {
        val libraryName = root.name
        ModuleRootModificationUtil.addModuleLibrary(module, libraryName, listOf(root.url), emptyList())
        undoWhenTestEnds {
            ModuleRootModificationUtil.updateModel(module) { model ->
                model.moduleLibraryTable.getLibraryByName(libraryName)?.let { model.moduleLibraryTable.removeLibrary(it) }
            }
        }
    }

    private fun undoWhenTestEnds(undo: () -> Unit) {
        Disposer.register(testRootDisposable) { undo() }
    }
}
