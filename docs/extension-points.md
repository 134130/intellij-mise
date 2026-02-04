# Extension Points

This plugin exposes extension points so other plugins can reuse the same SDK sync logic that is driven by `mise.toml` files.

## miseSdkSetup

Use this extension point to add SDK auto-configuration for a tool that the plugin does not ship out of the box.

### Registration (plugin.xml)

Your plugin must declare a dependency on the Mise plugin to register a provider. This can be either a required or optional dependency.

```xml
<!-- plugin.xml -->
<idea-plugin>
    <depends>com.github.l34130.mise</depends>

    <extensions defaultExtensionNs="com.github.l34130.mise">
        <miseSdkSetup implementation="com.example.mise.MyLanguageSdkSetup"/>
    </extensions>

    <actions>
        <action id="com.example.mise.MyLanguageSdkSetup"
                class="com.example.mise.MyLanguageSdkSetup"
                text="Reconfigure MyLanguage with mise">
            <add-to-group group-id="com.github.l34130.mise.actions"/>
        </action>
    </actions>
</idea-plugin>
```

If your plugin should work without the intellij-mise plugin installed, register the extension behind an optional dependency:

```xml
<!-- plugin.xml -->
<idea-plugin>
    <depends optional="true" config-file="mise-extensions.xml">com.github.l34130.mise</depends>
</idea-plugin>
```

```xml
<!-- mise-extensions.xml -->
<idea-plugin>
    <extensions defaultExtensionNs="com.github.l34130.mise">
        <miseSdkSetup implementation="com.example.mise.MyLanguageSdkSetup"/>
    </extensions>
    <actions>
        <action id="com.example.mise.MyLanguageSdkSetup"
                class="com.example.mise.MyLanguageSdkSetup"
                text="Reconfigure MyLanguage with mise">
            <add-to-group group-id="com.github.l34130.mise.actions"/>
        </action>
    </actions>
</idea-plugin>
```

### Implementation

Implement `com.github.l34130.mise.core.setup.AbstractProjectSdkSetup`. The base class handles the common workflow:

- Finds the matching tool from the combined output of `mise ls --local` and `mise ls --global`. Locally defined tools take precedence.  
- If the tool is not installed, will either prompt the user to install via a notification or automatically install if configured to do so. Installs run `mise install --raw --yes <tool>` in a Run tool window.
- If the tool is not configured as an SDK/interpreter, will either prompt the user with "Configure now" and "Always keep <SDK> in sync" actions or automatically configure the IDE.
- Calls your `checkSdkStatus` and `applySdkConfiguration` hooks on a background thread (use ReadAction/WriteAction when touching IDE state).

Defaults and settings integration:

- `defaultAutoInstall` and `defaultAutoConfigure` define the per-SDK defaults.
- Users can override these per SDK in "Mise Settings → SDK Setup".
- `getSettingsId` is the stable ID for storing user choices.
- `getSettingsDisplayName` is the label shown in the settings UI.

Key things to use from `MiseDevTool`:

- `displayVersion` is the requested version alias for user-facing text e.g., a tool definition of `java = "21"` gives a `displayVersion` of `21`, `java = "21.0.9"` gives `21.0.9`.
- `displayVersionWithResolved` adds the resolved version to the alias e.g., a tool definition of `java = "21"` that installs `21.0.9` would have `21 (21.0.9)`, we use this for notifications that may need to distinguish between SDKs that have the same alias.
- `resolvedVersion` returns the full resolved version string (not the alias) e.g., `21.0.9`.
- `resolvedInstallPath` returns the install directory (alias resolved) and converts WSL paths to UNC on Windows. This is not the binary path. e.g., `C:\Users\username\AppData\Local\mise\installs\java\21.0.9`, `\\wsl.localhost\Ubuntu\home\username\.local\share\mise\installs\java\21.0.9`, `/home/username/.local/share/mise/installs/java/21.0.9`
- Use `MiseCommandLineHelper.getBinPath("<tool>", project)` for executables. This resolves aliases to real paths, converts WSL paths correctly for the project context and always returns the full executable's name with any file extension.  

Minimal example pattern:

```kotlin
class MyLanguageSdkSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project) = MiseDevToolName("mytool")

    override fun checkSdkStatus(tool: MiseDevTool, project: Project): SdkStatus {
        // Compare IDE's current SDK to the resolved tool path.
        mySdkNeedsAnUpdate = yourFunctionToCheckIfTheSDKNeedsUpdating()
        return if (mySdkNeedsAnUpdate) {
            SdkStatus.NeedsUpdate(
                currentSdkVersion = null,
                currentSdkLocation = SdkLocation.Project, // or SdkLocation.Module(... ) or SdkLocation.Setting or SdkLocation.Custom(... )
            )
        } else {
            SdkStatus.UpToDate
        }
    }

    override fun applySdkConfiguration(tool: MiseDevTool, project: Project) {
        // Apply the SDK to the IDEs setup, see examples in this code base.
        yourFunctionToSetupTheIDEsSDK()
    }

    override fun <T : Configurable> getSettingsConfigurableClass(): KClass<out T>? = null
}
```

To automatically install a missing tool when it is detected override `defaultAutoInstall` to return true. 

To automatically configure the SDK in the IDE when a difference is detected, override `defaultAutoConfigure` to return true.

### SDK target patterns (use the one that matches your IDE)

The in-tree implementations are intended to be exemplars and cover the most common SDK storage models in JetBrains IDEs. Pick the pattern that matches the IDE API you are integrating with. If your plugin supports both IDEA and the language specific IDEs, you may need to implement both methods of configuration. 

#### 1) Project SDK (ProjectRootManager + ProjectJdkTable)

