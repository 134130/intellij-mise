package com.github.l34130.mise.database

data class MiseDatabaseAuthConfig(
    var usernameKey: String = "",
    var passwordKey: String = "",
) {
    companion object {
        const val PROP_USERNAME_KEY = "username"
        const val PROP_PASSWORD_KEY = "password"

        const val PROP_USERNAME_KEY_DEFAULT = "DB_USERNAME"
        const val PROP_PASSWORD_KEY_DEFAULT = "DB_PASSWORD"
    }
}
