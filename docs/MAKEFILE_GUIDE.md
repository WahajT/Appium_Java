# Makefile guide

The `Makefile` gives you short, memorable commands for everyday tasks. It does **not** replace
Maven:

- **Maven** (through the bundled wrapper `./mvnw`) resolves Java dependencies, compiles, runs
  TestNG/Cucumber and generates reports. Everything works with Maven alone.
- **Make** wraps those Maven commands. It adds the right flags (suite file, platform, tag filter),
  runs pre-flight checks that explain missing prerequisites, and keeps the commands the same
  across machines and CI.

Every target echoes the Maven command it runs, so you can always see or copy the underlying call.

Requirements: GNU Make 3.81+ and bash (the macOS defaults are fine; Linux; WSL or Git Bash on
Windows). `make help` works without Java, Appium, the Android SDK or Xcode.

## Variables

Pass variables on the command line (`make test-android TAGS="@smoke"`) or export them in your
shell.

| Variable | Default | Used by | Purpose |
|---|---|---|---|
| `TAGS` | empty | `test-android`, `test-ios`, `check-features` | Cucumber tag expression. When set, it **replaces** the platform default filter. |
| `ANDROID_TAGS` | `not @template and not @ios` | `test-android` | Default filter for Android runs. |
| `IOS_TAGS` | `not @template and not @android` | `test-ios` | Default filter for iOS runs. |
| `MVN_ARGS` | empty | most targets | Extra Maven arguments, e.g. framework overrides like `-Dandroid.udid=emulator-5556`. |
| `MVN` | `./mvnw` | all Maven targets | Maven command. Use `MVN=mvn` to use a locally installed Maven. |
| `MVN_FLAGS` | `-B` | all Maven targets | Maven flags (batch mode by default; add `-q` for quieter output). |
| `SKIP_PREFLIGHT` | `0` | `test-android`, `test-ios` | `1` skips the pre-flight checks, e.g. for cloud providers. |
| `E2E_SUITE` | `src/test/resources/testng-e2e.xml` | `test-android`, `test-ios`, `check-features` | TestNG suite for device runs. |
| `EMULATOR_TIMEOUT` | `300` | `wait-emulator` | Seconds to wait for an emulator to boot. |

App, device and server settings (`ANDROID_APP`, `IOS_UDID`, `APPIUM_SERVER_URL`, …) are **not**
Make variables. They are read by the framework from the environment or `.env`; see
[CONFIGURATION.md](CONFIGURATION.md). Exporting them works because Maven and the tests inherit your
environment.

## Targets

### `make help`

- **What it does:** Lists every target with a one-line description, plus the variables above.
- **Prerequisites:** none (no Java needed).
- **Command:** `make help` (also the default when you run plain `make`).
- **Runs:** locally, on any OS with make.
- **Output:** text only.

### `make doctor`

- **What it does:** Reports the status of everything the project can use. It checks the JDK (and
  that `javac` exists, because a JRE is not enough), the Maven wrapper, Node.js, Appium, the
  `uiautomator2`/`xcuitest` drivers, whether the Appium server at `APPIUM_SERVER_URL` responds, the
  Android SDK, `adb` and connected devices, emulator AVDs, Xcode and simulators (macOS only), and
  `.env`.
- **Prerequisites:** none.
- **Command:** `make doctor` (runs `scripts/doctor.sh`).
- **Runs:** locally; iOS checks only on macOS.
- **Output:** ✔ / ! / ✘ lines. It exits non-zero **only** when the build toolchain (JDK 17+ with
  `javac`, `./mvnw`) is missing. Android/iOS gaps are warnings, because you may only need one
  platform or a remote server.
- **Common errors:**
  - `javac not found`: install a full JDK (e.g. Temurin 17/21) and set `JAVA_HOME`.
  - `No Appium server answering`: start `appium`, or ignore this if you use
    `APPIUM_SERVER_START=true`.

### `make setup`

- **What it does:** Copies `.env.example` to `.env` if `.env` does not exist (an existing `.env` is
  never overwritten). It makes scripts executable, verifies the build toolchain, then downloads all
  dependencies and compiles main and test code.
