package com.github.l34130.mise.database

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.dataSource.DatabaseAuthProvider
import com.intellij.database.dataSource.DatabaseConnectionConfig
import com.intellij.database.dataSource.DatabaseConnectionInterceptor
import com.intellij.database.dataSource.DatabaseConnectionPoint
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls

@Suppress("UnstableApiUsage")
class MiseDatabaseAuthProvider : DatabaseAuthProvider {
    override fun getId(): @NonNls String = "mise"

    override fun getDisplayName(): @Nls String = "Mise"

    override fun getApplicability(
        point: DatabaseConnectionPoint,
        level: DatabaseAuthProvider.ApplicabilityLevel,
    ): DatabaseAuthProvider.ApplicabilityLevel.Result = DatabaseAuthProvider.ApplicabilityLevel.Result.APPLICABLE

    override suspend fun interceptConnection(
        proto: DatabaseConnectionInterceptor.ProtoConnection,
        silent: Boolean,
    ): Boolean {
        val usernameKey =
            proto.connectionPoint.getAdditionalProperty(MiseDatabaseAuthConfig.PROP_USERNAME_KEY)
                ?: MiseDatabaseAuthConfig.PROP_USERNAME_KEY_DEFAULT
        val passwordKey =
            proto.connectionPoint.getAdditionalProperty(MiseDatabaseAuthConfig.PROP_PASSWORD_KEY)
                ?: MiseDatabaseAuthConfig.PROP_PASSWORD_KEY_DEFAULT

        val settings = proto.project.service<MiseProjectSettings>().state
        if (!settings.useMiseDirEnv || !settings.useMiseInDatabaseAuthentication) return false

        val envVars =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                MiseCommandLineHelper.getEnvVars(
                    project = proto.project,
                    workDir = proto.project.guessMiseProjectPath(),
                    configEnvironment = settings.miseConfigEnvironment,
                )
            }.getOrThrow()

        val username = envVars[usernameKey]
        val password = envVars[passwordKey]

        if (username.isNullOrBlank()) {
            if (password.isNullOrBlank()) {
                throw IllegalArgumentException("Environment variable '$usernameKey' and '$passwordKey' are unset")
            }
            throw IllegalArgumentException("Environment variable '$usernameKey' is unset")
        } else if (password.isNullOrBlank()) {
            throw IllegalArgumentException("Environment variable '$passwordKey' is unset")
        }

        proto.connectionProperties["user"] = username
        proto.connectionProperties["password"] = password

        return true
    }

    override fun createWidget(
        project: Project?,
        credentials: DatabaseCredentials,
        config: DatabaseConnectionConfig,
    ): DatabaseAuthProvider.AuthWidget = MiseAuthWidget()
}
