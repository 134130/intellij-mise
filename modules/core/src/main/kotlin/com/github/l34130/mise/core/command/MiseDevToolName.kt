package com.github.l34130.mise.core.command

@JvmInline
value class MiseDevToolName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "DevTool name must not be blank" }
        // https://github.com/134130/intellij-mise/issues/87
        // require(value.isLowercase()) { "DevTool name must be lowercase" }
    }

    fun canonicalName(): String = toolNameToCanonicalName[value] ?: value.replaceFirstChar { it.uppercase() }

    companion object {
        private val toolNameToCanonicalName =
            mapOf(
                "go" to "Go",
                "java" to "Java",
                "kotlin" to "Kotlin",
                "node" to "Node.js",
                "python" to "Python",
                "ruby" to "Ruby",
                "rust" to "Rust",
                "deno" to "Deno",
                "flutter" to "Flutter",
                "dart" to "Dart",
                "swift" to "Swift",
                "c" to "C",
                "cpp" to "C++",
                "csharp" to "C#",
                "dotnet" to ".NET",
                "terraform" to "Terraform",
                "docker" to "Docker",
                "npm" to "npm",
                "pnpm" to "pnpm",
                "yarn" to "Yarn",
            )
    }

    override fun toString(): String = canonicalName()
}
