package com.github.l34130.mise.core.command

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

/**
 * Downloads and caches real mise binaries from GitHub Releases for integration tests.
 * Binaries are cached in a shared temp directory to avoid re-downloading across test runs.
 */
object MiseBinaryFixture {
    private val cacheDir: Path by lazy {
        val dir = Path.of(System.getProperty("java.io.tmpdir"), "mise-test-binaries")
        Files.createDirectories(dir)
        dir
    }

    /**
     * Returns the path to a mise binary for the given version.
     * Downloads it from GitHub Releases if not already cached.
     */
    fun getBinary(version: String): Path {
        val binaryName = buildAssetName(version)
        val cachedBinary = cacheDir.resolve(binaryName)

        if (Files.exists(cachedBinary) && Files.size(cachedBinary) > 0) {
            return cachedBinary
        }

        val url = "https://github.com/jdx/mise/releases/download/v$version/$binaryName"
        downloadFile(url, cachedBinary)
        makeExecutable(cachedBinary)
        return cachedBinary
    }

    private fun buildAssetName(version: String): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val platform = when {
            os.contains("mac") || os.contains("darwin") -> "macos"
            os.contains("linux") -> "linux"
            os.contains("windows") -> "windows"
            else -> error("Unsupported OS: $os")
        }

        val architecture = when {
            arch == "aarch64" || arch == "arm64" -> "arm64"
            arch == "amd64" || arch == "x86_64" -> "x64"
            else -> error("Unsupported architecture: $arch")
        }

        return "mise-v$version-$platform-$architecture"
    }

    private fun downloadFile(url: String, target: Path) {
        URI(url).toURL().openStream().use { input ->
            Files.newOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun makeExecutable(path: Path) {
        val os = System.getProperty("os.name").lowercase()
        if (!os.contains("windows")) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"))
        }
    }
}
