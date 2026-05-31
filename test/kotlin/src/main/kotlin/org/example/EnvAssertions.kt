package org.example

object EnvAssertions {
    fun requireMiseEnv(scenario: String) {
        val value = System.getenv("MISE_KOTLIN")
        println("$scenario MISE_KOTLIN = $value")
        check(value == "true") {
            "$scenario expected MISE_KOTLIN=true, but was $value"
        }
    }
}
