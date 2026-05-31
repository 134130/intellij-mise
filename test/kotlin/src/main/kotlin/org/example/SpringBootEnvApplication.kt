package org.example

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SpringBootEnvApplication {
    @Bean
    fun verifyMiseEnv(): CommandLineRunner =
        CommandLineRunner {
            EnvAssertions.requireMiseEnv("Spring Boot main")
        }
}

fun main(args: Array<String>) {
    runApplication<SpringBootEnvApplication>(*args)
}
