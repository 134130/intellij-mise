<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Mise Changelog

## [Unreleased]

## [5.8.0] - 2025-10-26

### Added

- Make VCS integration toggleable

### Fixed

- vcs: handle InterruptedException gracefully

## [5.7.2] - 2025-10-04

### Fixed

- PsiElement.getParent() is null
- Enhance PsiLocation initialization to avoid NPE
- Enhance stacktrace and add stackhash for Error Reporter

## [5.7.1] - 2025-10-03

### Fixed

- pnpm package manager setup is not working correctly

## [5.7.0] - 2025-09-25

### Added

- mise env support for VCS

### Fixed

- Remove `--offline` flag for `mise ls` command
- Threading violation when running Task with Sidebar

## [5.6.1] - 2025-09-15

### Added

- GOROOT reload support for Go 1.25+

## [5.6.0] - 2025-08-24

### Added

- Discovery subdirectory configuration files by @134130 in https://github.com/134130/intellij-mise/pull/318
- Reload Dev tools requires users' confirmation by @134130 in https://github.com/134130/intellij-mise/pull/316
- Separate NodeJS interpreter setup and Package manager setup by @134130 in https://github.com/134130/intellij-mise/pull/317

### Fixed

- GOROOT reload is not working correctly by @134130 in https://github.com/134130/intellij-mise/pull/316
- JS Package Manager reload is not working correctly by @134130 in https://github.com/134130/intellij-mise/pull/317

## [5.5.0] - 2025-08-02

### Added

- Make run configuration can override project settings by @134130 in https://github.com/134130/intellij-mise/pull/296
- Ruby SDK setup support by @134130 in https://github.com/134130/intellij-mise/pull/299
- Python SDK setup for `uv` by @134130 in https://github.com/134130/intellij-mise/pull/303

### Fixed

- `run` table contains a string array in a toml file doesn't support language injection by @134130 in https://github.com/134130/intellij-mise/pull/300
- Execute mise command on background thread by @134130 in https://github.com/134130/intellij-mise/pull/301

## [5.4.0] - 2025-07-06

### Added

- Add Ruby support by @134130 in https://github.com/134130/intellij-mise/pull/283
- Add support for args in run configurations by @bamorim in https://github.com/134130/intellij-mise/pull/281

### Fixed

- Improve command execution handling in terminal widget by @134130 in https://github.com/134130/intellij-mise/pull/282

## [5.3.3] - 2025-06-19

### Fixed

- Calling invokeAndWait from read-action leads to possible deadlock by @134130 in https://github.com/134130/intellij-mise/pull/272
- class `MnRunConfiguration` cannot be cast to class `GradleRunConfiguration` by @134130 in https://github.com/134130/intellij-mise/pull/273
- Read access is allowed from inside read-action only by @134130 in https://github.com/134130/intellij-mise/pull/274

## [5.3.2] - 2025-06-15

### Fixed

- Plugin verification failing with `Package 'com.intellij.deno' is not found` and `Package 'com.jetbrains.nodejs' is not found`

## [5.3.1] - 2025-06-08

### Fixed

- Test is cached when running tests with `gradle` and no source code changes
- Indicate progress when running Mise commands on background
- NullPointerException when indicating Mise file icon

## [5.3.0] - 2025-05-11

### Added

- getMiseExecutablePath supports WinGet path resolving by @134130 in https://github.com/134130/intellij-mise/pull/242

## [5.2.0] - 2025-05-01

### Added

- Add environment variable injection for C/C++ by @134130 in https://github.com/134130/intellij-mise/pull/237

### Fixed

- Invalid thread access for psi by @134130 in https://github.com/134130/intellij-mise/pull/235
- `ignore duplicated child at 1: Environment` warnings from AsyncTreeModel by @134130 in https://github.com/134130/intellij-mise/pull/236

## [5.1.0] - 2025-04-26

### Added

- Support structure tree by @134130 in https://github.com/134130/intellij-mise/pull/229

