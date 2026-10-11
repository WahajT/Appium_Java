# Appium Java Mobile Framework

A reusable starting point for **Android and iOS UI test automation** in Java. It is built on
[Appium](https://appium.io) 2+/3, Selenium 4, TestNG, Cucumber and Allure.

You configure your app and device in a `.env` file without touching framework code. You write page
objects and Cucumber steps for your own screens, and run everything through `make` or Maven.

> **Status:** early release (0.1.0). The framework is unit-tested and its Appium wiring has been
> exercised end to end, but it has **not yet been validated against a real emulator, simulator or
> device in this repository's CI**. See [Known limitations](#known-limitations).

---

## Contents

- [Features](#features)
- [How it works](#how-it-works)
- [Prerequisites](#prerequisites)
- [Quick start](#quick-start)
- [Setting up Appium, Android and iOS](#setting-up-appium-android-and-ios)
- [Configuring your application](#configuring-your-application)
- [Running tests](#running-tests)
- [Reports, screenshots and videos](#reports-screenshots-and-videos)
- [Writing tests](#writing-tests)
- [Extending the framework](#extending-the-framework)
- [Troubleshooting](#troubleshooting)
- [Known limitations](#known-limitations)
- [Contributing and license](#contributing-and-license)

## Features

- **One codebase, two platforms.** It uses `UiAutomator2Options` for Android and `XCUITestOptions`
  for iOS, and only applies capabilities that belong to the selected platform.
- **Configuration without code changes.** Settings are layered: `-D` system property, then
  environment variable, then `.env` file, then defaults. The app path, package/bundle id, device,
  server URL, timeouts and signing are all configurable. Any other capability can come from a JSON
  file.
- **Fails fast with clear errors.** A missing app file, a missing device setting or an unreachable
  server is reported before a session is requested.
- **Thread-safe driver lifecycle.** There is one Appium session per scenario, held in a
  `ThreadLocal` and always cleaned up.
- **Waits instead of sleeps.** `ElementActions` wraps explicit waits. `Gestures` provides W3C
  swipe, tap, long-press and scroll-to-text on both platforms.
- **Page-object base class** with per-platform `Locator`s.
- **Evidence on failure.** Failed scenarios get a screenshot automatically. Screen recording can be
  turned on for every scenario or per scenario with the `@record` tag. Both are attached to the
  Cucumber and Allure reports.
- **Android resilience.** An optional watcher dismisses "App isn't responding" system dialogs.
  TestNG retries are available and off by default.
- **Optional local Appium server.** Set `APPIUM_SERVER_START=true` and the framework starts and
  stops Appium for you.
- **Makefile front-end** with `doctor` and pre-run checks that explain what is missing.
- **CI that needs no device:** formatting, compiler warnings, unit tests, Cucumber step-binding
  dry-run, and shellcheck.

## How it works

```
src/main/java/io/github/wahajt/mobile/framework/      ← reusable framework (no app knowledge)
├── config/     FrameworkConfig, Setting, Platform    – layered configuration
├── driver/     CapabilitiesFactory, DriverFactory,   – options per platform, session creation,
│               DriverManager, AppiumServerManager      ThreadLocal lifecycle, optional local server
├── pages/      BasePage, Locator                     – page-object foundation
├── ui/         ElementActions, Gestures,             – waits, gestures, app/keyboard control,
│               DeviceActions, AndroidSystemDialogs     Android ANR dialog handling
├── reporting/  Artifacts                             – screenshots & recordings to disk
├── testng/     RetryAnalyzer, RetryTransformer       – opt-in retries
├── cucumber/   Hooks                                 – per-scenario lifecycle
└── data/       TestData                              – random test data

src/test/java/io/github/wahajt/mobile/
├── examples/pages/   LoginPage, HomePage (templates)  ← replace with your screens
├── examples/steps/   AppSteps (app-agnostic), LoginSteps (template)
├── runners/          CucumberTestRunner
└── framework/...     unit tests for the framework (no device)

src/test/resources/
├── features/smoke/       app_launch.feature – runs against ANY configured app
├── features/templates/   login.feature      – @template, excluded until you adapt it
├── testng-unit.xml       default suite (unit tests)
└── testng-e2e.xml        device suite (Cucumber)
```

A scenario runs like this:

1. **`@BeforeAll`**: log the effective configuration (credentials in URLs are masked) and,
   optionally, start a local Appium server.
2. **`@Before`**: `DriverManager.start()` builds options from config, opens a session (which
   installs and launches the app), dismisses any ANR dialog, and optionally starts recording.
3. **Steps** use page objects and get the driver from `DriverManager.driver()`.
4. **`@After`**: on failure, take a screenshot. If recording, save the video. **Always** quit the
   session.
5. **`@AfterAll`**: stop the local Appium server if one was started.

## Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| JDK (full JDK with `javac`, not just a JRE) | 17 or newer (tested with 17 and 21) | everything |
| Maven | none: `./mvnw` downloads Maven 3.9 | everything |
| GNU Make | 3.81+ (macOS default is fine) | the `make` targets (optional; Maven works alone) |
| Node.js + npm | current LTS | installing/running Appium |
| Appium | 2.x or 3.x (`npm i -g appium`) | running tests |
| Appium UiAutomator2 driver | latest (`appium driver install uiautomator2`) | Android |
| Android SDK (platform-tools, emulator) | recent | Android |
| macOS + Xcode + Command Line Tools | an Xcode version supported by your XCUITest driver | iOS |
| Appium XCUITest driver | latest (`appium driver install xcuitest`) | iOS |

Run `make doctor` to see what is installed and what is missing.

On Windows, use `mvnw.cmd` in place of `./mvnw` for the Maven commands below. The `make` targets
need a POSIX shell (macOS, Linux, WSL or Git Bash).

## Quick start

```bash
git clone <your-fork-url> appium-java-mobile-framework
cd appium-java-mobile-framework

make doctor              # what is installed / missing
make setup               # creates .env from .env.example and downloads dependencies
make test                # framework unit tests – no device required

# Edit .env: set PLATFORM, and ANDROID_APP / ANDROID_APP_PACKAGE (or IOS_* for iOS)
appium                   # in a second terminal (or set APPIUM_SERVER_START=true)
make test-android        # or: make test-ios
make report              # Allure HTML report
```

The smoke feature (`features/smoke/app_launch.feature`) has **no app-specific locators**. It checks
that your app launches, renders and survives being backgrounded, so it is a quick way to prove
your setup works.

## Setting up Appium, Android and iOS

### Appium

```bash
npm install -g appium
appium driver install uiautomator2     # Android
appium driver install xcuitest         # iOS (macOS only)
appium driver doctor uiautomator2      # optional: checks Android prerequisites
appium driver doctor xcuitest          # optional: checks iOS prerequisites
appium                                 # starts the server on http://127.0.0.1:4723
```

Appium 2+ serves from `/` by default. If your server uses the legacy `/wd/hub` base path, include
it in `APPIUM_SERVER_URL`.

### Android SDK and emulator

1. Install [Android Studio](https://developer.android.com/studio) or the command-line tools.
2. Set environment variables (add them to your shell profile):
   ```bash
   export ANDROID_HOME="$HOME/Library/Android/sdk"   # macOS default; Linux: $HOME/Android/Sdk
   export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
   ```
3. Create an emulator in Android Studio's Device Manager, or with
   `sdkmanager "system-images;android-34;google_apis;x86_64"` plus
   `avdmanager create avd -n <name> -k "system-images;android-34;google_apis;x86_64"`.
4. Start it with `emulator -avd <name>`, then run `make wait-emulator`. Alternatively, set
   `ANDROID_AVD=<name>` and Appium boots it for you.
5. For a **real device**, enable *Developer options → USB debugging*, connect it, accept the RSA
   prompt, and copy its serial from `adb devices` into `ANDROID_UDID`.

### Xcode and iOS (macOS only)

1. Install Xcode from the App Store, open it once to finish setup, then run
   `xcode-select --install`.
2. **Simulator**: list devices with `xcrun simctl list devices available`, then set
   `IOS_DEVICE_NAME` (and preferably `IOS_PLATFORM_VERSION`). You need a `.app` built for the
   simulator SDK.
3. **Real device** (needs more setup):
   - Enable *Developer Mode* on the device and trust the computer.
   - Set `IOS_UDID` (from `xcrun xctrace list devices`) and provide a signed `.ipa`, or an app that
     is already installed, via `IOS_BUNDLE_ID`.
   - WebDriverAgent must be signed: set `IOS_XCODE_ORG_ID` (your Team ID) and usually
     `IOS_UPDATED_WDA_BUNDLE_ID` to a bundle id your provisioning profile allows. See the
     [XCUITest driver real-device guide](https://appium.github.io/appium-xcuitest-driver/latest/preparation/real-device-config/).
   - The first session builds WebDriverAgent and can take several minutes, so raise
     `IOS_WDA_LAUNCH_TIMEOUT` if needed.

## Configuring your application

Copy `.env.example` to `.env` (or run `make setup`) and fill in what applies. Minimal examples:

```bash
# Android – install from an .apk
PLATFORM=android
ANDROID_APP=apps/<your-app>.apk

# Android – app already installed on the device
PLATFORM=android
ANDROID_APP_PACKAGE=com.example.myapp
ANDROID_APP_ACTIVITY=.MainActivity

# iOS simulator
PLATFORM=ios
IOS_DEVICE_NAME=iPhone 15
IOS_PLATFORM_VERSION=17.5
IOS_APP=apps/<YourApp>.app

# iOS real device
PLATFORM=ios
IOS_UDID=<device-udid>
IOS_BUNDLE_ID=com.example.myapp
IOS_XCODE_ORG_ID=<TEAM_ID>
IOS_UPDATED_WDA_BUNDLE_ID=com.<yourteam>.WebDriverAgentRunner
```

`apps/`, `*.apk`, `*.ipa`, `*.app/` and `.env` are git-ignored, so app binaries and local settings
are never committed.

**Every setting, its default, its environment variable and its system property is documented in
[docs/CONFIGURATION.md](docs/CONFIGURATION.md).** That includes timeouts, retries, screenshots,
video, parallel ports, signing, and extra capabilities via `CAPABILITIES_FILE`.

## Running tests

### With Make

```bash
make test                                   # unit tests (no device)
make test-android                           # all Android scenarios except @template / @ios
make test-ios                               # all iOS scenarios except @template / @android
make test-android TAGS="@smoke"             # choose scenarios by tag
make test-android MVN_ARGS="-Dandroid.udid=emulator-5556 -Dretry.count=1"
make check-features                         # verify every step has a definition (no device)
```

`make help` lists all targets. [docs/MAKEFILE_GUIDE.md](docs/MAKEFILE_GUIDE.md) explains each one
in detail.

### With Maven directly

Make is only a convenience wrapper. Maven runs the build and the tests:

```bash
./mvnw test                                                    # unit tests
./mvnw test -Dtestng.suite=src/test/resources/testng-e2e.xml \
            -Dplatform=android \
            -Dcucumber.filter.tags="not @template and not @ios"  # Android scenarios
./mvnw test -Dtestng.suite=src/test/resources/testng-e2e.xml \
            -Dplatform=ios -Dios.device.name="iPhone 15" \
            -Dcucumber.filter.tags="@smoke"                      # iOS smoke
./mvnw -DskipTests package                                     # build the jar
```

Any setting can be passed as `-D<property>=<value>` (see
[docs/CONFIGURATION.md](docs/CONFIGURATION.md)).

### Platform-specific scenarios

Tag a scenario `@android` or `@ios` if it only applies to one platform. The other platform's
default tag filter excludes it.

## Reports, screenshots and videos

| Output | Location | How to open |
|---|---|---|
| Cucumber HTML / JSON | `target/cucumber-reports/cucumber.html`, `cucumber.json` | open in a browser |
| Allure results | `target/allure-results/` | `make report` → `target/site/allure-maven-plugin/index.html`, or `make report-serve` |
| Failure screenshots | `target/screenshots/` | also attached to both reports |
| Screen recordings | `target/videos/` (`VIDEO_RECORD=true` or tag `@record`) | also attached to both reports |
| TestNG / Surefire | `target/surefire-reports/` | |
| Auto-started Appium log | `target/appium-server.log` | |

`make report` downloads the Allure command-line tool through Maven, so you don't need to install
Allure globally. `make clean` deletes `target/`, including all of the above.

## Writing tests

### 1. A page object

```java
package io.github.wahajt.mobile.myapp.pages;

public class SettingsPage extends BasePage {
  // Same accessibility id on both platforms:
  private static final Locator TITLE = Locator.both(AppiumBy.accessibilityId("settings_title"));
  // Different locators per platform:
  private static final Locator DARK_MODE = Locator.of(
      AppiumBy.id("com.example.myapp:id/dark_mode_switch"),
      AppiumBy.iOSClassChain("**/XCUIElementTypeSwitch[`name == \"dark_mode\"`]"));

  public SettingsPage(AppiumDriver driver) { super(driver); }

  @Override public boolean isDisplayed() {
    return actions.isDisplayedWithin(by(TITLE), actions.timeout());
  }

  public SettingsPage enableDarkMode() {
    actions.click(by(DARK_MODE));      // waits until clickable – no Thread.sleep
    return this;
  }
}
```

`BasePage` gives every page `driver`, `config`, `actions` (`ElementActions`: waits, click, type,
text, `isDisplayedWithin`), `gestures` (swipe, tap, long-press, `scrollToText`) and `device`
(keyboard, background/activate/terminate app, back).

Locator tips: prefer **accessibility ids**. Ask your developers to add them, because they are fast
and stable on both platforms. Avoid long absolute XPaths.

### 2. Steps and a feature

```java
public class SettingsSteps {
  @When("I enable dark mode")
  public void enableDarkMode() {
    new SettingsPage(DriverManager.driver()).enableDarkMode();
  }
}
```

```gherkin
@settings
Feature: Settings
  Scenario: Dark mode can be enabled
    Given the application is launched
    When I enable dark mode
    ...
```

Put step classes in a package listed in `glue` of
[`CucumberTestRunner`](src/test/java/io/github/wahajt/mobile/runners/CucumberTestRunner.java). The
templates in `examples/` (`LoginPage`, `LoginSteps`, `features/templates/login.feature`) show a
complete flow. Credentials come from `TEST_USERNAME` / `TEST_PASSWORD` environment variables and
never from source.

### 3. Without Cucumber

The framework classes don't depend on Cucumber. In a plain TestNG class, call
`DriverManager.start(FrameworkConfig.get())` in `@BeforeMethod` and `DriverManager.quit()` in
`@AfterMethod(alwaysRun = true)`, and use page objects as above.

## Extending the framework

- **Your own project layout.** Keep `io.github.wahajt.mobile.framework` as is. Put your pages and
  steps in your own package, add it to `glue`, and delete or keep `examples/` as a reference.
- **New setting.** Add it to the `Setting` enum, use it in `CapabilitiesFactory` (or wherever it
  applies), and document it in `docs/CONFIGURATION.md` and `.env.example`. `ConfigurationDocsTest`
  fails until you do.
- **One-off capabilities.** No code change is needed: use `CAPABILITIES_FILE`.
- **Cloud device farms.** Point `APPIUM_SERVER_URL` at the provider's hub, put provider options in
  a `CAPABILITIES_FILE` that you keep out of git or generate in CI from secrets, and run with
  `SKIP_PREFLIGHT=1`.
- **Login and other preconditions.** Add a Cucumber `@Before` hook with a tag expression (for
  example `@Before("@loggedIn")`) that brings the app into the required state through page
  objects.
- **Parallel runs** need one device per thread. Give each its own `ANDROID_UDID` +
  `ANDROID_SYSTEM_PORT` (or `IOS_UDID` + `IOS_WDA_LOCAL_PORT`), and enable parallel scenarios by
  overriding `scenarios()` in your runner with `@DataProvider(parallel = true)`. `DriverManager`
  is already thread-safe, but the bundled configuration targets one device per JVM, so the
  simplest approach is one Maven invocation per device.

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `release version 17 not supported` or `No compiler is provided` | Only a JRE is installed. Install a full JDK and set `JAVA_HOME` (`make doctor` checks `javac`). |
| `No Appium server at http://127.0.0.1:4723` | Start `appium` in another terminal, set `APPIUM_SERVER_START=true`, or fix `APPIUM_SERVER_URL`. |
| `Could not find a driver for automationName 'UiAutomator2'` / `'XCUITest'` | `appium driver install uiautomator2` / `xcuitest`. |
| `Application file not found: …` | The path in `ANDROID_APP` / `IOS_APP` is wrong. Relative paths are resolved from the project root. |
| `Android needs ANDROID_APP … and/or ANDROID_APP_PACKAGE` | Set at least one of them in `.env`. |
| `No Android device online` | Start an emulator, connect a device (`adb devices`), or set `ANDROID_AVD`. |
| Session creation times out on the first run | App install or WebDriverAgent build is slow. Raise `TIMEOUT_SERVER_READ`, `ANDROID_INSTALL_TIMEOUT` or `IOS_WDA_LAUNCH_TIMEOUT`. |
| iOS real device: `xcodebuild failed with code 65` | WebDriverAgent signing problem. Check `IOS_XCODE_ORG_ID`, `IOS_UPDATED_WDA_BUNDLE_ID`, Developer Mode and device trust. |
| `iOS tests need macOS + Xcode` from `make test-ios` | iOS automation requires a Mac. Run there, or point `APPIUM_SERVER_URL` at a remote Mac/cloud and use `SKIP_PREFLIGHT=1`. |
| Elements not found intermittently | Wait for the element with `actions.waitVisible(...)` rather than adding sleeps. Check locators in Appium Inspector. |
| "App isn't responding" dialogs on emulators | Keep `ANDROID_ANR_WATCHER=true`, give the emulator more RAM/CPU, and use hardware acceleration. |
| Scenario passes alone but fails in a full run | State leaks between scenarios. Use `APP_NO_RESET=false` (the default), or reset state in a `@Before` hook. |

## Known limitations

- Runtime behaviour on **real emulators, simulators and devices has not been verified in this
  repository's CI**. The CI covers compilation, unit tests and Cucumber step bindings. The full
  Appium session lifecycle (capabilities sent, sessions created and always deleted, screenshots,
  retries) was checked against a mock W3C endpoint during development. Reports from real-device
  runs are welcome. Please open an issue.
- Parallel execution across multiple devices is possible but not configured out of the box.
- `DeviceActions.back()` on iOS depends on the app exposing a navigation back action.
- `make` targets require a POSIX shell. Windows users should use `mvnw.cmd` directly, or WSL.

## Contributing and license

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md), the
[Code of Conduct](CODE_OF_CONDUCT.md) and the [security policy](SECURITY.md). Changes are recorded
in [CHANGELOG.md](CHANGELOG.md).

Licensed under the [Apache License 2.0](LICENSE).

Appium is a project of the OpenJS Foundation. This project is independent and not affiliated with
or endorsed by the Appium project.
