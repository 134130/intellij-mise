package com.github.l34130.mise.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.util.concurrency.SequentialTaskExecutor
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.rd.util.ConcurrentHashMap
import org.jetbrains.concurrency.CancellablePromise
import java.util.concurrent.atomic.AtomicLong

@Service(Service.Level.PROJECT)
class MiseTomlService(
    val project: Project,
) : Disposable {
    private val files: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet<VirtualFile>()
    private val refs: MutableSet<String> = ConcurrentHashMap.newKeySet<String>()
    private val anyChangeCount = AtomicLong(0)

    private val executor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("MiseTomlService")

    init {
        project.messageBus.connect(this).let {
            it.subscribe(
                MiseTomlFileVfsListener.MISE_TOML_CHANGED,
                Runnable {
                    refs.clear()
                    anyChangeCount.incrementAndGet()
                },
            )
            MiseTomlFileVfsListener.startListening(project, this, it)
        }
    }

//    override fun getModificationCount(): Long = anyChangeCount.get()

    fun registerRef(ref: String) {
        val index = ref.lastIndexOfAny(listOf("\\", "/"))
        if (index >= 0) {
            refs.add(ref.substring(index + 1))
        }
    }

    fun possiblyHasReference(ref: String) = refs.contains(ref)

    fun getMiseTomlFiles(): Set<VirtualFile> = files

    fun refresh(): CancellablePromise<Unit> {
        files.clear()
        return loadMiseTomlFiles()
    }

    private fun loadMiseTomlFiles(): CancellablePromise<Unit> =
        ReadAction
            .nonBlocking<Unit> {
                val result = mutableListOf<VirtualFile>()

                for (baseDir in project.getBaseDirectories()) {
                    for (child in baseDir.children) {
                        if (child.isDirectory) {
                            // mise/config.toml or .mise/config.toml
                            if (child.name == "mise" || child.name == ".mise") {
                                result.addIfNotNull(child.resolveFromRootOrRelative("config.toml"))
                            }

                            if (child.name == ".config") {
                                // .config/mise.toml
                                result.addIfNotNull(child.resolveFromRootOrRelative("mise.toml"))
                                // .config/mise/config.toml
                                result.addIfNotNull(child.resolveFromRootOrRelative("config.toml"))
                                // .config/mise/conf.d/*.toml
                                result.addAll(child.resolveFromRootOrRelative("conf.d")?.children.orEmpty())
                            }
                        } else {
                            // mise.local.toml, mise.toml, .mise.local.toml, .mise.toml
                            if (child.name.matches(MISE_TOML_NAME_REGEX)) {
                                result.add(child)
                            }
                        }
                    }
                }

                files.addAll(result)
            }.expireWith(this)
            .submit(executor)

    override fun dispose() {
    }

    companion object {
        private val MISE_TOML_NAME_REGEX = "^\\.?mise\\.(\\w+\\.)?toml$".toRegex()

        fun getInstance(project: Project): MiseTomlService = project.getService(MiseTomlService::class.java)
    }
}