### Fixed

- Python: Run configration envvars are not restoring by @134130 in https://github.com/134130/intellij-mise/pull/230

## [5.0.0] - 2025-04-19

### Changed

- Change minimum support version from `2024.2` to `2025.1`

## [4.5.0] - 2025-04-06

### Changed

- Separate mise executable configuration to application scope
- Make mise toml task Run Configuration as portable
- Bump org.jetbrains.intellij.platform.settings from 2.4.0 to 2.5.0

### Fixed

- Read access is allowed from inside read-action in RunMiseTomlTaskAction
- Remove internal API usage: `IdeaReportingEvent`

## [4.4.1] - 2025-03-21

### Fixed

- Mise settings panel cannot be opened when `mise` is not installed
- Mise 'depends' with arguments occurs an `Cannot resolve symbol` error

## [4.4.0] - 2025-03-03

### Added

- Support argument passing for Mise Settings' `executable path` field
- Dependency graph diagram support for Mise Tasks

### Fixed

- Slow operation on `Run Mise Task` on ToolWindow

## [4.3.0] - 2025-02-22

### Added

- Add support for Mise Toml Tasks configured in `task_configs` section
- Add source comment for Mise ToolWindow

### Fixed

- Error report submitter always reports with title `null`
- Cannot run unresolved tasks on Mise ToolWindow
- Some IDEs are not bundled with `ShLanguage` plugin
- Ensure `mise` command is initialized before drawing the ToolWindow

## [4.2.1] - 2025-02-21

### Fixed

- Fix `nodejs` sdk is not loaded properly on Windows
- Fix Mise ToolWindow cannot run File tasks

## [4.2.0] - 2025-02-09

### Added

- Support sh language injection
- Use MiseRunConfiguration on running task on ToolWindow
- Add support for `Go to Declaration` on ToolWindow

### Fixed

- Add `wait_for` for tasks completion
- RunLineMarkerContributor is contributing on non-leaf nodes

## [4.1.0] - 2025-02-01

### Added

- Support run configuration for PyCharm Community & Professional

## [4.0.0] - 2025-01-31

### BREAKING CHANGE

- Change minimum support version from `2023.3` to `2024.2` to support `2025.1+` IDEs

## [3.5.0] - 2025-01-30

### Added

- Use index for Mise task code completion.
- Support file task code completion.
- Support task code completion over project's base directory. (was only working on the `mise.toml` file)
- Support documentation for Mise task code completion.

### Fixed

- Refactoring `mise.toml` files are not working properly.

## [3.4.1] - 2025-01-22

### Fixed

- Failed to load `MiseTomlDocumentationProvider`
- stacktrace for error reporter is too short

## [3.4.0] - 2025-01-19

### Added

- Support code completion for mise file tasks.

### Fixed

- Remove JsonSchema Provider for Mise file which is not working properly.
- Use IconProvider for Mise file to correctly show the icon.

## [3.3.0] - 2025-01-16

### Added

- Run Mise task as a run configuration.
- Unit tests for Mise code completion.

### Fixed

- Code completion is not working on `depends` single string.
- Mise task with quoted string cannot escape the quotes.

## [3.2.6] - 2025-01-01

### Fixed

- Jackson Deserialization failing on Unknown properties

## [3.2.5] - 2024-12-28

### Fixed

- Mise executable customization is not working
- Address new version of `mise tasks --json` output

## [3.2.4] - 2024-12-28

### Fixed

- Error report submitter cannot report an error when the error is too long.
- Mise Tools name is not showing correctly.
- Escape long-running tasks running on EDT.

### Added

- Add a mise cli version to the error report.

## [3.2.3] - 2024-12-26

### Fixed

- Fix environment variables are not loaded onto Gradle Task when triggered on Gradle Tool Window.

## [3.2.2] - 2024-12-25

### Fixed

- Fix runConfiguration cannot be saved
- Fix runConfiguration cannot load envvars when it doesn't have working directory.
  - When the working directory is not set, it will use the project's base directory.

