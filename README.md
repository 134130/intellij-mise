![Build](https://github.com/134130/intellij-mise/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/24904-mise.svg)](https://plugins.jetbrains.com/plugin/24904-mise)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/24904-mise.svg)](https://plugins.jetbrains.com/plugin/24904-mise)

<!-- Plugin description -->
# Mise

**[GitHub](https://github.com/134130/intellij-mise)** | **[Issue](https://github.com/134130/intellij-mise/issues)** | **[Changelog](https://github.com/134130/intellij-mise/blob/main/CHANGELOG.md)**

**Mise** is a plugin for JetBrains IDEs that allows you to set environment variables for your run configurations
from Mise config files. see: **[mise-en-place](https://mise.jdx.dev)**

## Features

- **Tool Integration**: Set project's SDK automatically from `mise.toml` files.
  - `java`, `go`, `node`, `deno` SDKs are supported.
- **Environment Variables**: Set environment variables for your run configurations from `mise.toml` files.

## Supported Platforms
- **IntelliJ IDEA**
- **GoLand**
- **WebStorm**
- **Rider**
- _Submit issue if you need other IDE_

<!-- Plugin description end -->

## Screenshots

![main](https://github.com/user-attachments/assets/e668d651-9d39-497e-b1b6-d9f05d5c3232)

![run](https://github.com/user-attachments/assets/6b5ef885-17d0-4865-ac3e-150a588d4d02)

![run-gif](https://github.com/user-attachments/assets/1af82256-954c-4b3c-afcc-6bc8dd1f44a1)

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Mise"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/134130/intellij-mise/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
