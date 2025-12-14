package com.github.l34130.mise.core.command

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.l34130.mise.core.setting.MiseApplicationSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.application
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service(Service.Level.APP)
class MiseCommandService(
    private val cs: CoroutineScope,
) {
    val settings = application.service<MiseApplicationSettings>()
    private val cache: Cache<String, Any> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(1.seconds.toJavaDuration())
            .build()

    suspend fun getEnvVars(
        workDir: String?,
        configEnvironment: String?,
    ): Result<Map<String, String>> {
        cache.getIfPresent("getEnvVars(workDir=$workDir,configEnvironment=$configEnvironment)")?.let {
            @Suppress("UNCHECKED_CAST")
            return Result.success(it as Map<String, String>)
        }

        val commandLineArgs = mutableListOf("env", "--json")

        val miseCommandLine = MiseCommandLine(workDir, configEnvironment)
        return miseCommandLine.runCommandLineAsync(commandLineArgs)
    }
}