Use this when the IDE stores the language SDK at the project level.

Steps:
- Read `ProjectRootManager.getInstance(project).projectSdk`.
- Compare to the mise tool (typically by SDK home path or SDK name).
- Return `SdkStatus.NeedsUpdate(..., currentSdkLocation = SdkLocation.Project)`.
- In `applySdkConfiguration`, add or update the SDK in `ProjectJdkTable` and set
  `ProjectRootManager.getInstance(project).projectSdk = newSdk`.

Reference implementations:
- Java: [`MiseProjectJdkSetup.kt`](../modules/products/idea/src/main/kotlin/com/github/l34130/mise/idea/jdk/MiseProjectJdkSetup.kt) 
- Go: [`MiseProjectGoSdkSetup.kt`](../modules/products/goland/src/main/kotlin/com/github/l34130/mise/goland/go/MiseProjectGoSdkSetup.kt)
- Ruby: [`MiseProjectRubySdkSetup.kt`](../modules/products/ruby/src/main/kotlin/com/github/l34130/mise/ruby/sdk/MiseProjectRubySdkSetup.kt)

#### 2) Module SDK (ModuleRootManager + ModuleRootModificationUtil)

Use this when modules can override or inherit the project SDK (Python).

Rules implemented in-tree:
- If the project SDK is a Python SDK, check it first.
- For each module:
  - If the module inherits the project SDK and the project SDK is Python, skip it.
  - If the module does not inherit, compare its SDK directly.
- If multiple modules need changes, return `SdkStatus.MultipleNeedsUpdate` and supply a `configureAction` per module so each notification targets one module.

Apply:
- `ModuleRootModificationUtil.setModuleSdk(module, newSdk)`.

Reference implementation:
- Python: [MisePythonSdkSetup.kt](../modules/products/pycharm/src/main/kotlin/com/github/l34130/mise/pycharm/sdk/MisePythonSdkSetup.kt)

#### 3) Settings-based interpreter path

Use this when the IDE stores an executable path in settings.

Steps:
- Read the current path in a `ReadAction`.
- Resolve the desired path via `MiseCommandLineHelper.getBinPath`.
- Return `SdkStatus.NeedsUpdate(..., currentSdkLocation = SdkLocation.Setting)`.
- In `applySdkConfiguration`, update the settings in a `WriteAction`.

Reference implementation:
- Node interpreter: [`MiseProjectInterpreterSetup.kt`](../modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/node/MiseProjectInterpreterSetup.kt)

#### 4) Settings-based tool path (non-interpreter)

Use this when the IDE stores a tool path in settings (e.g., Deno).

Reference implementation:
- Deno: [`MiseProjectDenoSetup.kt`](../modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/deno/MiseProjectDenoSetup.kt)

#### 5) Settings-based package manager

Use this when the IDE stores a package manager path in settings (e.g., npm/yarn/pnpm).

Reference implementation:
- Node package manager: [`MiseProjectPackageSetup.kt`](../modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/node/MiseProjectPackageSetup.kt)

#### 6) Custom SDK locations

If your SDK lives somewhere else (remote targets, custom settings), set `currentSdkLocation = SdkLocation.Custom("YourLabel")` so notifications describe the source clearly. Use `configureAction` to scope updates to that location when the default `applySdkConfiguration` is too broad.

### Reference implementations

These are the in-tree providers that show typical usage:

- Java (project SDK): [`modules/products/idea/src/main/kotlin/com/github/l34130/mise/idea/jdk/MiseProjectJdkSetup.kt`](../modules/products/idea/src/main/kotlin/com/github/l34130/mise/idea/jdk/MiseProjectJdkSetup.kt)
- Go (project SDK): [`modules/products/goland/src/main/kotlin/com/github/l34130/mise/goland/go/MiseProjectGoSdkSetup.kt`](../modules/products/goland/src/main/kotlin/com/github/l34130/mise/goland/go/MiseProjectGoSdkSetup.kt)
- Ruby (project SDK): [`modules/products/ruby/src/main/kotlin/com/github/l34130/mise/ruby/sdk/MiseProjectRubySdkSetup.kt`](../modules/products/ruby/src/main/kotlin/com/github/l34130/mise/ruby/sdk/MiseProjectRubySdkSetup.kt)
- Python (module SDK): [`modules/products/pycharm/src/main/kotlin/com/github/l34130/mise/pycharm/sdk/MisePythonSdkSetup.kt`](../modules/products/pycharm/src/main/kotlin/com/github/l34130/mise/pycharm/sdk/MisePythonSdkSetup.kt)
- Node interpreter (settings path): [`modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/node/MiseProjectInterpreterSetup.kt`](../modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/node/MiseProjectInterpreterSetup.kt)
- Node package manager (settings path): [`modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/node/MiseProjectPackageSetup.kt`](../modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/node/MiseProjectPackageSetup.kt)
- Deno (settings path): [`modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/deno/MiseProjectDenoSetup.kt`](../modules/products/nodejs/src/main/kotlin/com/github/l34130/mise/nodejs/deno/MiseProjectDenoSetup.kt)

### Registering the Reconfigure Action

Reconfiguring the IDE with the SDK specified in the mise.toml is handled via an Action which will appear in the find all dialog and in tool menus if configured in the plugin.xml. The actual action is handled by the [`AbstractProjectSdkSetup`](../modules/core/src/main/kotlin/com/github/l34130/mise/core/setup/AbstractProjectSdkSetup.kt) class's `actionPerformed` which calls `configureSdk` by default and is DumbAware. If you override `actionPerformed` ensure that you either call `super` or call the `configureSdk` method yourself.  
