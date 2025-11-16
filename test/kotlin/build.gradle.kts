plugins {
    id("dev.nx.gradle.project-graph") version("0.1.9")
    kotlin("jvm") version "2.1.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
allprojects {
    apply {
        plugin("dev.nx.gradle.project-graph")
    }
}