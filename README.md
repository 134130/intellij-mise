[![Build](https://github.com/134130/intellij-mise/workflows/Build/badge.svg)](https://github.com/134130/intellij-mise/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/24904-mise.svg)](https://plugins.jetbrains.com/plugin/24904-mise)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/24904-mise.svg)](https://plugins.jetbrains.com/plugin/24904-mise)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/24904)](https://plugins.jetbrains.com/plugin/24904-mise)


<!-- Plugin description -->
# Mise

**[GitHub](https://github.com/134130/intellij-mise)** | **[Issue](https://github.com/134130/intellij-mise/issues)** | **[Changelog](https://github.com/134130/intellij-mise/blob/main/CHANGELOG.md)**

**Mise** is a plugin for JetBrains IDEs that provides integration with [mise-en-place](https://mise.jdx.dev)


## Features

- **Environment Variables**: Set environment variables for your run configurations from `mise.toml` files.
  - See [Supported Run configurations](#supported-run-configurations) for the list of supported Run configurations.
- **Tool Integration**: Set project's SDK automatically from `mise.toml` files.
  - `java`, `go`, `node`, `deno` SDKs are supported.
- **Language Support**: Provides language features for `mise.toml` files.
  - Code completion
  - Reference (refactoring)
  - Language Injection
- **Diagram Support**: Provides task dependency diagrams.

## Supported Run configurations
- **JVM**
  - Java
  - Kotlin
  - JUnit
  - Gradle
- **Go**
  - Go Build
  - Go Test
- **Python**
- **Node.js**
  - Node.js
  - npm
  - Deno 
- **C#**
- _Submit issue if you need others_

<!-- Plugin description end -->

## Screenshots

![main](https://github.com/user-attachments/assets/e668d651-9d39-497e-b1b6-d9f05d5c3232)

![language-completion](https://github.com/user-attachments/assets/073644b8-2189-411d-b3a9-64c207ac44e4)

![language-reference](https://github.com/user-attachments/assets/426f0f1b-6824-4dc5-8ab9-e52f76cd9f69)

![run](https://github.com/user-attachments/assets/6b5ef885-17d0-4865-ac3e-150a588d4d02)

![gif](https://github.com/user-attachments/assets/dc4681b5-4a00-4629-9832-2dfe6407da96)

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Mise"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/134130/intellij-mise/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Ecosystem

- See [mise-vscode](https://github.com/hverlin/mise-vscode/) if you are looking for a similar plugin for VSCode
- [Mise documentation](https://mise.jdx.dev/)

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
