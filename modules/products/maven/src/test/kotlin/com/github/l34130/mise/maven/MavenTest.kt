package com.github.l34130.mise.maven

import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.idea.maven.project.MavenInSpecificPath
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.nio.file.Files
import java.nio.file.Path

class MavenTest : BasePlatformTestCase() {
    private lateinit var settingsDir: Path

    override fun setUp() {
        super.setUp()
        settingsDir = Files.createTempDirectory("mise-maven-settings").toRealPath()
    }

    override fun tearDown() {
        try {
            settingsDir.toFile().deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun `test MavenInSpecificPath stores and returns home path`() {
        val path = "/fake/maven/3.9.6"
        val homeType = MavenInSpecificPath(path)
        assertEquals(path, homeType.mavenHome)
    }

    fun `test settings options and quoted paths`() {
        assertEquals("/home/user/.m2/settings.xml", mavenUserSettingsPath("-B --settings /home/user/.m2/settings.xml verify"))
        assertEquals("my settings.xml", mavenUserSettingsPath("-s \"my settings.xml\""))
        assertEquals("my settings.xml", mavenUserSettingsPath("--settings='my settings.xml'"))
        assertEquals("settings.xml", mavenUserSettingsPath("-s=settings.xml"))
        assertEquals("C:\\Users\\me\\settings.xml", mavenUserSettingsPath("""--settings "C:\Users\me\settings.xml""""))
    }

    fun `test absent or incomplete settings options leave no path`() {
        val arguments = listOf(
            null, "", "-B verify", "--global-settings global.xml", "--settings", "-s -B",
            "--settings=", "--settings ''", "-- --settings ignored.xml",
        )
        for (args in arguments) {
            assertNull(mavenUserSettingsPath(args))
        }
    }

    fun `test settings change independently of Maven home`() {
        val settings = MavenProjectsManager.getInstance(project).generalSettings
        val home = settings.mavenHomeType
        val first = Files.writeString(settingsDir.resolve("first settings.xml"), "<settings/>").toFile()
        val second = Files.writeString(settingsDir.resolve("second.xml"), "<settings/>").toFile()
        val setup = MiseProjectMavenSetup()

        setup.syncUserSettings(project, "--settings '${first.path}'")
        assertEquals(first.path, settings.userSettingsFile)
        setup.syncUserSettings(project, "-s ${second.path}")
        assertEquals(second.path, settings.userSettingsFile)
        assertEquals(home, settings.mavenHomeType)

        setup.syncUserSettings(project, null)
        setup.syncUserSettings(project, "--settings")
        assertEquals(second.path, settings.userSettingsFile)
    }

    fun `test relative settings paths resolve against project directory`() {
        val file = Files.writeString(settingsDir.resolve("settings.xml"), "<settings/>").toFile()
        val relativePath = Path.of(project.guessMiseProjectPath()).relativize(Path.of(file.path))
        MiseProjectMavenSetup().syncUserSettings(project, "--settings '$relativePath'")
        assertEquals(file.path, MavenProjectsManager.getInstance(project).generalSettings.userSettingsFile)
    }

    fun `test missing settings files preserve existing selection`() {
        val settings = MavenProjectsManager.getInstance(project).generalSettings
        settings.setUserSettingsFile("/existing/settings.xml")
        try {
            MiseProjectMavenSetup().syncUserSettings(project, "--settings missing-settings.xml")
            fail("Missing settings file should be rejected")
        } catch (_: IllegalArgumentException) {
            assertEquals("/existing/settings.xml", settings.userSettingsFile)
        }
    }
}
