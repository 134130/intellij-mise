<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Mise Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/134130/intellij-mise/compare/v2.3.0...HEAD
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