## [3.2.1] - 2024-12-16

### Added

- Introduce error reporter for more convenient error reporting

## [3.2.0] - 2024-12-08

### Added

- Support `mise trust` on UI
- Add toolbar for Mise Tool window

### Fixed

- Handle if `mise` command is not installed
- Use mise --env if available

## [3.1.0] - 2024-11-23

### Added

- Support mise executable path configuration

## [3.0.0] - 2024-11-18

### BREAKING CHANGE

- Change minimum support version from `2022.2` to `2023.3` to support `2024.3+` IDEs

## [2.6.2] - 2024-11-16

- fix: Do not check DevTool name's lowercase by @134130 in https://github.com/134130/intellij-mise/pull/88

## [2.6.1] - 2024-11-15

### Fixed

- 2024.3 Compatibility

## [2.6.0] - 2024-11-15

### Added

- Trust config files with Button

### Fixed

- Tools Window is now beautified
- Drop Schema provider for `mise.toml` files
- Some performance improvements

## [2.5.0] - 2024-11-12

### Added

- Add mise settings to preferences
- Add mise panel (display tasks, env, tool)
- Add run button in `mise.toml` files
- Support autocomplete in `mise.toml` files

### Fixed

- Fix Node.js envvars are overriding runconfiguraion's envvars

## [2.4.0] - 2024-11-10

### Added

- Automatically setup NodeJS Interpreter and Package Manager

### Fixed

- Fix Deno's Path is not configured to executable, but home directory.

## [2.3.0] - 2024-11-09

### Added

- Support auto-configuration for Deno.

## [2.2.0] - 2024-11-09

### Added

- Use `--offline` flag for `mise ls` if available.

### Changed

- Fix NodeJS's run configuration is not working on IDEA.

## [2.1.1] - 2024-10-03

### Changed

- Unset the untilBuild property to automatically include future IntelliJ versions

## [2.1.0] - 2024-09-18

### Added

- Add run configurations support for Rider.
- Rename from 'mise' to 'Mise'.

## [2.0.1] - 2024-08-31

### Added

- Support automatic tools update for GoLand.

## [2.0.0] - 2024-08-03

### Changed

- Migrate to intellij-platform-gradle-plugin v2.0.0
- Support JetBrains 2024.2+ IDEs

### Removed

- Drop not perfectly working feature for the PyCharm Community & Professional

## [1.3.0] - 2024-08-03

### Added

- Add environment variables (run configuration) support for the PyCharm Community & Professional

## [1.2.1] - 2024-07-24

- Remove screenshots from plugin.xml. (JetBrains Marketplace guidelines)

## [1.2.0] - 2024-07-21

- Add support for the Go SDK (GOROOT) integration from mise tools.

## [1.1.0] - 2024-07-20

### Added

- Add support for the JDK integration from mise tools.

## [1.0.1] - 2024-07-03

### Added

- Support JDK integration from mise tools.

