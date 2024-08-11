# Contributing to the IntelliJ Mise Plugin

## Running other IDEs for testing
As this plugin targets multiple IDEs, when developing you'll want to test against a specific IDE.

You can get a list of available tasks for running the plugin in different IDEs by running the following command:
```shell
./gradlew tasks --group='intellij platform' | grep -E '^run'
```

> [!TIP]
> If you want to test other version of IDEs, you can change the version field in the [build.gradle.kts](./build.gradle.kts) file.
