package com.github.l34130.mise.pycharm.sdk

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.python.PythonModuleTypeBase
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUtil
import kotlin.reflect.KClass

class MisePythonSdkSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project): MiseDevToolName = MiseDevToolName("python")

    override fun defaultAutoConfigure(project: Project): Boolean = false

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        checkUvEnabled(project)

        val desiredHomePath = tool.resolvePythonPath(project)
        val targetModules = resolveTargetModules(project)
        val projectPythonSdk =
            ReadAction.compute<Sdk?, Throwable> {
                resolveProjectPythonSdk(project)
            }
        val projectSdkHomePath = projectPythonSdk?.homePath
        val projectMatches = projectSdkHomePath != null && FileUtil.pathsEqual(projectSdkHomePath, desiredHomePath)

        if (logger.isTraceEnabled) {
            val moduleNames = targetModules.joinToString { it.name }
            logger.trace(
                "Python SDK check: desiredHomePath='$desiredHomePath', " +
                    "projectSdk='${projectPythonSdk?.name}', targetModules=[$moduleNames]"
            )
        }

        val updates = mutableListOf<SdkStatus.NeedsUpdate>()

        if (projectPythonSdk != null && !projectMatches) {
            updates.add(
                SdkStatus.NeedsUpdate(
                    currentSdkVersion = projectPythonSdk.versionString,
                    currentSdkLocation = SdkLocation.Project,
                    configureAction = { applyProjectSdk(tool, project) },
                )
            )
        }

        if (targetModules.isNotEmpty()) {
            val moduleUpdates =
                ReadAction.compute<List<SdkStatus.NeedsUpdate>, Throwable> {
                    targetModules.mapNotNull { module ->
                        val rootManager = ModuleRootManager.getInstance(module)
                        val isInherited = rootManager.isSdkInherited || rootManager.sdk == null
                        if (isInherited && projectPythonSdk != null) {
                            if (logger.isTraceEnabled) {
                                logger.trace("Python SDK module check: module='${module.name}', inherited=true, skipped")
                            }
                            return@mapNotNull null
                        }

                        val moduleSdk = rootManager.sdk
                        val pythonModuleSdk = moduleSdk?.takeIf { PythonSdkUtil.isPythonSdk(it) }
                        val sdkHomePath = pythonModuleSdk?.homePath
                        val matches = sdkHomePath != null && FileUtil.pathsEqual(sdkHomePath, desiredHomePath)
                        if (logger.isTraceEnabled) {
                            logger.trace(
                                "Python SDK module check: module='${module.name}', " +
                                    "sdk='${pythonModuleSdk?.name}', sdkHomePath='$sdkHomePath', matches=$matches"
                            )
                        }
                        if (matches) {
                            return@mapNotNull null
                        }

                        SdkStatus.NeedsUpdate(
                            currentSdkVersion = pythonModuleSdk?.versionString,
                            currentSdkLocation = SdkLocation.Module(module.name),
                            configureAction = { applyModuleSdk(tool, project, module) },
                        )
                    }
                }
            updates.addAll(moduleUpdates)
        }

        if (updates.isEmpty()) {
            if (logger.isTraceEnabled && targetModules.isEmpty()) {
                logger.trace("Python SDK check: no target modules found and no mismatches detected.")
            }
            return SdkStatus.UpToDate
        }

        return if (updates.size == 1) {
            updates.first()
        } else {
            SdkStatus.MultipleNeedsUpdate(updates)
        }
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ) {
        applyProjectSdk(tool, project)
    }

    override fun <T : Configurable> getSettingsConfigurableClass(): KClass<out T>? = null

    private fun checkUvEnabled(project: Project) {
        val configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment
        val useUv =
            // Check if the 'settings.python.uv_venv_auto' is set to true
            MiseCommandLineHelper
                .getConfig(project, project.guessMiseProjectPath(), configEnvironment, "settings.python.uv_venv_auto")
                .getOrNull()
                ?.trim()
                ?.toBoolean() ?: false

        if (!useUv) {
            throw UnsupportedOperationException("Mise Python SDK setup requires 'settings.python.uv_venv_auto' to be true.")
        }
    }
}

private fun MiseDevTool.resolvePythonPath(project: Project): String {
    return MiseCommandLineHelper.getBinPath("python", project)
        .getOrElse { throw IllegalStateException("Failed to find Python executable ($resolvedInstallPath): ${it.message}", it) }
}

private fun MiseDevTool.ensureUvSdk(project: Project): Sdk {
    val sdkName = uvSdkName()
    val sdk = ProjectJdkImpl(
        sdkName,
        PythonSdkType.getInstance(),
        resolvePythonPath(project),
        resolvedVersion,
    )

    val registeredSdk =
        WriteAction.computeAndWait<Sdk, Throwable> {
        val table = ProjectJdkTable.getInstance()
        val existing = PythonSdkUtil.getAllSdks().firstOrNull { it.name == sdkName }
        if (existing == null) {
            table.addJdk(sdk)
            sdk
        } else {
            table.updateJdk(existing, sdk)
            existing
        }
    }

    PythonSdkType.getInstance().setupSdkPaths(registeredSdk)
    return registeredSdk
}

private fun resolveProjectPythonSdk(project: Project): Sdk? {
    val projectSdk = ProjectRootManager.getInstance(project).projectSdk
    return projectSdk?.takeIf { PythonSdkUtil.isPythonSdk(it) }
}

private fun applyProjectSdk(
    tool: MiseDevTool,
    project: Project,
) {
    val newSdk = tool.ensureUvSdk(project)
    WriteAction.computeAndWait<Unit, Throwable> {
        ProjectRootManager.getInstance(project).projectSdk = newSdk
    }
}

private fun applyModuleSdk(
    tool: MiseDevTool,
    project: Project,
    module: Module,
) {
    val newSdk = tool.ensureUvSdk(project)
    WriteAction.computeAndWait<Unit, Throwable> {
        ModuleRootModificationUtil.setModuleSdk(module, newSdk)
    }
}

private fun resolveTargetModules(project: Project): List<Module> {
    return ReadAction.compute<List<Module>, Throwable> {
        val modules = ModuleManager.getInstance(project).modules.toList()
        if (modules.isEmpty()) return@compute emptyList()

        val pythonModules = modules.filter { ModuleType.get(it) is PythonModuleTypeBase<*> }
        if (pythonModules.isNotEmpty()) return@compute pythonModules

        val configuredModules = modules.filter { resolveModulePythonSdk(it) != null }
        if (configuredModules.isNotEmpty()) return@compute configuredModules

        if (modules.size == 1) return@compute modules

        val basePath = project.basePath ?: return@compute emptyList()
        val baseModule =
            modules.firstOrNull { module ->
                ModuleRootManager.getInstance(module).contentRoots.any { it.path == basePath }
            }
        baseModule?.let { listOf(it) } ?: emptyList()
    }
}

private fun resolveModulePythonSdk(module: Module): Sdk? {
    val moduleSdk = ModuleRootManager.getInstance(module).sdk
    if (moduleSdk != null && PythonSdkUtil.isPythonSdk(moduleSdk)) return moduleSdk
    return PythonSdkUtil.findPythonSdk(module)
}

private fun MiseDevTool.uvSdkName(): String {
    val displayVersion = this.displayVersion
    return if (displayVersion.isBlank()) {
        "uv (python)"
    } else {
        "uv (python $displayVersion)"
    }
}

private val logger = logger<MisePythonSdkSetup>()
