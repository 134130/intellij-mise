package com.github.l34130.mise.core.run

enum class ConfigEnvironmentStrategy(
    val value: String,
) {
    USE_PROJECT_SETTINGS("use_project_settings"),
    OVERRIDE_PROJECT_SETTINGS("override_project_settings"),
    ;

    companion object {
        fun from(value: String): ConfigEnvironmentStrategy? = entries.firstOrNull { it.value == value }
    }
}
