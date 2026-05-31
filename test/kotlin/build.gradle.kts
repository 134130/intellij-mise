plugins {
    id("dev.nx.gradle.project-graph") version("0.1.9")
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:3.5.14")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5-jvm:6.1.11")
    implementation(kotlin("stdlib"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
allprojects {
    apply {
        plugin("dev.nx.gradle.project-graph")
    }
}