- **Prerequisites:** JDK 17+, internet access for the first dependency download.
- **Command:** `make setup` → `./mvnw -B -DskipTests test-compile`.
- **Runs:** locally; no device needed.
- **Output:** `.env`, populated `~/.m2` cache, `target/classes`, `target/test-classes`.
- **Common errors:** `Missing build prerequisites`: run `make doctor`. Proxy/firewall failures
  while downloading: configure Maven's `~/.m2/settings.xml` proxy.

### `make build`

- **What it does:** Compiles and packages the framework jar without running tests.
- **Prerequisites:** JDK 17+.
- **Command:** `make build` → `./mvnw -B -DskipTests package`.
- **Runs:** locally; no device.
- **Output:** `target/appium-java-mobile-framework-<version>.jar`.
- **Common errors:** `release version 17 not supported` / `No compiler is provided`: you are on a
  JRE or an old JDK; fix `JAVA_HOME`.

### `make test`

- **What it does:** Runs the framework's own unit tests (`testng-unit.xml`). These cover
  configuration precedence, per-platform capability building, the docs/config consistency check,
  and utilities.
- **Prerequisites:** JDK 17+. **No Appium server or device.**
- **Command:** `make test` → `./mvnw -B test`.
- **Runs:** locally and in CI.
- **Output:** `Tests run: N, Failures: 0 …`; details in `target/surefire-reports/`.
- **Common errors:** `ConfigurationDocsTest` fails after you add a `Setting` without documenting
  it. Add it to `docs/CONFIGURATION.md` and `.env.example`.

### `make test-android`

- **What it does:** Runs `scripts/preflight.sh android`, then the Cucumber scenarios on Android.
  The preflight checks that the Appium server is reachable (or `appium` is installed when
  `APPIUM_SERVER_START=true`), that `ANDROID_APP` or `ANDROID_APP_PACKAGE` is set and the file
  exists, and, for a local server, that a device is online or `ANDROID_AVD` is set.
- **Prerequisites:** Appium + `uiautomator2` driver, Android SDK, an emulator or device, and an app
  configured in `.env`.
- **Command:**
  ```bash
  make test-android
  # runs: ./mvnw -B test -Dtestng.suite=src/test/resources/testng-e2e.xml -Dplatform=android \
  #         "-Dcucumber.filter.tags=not @template and not @ios"
  make test-android TAGS="@smoke"
  make test-android MVN_ARGS="-Dandroid.udid=emulator-5556 -Dretry.count=1"
  ```
- **Runs:** locally (Linux, macOS, Windows/WSL). Requires a connected device/emulator, or a
  remote Appium server.
- **Output:** Cucumber console output; `target/cucumber-reports/`, `target/allure-results/`,
  `target/screenshots/` (failures), `target/videos/` (if recording).
- **Common errors:** `No Appium server at …`, `No Android device online`,
  `ANDROID_APP does not exist`: each message says what to fix. `SKIP_PREFLIGHT=1` bypasses the
  checks (the framework still validates its configuration).

### `make test-ios`

- **What it does:** Runs `scripts/preflight.sh ios`, then the Cucumber scenarios on iOS. The
  preflight checks the Appium server, that `IOS_APP` or `IOS_BUNDLE_ID` is set, and, for a local
  server, that you are on macOS with Xcode and have set `IOS_DEVICE_NAME` or `IOS_UDID`.
- **Prerequisites:** macOS + Xcode, Appium + `xcuitest` driver, a simulator or a prepared real
  device, and an app configured in `.env`.
- **Command:**
  ```bash
  make test-ios
  # runs: ./mvnw -B test -Dtestng.suite=src/test/resources/testng-e2e.xml -Dplatform=ios \
  #         "-Dcucumber.filter.tags=not @template and not @android"
  make test-ios TAGS="@smoke" MVN_ARGS="-Dios.device.name='iPhone 15'"
  ```
- **Runs:** macOS for a local server. On Linux/Windows it only works against a **remote** Appium
  server on a Mac or cloud (`APPIUM_SERVER_URL=…`, often with `SKIP_PREFLIGHT=1`).
- **Output:** same as `test-android`.
- **Common errors:** `iOS tests need macOS + Xcode` (local server on a non-Mac). For WebDriverAgent
  signing errors on real devices, see the README troubleshooting table.

### `make check-features`

- **What it does:** Runs every feature file, including `@template` ones, as a Cucumber **dry-run**.
  This verifies that each step has exactly one matching step definition, without opening a
  session.
