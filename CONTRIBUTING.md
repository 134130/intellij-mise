# Contributing to the IntelliJ Mise Plugin

Thank you for your interest in contributing to the IntelliJ Mise Plugin!

## Running other IDEs for testing

As this plugin targets multiple IDEs, when developing you'll want to test against a specific IDE.

You can get a list of available tasks for running the plugin in different IDEs by running the following command:

```shell
./gradlew tasks --group='intellij platform' | grep -E '^run'
```

> [!NOTE]
> In many cases, Intellij Ultimates supports other products via plugins. Maybe you just need `runIntellijIdeaUltimate`

After that, you can run the desired IDE by running the following command:

```shell
./gradlew prepareSandbox_runIntellijIdeaUltimate # only for the first time
./gradlew runIntellijIdeaUltimate
```

> [!NOTE]
> If you want to use breakpoints for debugging, you can just run the gradle task with IDEA's debug option


> [!TIP]
> If you want to test other version of IDEs, you can change the version field in the [build.gradle.kts](./build.gradle.kts) file.
