package com.github.l34130.mise.core

import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.util.containers.addIfNotNull

class MiseTaskService {
    companion object {
        fun getFileTaskDirectories(project: Project): List<VirtualFile> {
            val result = mutableListOf<VirtualFile>()
            for (baseDir in project.getBaseDirectories()) {
                result.addIfNotNull(baseDir.resolveFromRootOrRelative("mise-tasks")?.takeIf { it.isDirectory })
                result.addIfNotNull(baseDir.resolveFromRootOrRelative(".mise-tasks")?.takeIf { it.isDirectory })
                result.addIfNotNull(baseDir.resolveFromRootOrRelative("mise/tasks")?.takeIf { it.isDirectory })
                result.addIfNotNull(baseDir.resolveFromRootOrRelative(".mise/tasks")?.takeIf { it.isDirectory })
                result.addIfNotNull(baseDir.resolveFromRootOrRelative(".config/mise/tasks")?.takeIf { it.isDirectory })
            }
            return result
        }
    }

//            val miseTomlFiles = MiseTomlService.getMiseTomlFiles(project)
// TODO: Implement MiseToml `task_config` support
//                ReadAction
//                    .run<Throwable> {
//                        for (miseToml in miseTomlFiles) {
//                            val psiFile = miseToml.findPsiFile(project) as? MiseTomlFile ?: continue
//
//                            val taskConfig =
//                                psiFile
//                                    .childrenOfType<TomlTable>()
//                                    .firstOrNull { it.header.key?.textMatches("task_config") == true } ?: continue
//
//                            val taskDir = taskConfig.getValueWithKey("dir")?.stringValue ?: continue
//                            result.addIfNotNull(baseDir.resolveFromRootOrRelative(taskDir))
//                        }
//                    }
}