- **Prerequisites:** JDK 17+. No device.
- **Command:** `make check-features` → `./mvnw -B test -Dtestng.suite=… -Dcucumber.execution.dry-run=true "-Dcucumber.filter.tags=@template or not @template"`.
- **Runs:** locally and in CI.
- **Output:** each step marked ✔. An undefined step makes the build fail and prints a snippet to
  implement.

### `make lint`

- **What it does:** Checks Java formatting (google-java-format via Spotless), recompiles with
  `-Dmaven.compiler.failOnWarning=true` so any compiler warning fails the build, and runs
  `shellcheck` on `scripts/*.sh` if it is installed.
- **Prerequisites:** JDK 17+; `shellcheck` is optional (it is skipped with a message when absent).
- **Command:** `make lint` → `./mvnw -B spotless:check` then `./mvnw -B -Dmaven.compiler.failOnWarning=true clean test-compile`.
- **Runs:** locally and in CI.
- **Note:** this target runs `clean`, so it deletes `target/` (and with it, previous reports).
- **Common errors:** `The following files had format violations`: run `make format`.

### `make format`

- **What it does:** Rewrites Java sources to google-java-format style.
- **Command:** `make format` → `./mvnw -B spotless:apply`.
- **Output:** modified `.java` files. Review them with `git diff`.

### `make clean`

- **What it does:** Deletes `target/`: compiled classes, the jar, test and Allure results,
  generated reports, screenshots, videos, and the auto-started Appium log. It never touches
  `.env`, sources or anything outside `target/`.
- **Command:** `make clean` → `./mvnw -B -q clean`.
- **Output:** `Removed target/`.

### `make report`

- **What it does:** Builds the Allure HTML report from `target/allure-results/` of the last device
  run. The Allure command-line tool is downloaded by the `allure-maven` plugin, so no global
  install is needed.
- **Prerequisites:** a previous `make test-android` / `make test-ios` run. Unit tests do not
  produce Allure results.
- **Command:** `make report` → `./mvnw -B allure:report`.
- **Output:** `target/site/allure-maven-plugin/index.html`. The Cucumber report is already at
  `target/cucumber-reports/cucumber.html`.
- **Common errors:** `No Allure results in target/allure-results`: run device tests first (and
  note that `make clean` / `make lint` delete them).
- **Viewing the report:** open it with `make report-serve` (below) or another local web server.
  Browsers often block the report's scripts when it is opened from `file://`.

### `make report-serve`

- **What it does:** Generates the Allure report into a temporary directory, starts a local web
  server, and opens your browser. Press Ctrl+C to stop.
- **Command:** `make report-serve` → `./mvnw -B allure:serve`.
- **Runs:** locally with a desktop browser (not useful in headless CI).

### `make wait-emulator`

- **What it does:** Blocks until an Android emulator or device is attached and reports
  `sys.boot_completed=1`, or fails after `EMULATOR_TIMEOUT` seconds. This is useful in scripts and
  CI after `emulator -avd <name> &`.
- **Prerequisites:** `adb` on `PATH`. Set `ANDROID_SERIAL` when several devices are attached.
- **Command:** `make wait-emulator EMULATOR_TIMEOUT=600`.
- **Output:** progress lines, then `Device booted after ~Ns`. It exits non-zero on timeout.

## Typical workflows

```bash
# First time on a new machine
make doctor
make setup
make test

# Daily Android work (Appium running in another terminal)
make test-android TAGS="@smoke"
make report-serve

# Before opening a pull request
make lint test check-features

# Headless CI with an emulator you start yourself
emulator -avd <avd_name> -no-window -no-audio &
make wait-emulator
APPIUM_SERVER_START=true make test-android
make report
```

## Platform notes

| Target | Linux | macOS | Windows |
|---|---|---|---|
| `help`, `doctor`, `setup`, `build`, `test`, `check-features`, `lint`, `format`, `clean`, `report` | ✔ | ✔ | WSL / Git Bash (or the equivalent `mvnw.cmd` command) |
| `test-android`, `wait-emulator` | ✔ | ✔ | WSL / Git Bash |
| `test-ios` | remote Mac/cloud server only | ✔ | remote Mac/cloud server only |
