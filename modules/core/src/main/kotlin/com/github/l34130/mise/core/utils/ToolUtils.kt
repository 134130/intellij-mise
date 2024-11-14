package com.github.l34130.mise.core.utils

object ToolUtils {
    fun getCanonicalName(toolName: String): String =
        when (toolName) {
            "go" -> "Go"
            "java" -> "Java"
            "kotlin" -> "Kotlin"
            "node" -> "Node.js"
            "python" -> "Python"
            "ruby" -> "Ruby"
            "rust" -> "Rust"
            "deno" -> "Deno"
            "flutter" -> "Flutter"
            "dart" -> "Dart"
            "swift" -> "Swift"
            "c" -> "C"
            "cpp" -> "C++"
            "csharp" -> "C#"
            "dotnet" -> ".NET"
            "terraform" -> "Terraform"
            "docker" -> "Docker"
            else -> toolName
        }
}
