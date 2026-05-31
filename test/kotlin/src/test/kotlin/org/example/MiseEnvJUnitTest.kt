package org.example

import kotlin.test.Test

class MiseEnvJUnitTest {
    @Test
    fun `Kotlin test receives mise env`() {
        EnvAssertions.requireMiseEnv("Kotlin test")
    }
}
