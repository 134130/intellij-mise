<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Mise Changelog

## [Unreleased]

## [3.1.0] - 2024-11-23

### Added

- Support mise executable path configuration

## [3.0.0] - 2024-11-18

### BREAKING CHANGE

- Change minimum support version from `2024.2` to `2023.3` to support `2024.3+` IDEs

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

[Unreleased]: https://github.com/134130/intellij-mise/compare/v3.1.0...HEAD
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
