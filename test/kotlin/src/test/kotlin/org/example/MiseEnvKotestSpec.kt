package org.example

import io.kotest.core.spec.style.StringSpec

class MiseEnvKotestSpec : StringSpec({
    "Kotest receives mise env" {
        EnvAssertions.requireMiseEnv("Kotest")
    }
})