[Unreleased]: https://github.com/134130/intellij-mise/compare/v5.8.0...HEAD
[5.8.0]: https://github.com/134130/intellij-mise/compare/v5.7.2...v5.8.0
[5.7.2]: https://github.com/134130/intellij-mise/compare/v5.7.1...v5.7.2
[5.7.1]: https://github.com/134130/intellij-mise/compare/v5.7.0...v5.7.1
[5.7.0]: https://github.com/134130/intellij-mise/compare/v5.6.1...v5.7.0
[5.6.1]: https://github.com/134130/intellij-mise/compare/v5.6.0...v5.6.1
[5.6.0]: https://github.com/134130/intellij-mise/compare/v5.5.0...v5.6.0
[5.5.0]: https://github.com/134130/intellij-mise/compare/v5.4.0...v5.5.0
[5.4.0]: https://github.com/134130/intellij-mise/compare/v5.3.3...v5.4.0
[5.3.3]: https://github.com/134130/intellij-mise/compare/v5.3.2...v5.3.3
[5.3.2]: https://github.com/134130/intellij-mise/compare/v5.3.1...v5.3.2
[5.3.1]: https://github.com/134130/intellij-mise/compare/v5.3.0...v5.3.1
[5.3.0]: https://github.com/134130/intellij-mise/compare/v5.2.0...v5.3.0
[5.2.0]: https://github.com/134130/intellij-mise/compare/v5.1.0...v5.2.0
[5.1.0]: https://github.com/134130/intellij-mise/compare/v5.0.0...v5.1.0
[5.0.0]: https://github.com/134130/intellij-mise/compare/v4.5.0...v5.0.0
[4.5.0]: https://github.com/134130/intellij-mise/compare/v4.4.1...v4.5.0
[4.4.1]: https://github.com/134130/intellij-mise/compare/v4.4.0...v4.4.1
[4.4.0]: https://github.com/134130/intellij-mise/compare/v4.3.0...v4.4.0
[4.3.0]: https://github.com/134130/intellij-mise/compare/v4.2.1...v4.3.0
[4.2.1]: https://github.com/134130/intellij-mise/compare/v4.2.0...v4.2.1
[4.2.0]: https://github.com/134130/intellij-mise/compare/v4.1.0...v4.2.0
[4.1.0]: https://github.com/134130/intellij-mise/compare/v4.0.0...v4.1.0
[4.0.0]: https://github.com/134130/intellij-mise/compare/v3.5.0...v4.0.0
[3.5.0]: https://github.com/134130/intellij-mise/compare/v3.4.1...v3.5.0
[3.4.1]: https://github.com/134130/intellij-mise/compare/v3.4.0...v3.4.1
[3.4.0]: https://github.com/134130/intellij-mise/compare/v3.3.0...v3.4.0
[3.3.0]: https://github.com/134130/intellij-mise/compare/v3.2.6...v3.3.0
[3.2.6]: https://github.com/134130/intellij-mise/compare/v3.2.5...v3.2.6
[3.2.5]: https://github.com/134130/intellij-mise/compare/v3.2.4...v3.2.5
[3.2.4]: https://github.com/134130/intellij-mise/compare/v3.2.3...v3.2.4
[3.2.3]: https://github.com/134130/intellij-mise/compare/v3.2.2...v3.2.3
[3.2.2]: https://github.com/134130/intellij-mise/compare/v3.2.1...v3.2.2
[3.2.1]: https://github.com/134130/intellij-mise/compare/v3.2.0...v3.2.1
[3.2.0]: https://github.com/134130/intellij-mise/compare/v3.1.0...v3.2.0
[3.1.0]: https://github.com/134130/intellij-mise/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/134130/intellij-mise/compare/v2.6.2...v3.0.0
[2.6.2]: https://github.com/134130/intellij-mise/compare/v2.6.1...v2.6.2
[2.6.1]: https://github.com/134130/intellij-mise/compare/v2.6.0...v2.6.1
[2.6.0]: https://github.com/134130/intellij-mise/compare/v2.5.0...v2.6.0
[2.5.0]: https://github.com/134130/intellij-mise/compare/v2.4.0...v2.5.0
[2.4.0]: https://github.com/134130/intellij-mise/compare/v2.3.0...v2.4.0
[2.3.0]: https://github.com/134130/intellij-mise/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/134130/intellij-mise/compare/v2.1.1...v2.2.0
[2.1.1]: https://github.com/134130/intellij-mise/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/134130/intellij-mise/compare/v2.0.1...v2.1.0
[2.0.1]: https://github.com/134130/intellij-mise/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/134130/intellij-mise/compare/v1.3.0...v2.0.0
[1.3.0]: https://github.com/134130/intellij-mise/compare/v1.2.1...v1.3.0
[1.2.1]: https://github.com/134130/intellij-mise/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/134130/intellij-mise/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/134130/intellij-mise/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/134130/intellij-mise/commits/v1.0.1
