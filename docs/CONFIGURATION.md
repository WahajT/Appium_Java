# Configuration reference

Every setting can be supplied in four ways. The first non-empty value wins:

| Priority | Source | Example |
|---|---|---|
| 1 | JVM system property | `./mvnw test -Dandroid.app=/abs/app.apk` or `make test-android MVN_ARGS="-Dandroid.app=..."` |
| 2 | Environment variable | `export ANDROID_APP=/abs/app.apk` |
| 3 | Local `.env` file in the project root (git-ignored) | `ANDROID_APP=/abs/app.apk` |
| 4 | Built-in default | see tables below |

* The environment-variable name is always the property name upper-cased with `.` replaced by `_`
  (`android.app` ⇄ `ANDROID_APP`). A unit test enforces this.
* Empty values are ignored, so an empty line in `.env` falls back to the default.
* Use another file instead of `.env` with `-Dconfig.file=path` or `CONFIG_FILE=path`. If you name a file
  explicitly and it is missing, the run fails.
* Configuration is read once per JVM. The effective values are logged at the start of a Cucumber run.
  User-info in URLs (`https://user:key@host`) is masked as `****`.
* Durations are whole **seconds**.
* Relative app paths are resolved against the project root and sent to Appium as absolute paths. A
  missing local file fails before a session is requested. `http(s)://` URLs are passed through unchanged.

Start from the template:

```bash
cp .env.example .env     # or: make setup
```

## General

| Env variable | System property | Default | Purpose | Example |
|---|---|---|---|---|
| `PLATFORM` | `platform` | `android` | Target platform: `android` or `ios`. `make test-android` / `make test-ios` set it for you. | `ios` |
| `APPIUM_SERVER_URL` | `appium.server.url` | `http://127.0.0.1:4723` | Appium server endpoint, including any base path (Appium 1.x used `/wd/hub`). It can point to a remote grid or cloud provider. | `http://127.0.0.1:4723` |
| `APPIUM_SERVER_START` | `appium.server.start` | `false` | When `true`, the framework starts a local Appium server on the URL's host, port and base path before the run and stops it afterwards. Requires `appium` to be installed. | `true` |
| `APPIUM_SERVER_MAIN_JS` | `appium.server.main.js` | unset | Path to Appium's `main.js`, used only if auto-start cannot locate Appium (for example with some nvm setups). | `<npm root -g>/appium/build/lib/main.js` |
| `APPIUM_SERVER_LOG` | `appium.server.log` | `target/appium-server.log` | Log file for an auto-started server. | `target/appium.log` |
| `CAPABILITIES_FILE` | `capabilities.file` | unset | JSON object of extra capabilities, merged last so it overrides everything else. Names without a vendor prefix get `appium:`. See [`config/capabilities.example.json`](../config/capabilities.example.json). | `config/capabilities.json` |
| `APP_NO_RESET` | `app.no.reset` | `false` | Keep app data between sessions (`appium:noReset`). | `true` |
| `APP_FULL_RESET` | `app.full.reset` | `false` | Uninstall and reinstall the app around each session (`appium:fullReset`). | `true` |

## Timeouts (seconds)

| Env variable | System property | Default | Purpose | Example |
|---|---|---|---|---|
| `TIMEOUT_NEW_COMMAND` | `timeout.new.command` | `120` | Idle time after which Appium ends the session (`appium:newCommandTimeout`). | `300` |
| `TIMEOUT_EXPLICIT_WAIT` | `timeout.explicit.wait` | `15` | Default explicit wait used by `ElementActions` / page objects. The framework never sets an implicit wait. | `30` |
| `TIMEOUT_SERVER_READ` | `timeout.server.read` | `300` | HTTP read timeout of the Java client. Raise it when app installation or session start is slow. | `600` |

## Reporting and resilience

| Env variable | System property | Default | Purpose | Example |
|---|---|---|---|---|
| `SCREENSHOT_ON_FAILURE` | `screenshot.on.failure` | `true` | Capture a screenshot when a scenario fails. It is saved to disk and attached to the Cucumber and Allure reports. | `false` |
| `SCREENSHOT_DIR` | `screenshot.dir` | `target/screenshots` | Where screenshots are written. | `build/shots` |
| `VIDEO_RECORD` | `video.record` | `false` | Record the screen of every scenario. To record a single scenario, tag it `@record`. | `true` |
| `VIDEO_DIR` | `video.dir` | `target/videos` | Where recordings (`.mp4`) are written. | `build/videos` |
| `RETRY_COUNT` | `retry.count` | `0` | How many times to re-run a failed scenario. Retries hide flakiness, so keep this low. | `1` |

## Android (UiAutomator2)

| Env variable | System property | Default | Purpose | Example |
|---|---|---|---|---|
| `ANDROID_DEVICE_NAME` | `android.device.name` | `Android Emulator` | Informational device name (required by Appium, not used to pick a device). | `Pixel_7` |
| `ANDROID_UDID` | `android.udid` | unset | Serial from `adb devices`. Leave unset to use the only connected device. | `emulator-5554` |
| `ANDROID_PLATFORM_VERSION` | `android.platform.version` | unset | Android version the device must run. | `14` |
| `ANDROID_AVD` | `android.avd` | unset | AVD name for Appium to boot if it is not already running (`emulator -list-avds`). | `<your_avd_name>` |
| `ANDROID_APP` | `android.app` | unset | Path or URL of the `.apk` to install. | `apps/<your-app>.apk` |
| `ANDROID_APP_PACKAGE` | `android.app.package` | unset | Package of the app under test. Set it to launch an already-installed app, or to make app-state checks explicit. | `com.example.myapp` |
| `ANDROID_APP_ACTIVITY` | `android.app.activity` | unset | Activity to launch. Leave unset to use the app's launcher activity. | `.MainActivity` |
| `ANDROID_APP_WAIT_ACTIVITY` | `android.app.wait.activity` | unset | Activity (or wildcard) Appium waits for after launch, useful with splash screens. | `*` |
| `ANDROID_AUTO_GRANT_PERMISSIONS` | `android.auto.grant.permissions` | `true` | Grant all runtime permissions on install. | `false` |
| `ANDROID_SYSTEM_PORT` | `android.system.port` | unset | UiAutomator2 server port. It must be unique per device when running in parallel. | `8201` |
| `ANDROID_ADB_EXEC_TIMEOUT` | `android.adb.exec.timeout` | unset (Appium default) | Timeout for individual adb commands. | `60` |
| `ANDROID_INSTALL_TIMEOUT` | `android.install.timeout` | unset (Appium default) | Timeout for installing the `.apk`. | `300` |
| `ANDROID_ANR_WATCHER` | `android.anr.watcher` | `true` | At session start, press **Wait** on any "App isn't responding" system dialog, which is common on slow emulators. Has no effect on iOS. | `false` |

You must set **`ANDROID_APP`** (install from file or URL), **`ANDROID_APP_PACKAGE`** (app already
installed), or both. Otherwise the framework stops with a clear error.

## iOS (XCUITest, requires macOS + Xcode for the Appium server)

| Env variable | System property | Default | Purpose | Example |
|---|---|---|---|---|
| `IOS_DEVICE_NAME` | `ios.device.name` | unset | Simulator name (`xcrun simctl list devices`). | `iPhone 15` |
| `IOS_UDID` | `ios.udid` | unset | UDID of a simulator or real device. Required for real devices. | `<device-udid>` |
| `IOS_PLATFORM_VERSION` | `ios.platform.version` | unset | iOS version. Recommended for simulators, so Appium selects the right runtime. | `17.5` |
| `IOS_APP` | `ios.app` | unset | Simulators: `.app` directory or zipped `.app`. Real devices: signed `.ipa`. | `apps/<YourApp>.app` |
| `IOS_BUNDLE_ID` | `ios.bundle.id` | unset | Bundle identifier of the app under test (needed for an already-installed app). | `com.example.myapp` |
| `IOS_AUTO_ACCEPT_ALERTS` | `ios.auto.accept.alerts` | `false` | Automatically accept system permission alerts. | `true` |
| `IOS_WDA_LAUNCH_TIMEOUT` | `ios.wda.launch.timeout` | unset (Appium default) | Time allowed for WebDriverAgent to start. The first build on a real device can take minutes. | `180` |
| `IOS_WDA_LOCAL_PORT` | `ios.wda.local.port` | unset | WebDriverAgent port. It must be unique per device when running in parallel. | `8101` |
| `IOS_USE_PREBUILT_WDA` | `ios.use.prebuilt.wda` | `false` | Reuse an already-built WebDriverAgent to speed up session start. | `true` |
| `IOS_XCODE_ORG_ID` | `ios.xcode.org.id` | unset | **Real devices only.** Apple Developer Team ID used to sign WebDriverAgent. Setting it enables signing. | `<TEAM_ID>` |
| `IOS_XCODE_SIGNING_ID` | `ios.xcode.signing.id` | `Apple Development` | **Real devices only.** Signing identity, used only together with `IOS_XCODE_ORG_ID`. | `Apple Development` |
| `IOS_UPDATED_WDA_BUNDLE_ID` | `ios.updated.wda.bundle.id` | unset | **Real devices only.** A unique WebDriverAgent bundle id that your provisioning profile allows. | `com.<yourteam>.WebDriverAgentRunner` |

You must set **`IOS_UDID`** or **`IOS_DEVICE_NAME`**, and also **`IOS_APP`**, **`IOS_BUNDLE_ID`**, or both.

### Simulator vs. real device

| | Simulator | Real device |
|---|---|---|
| App artifact | `.app` built for the simulator (`-sdk iphonesimulator`) | `.ipa` signed for the device (development or ad-hoc profile) |
| Device selection | `IOS_DEVICE_NAME` + `IOS_PLATFORM_VERSION` (or simulator `IOS_UDID`) | `IOS_UDID` (required) |
| Signing | none | `IOS_XCODE_ORG_ID`, `IOS_XCODE_SIGNING_ID`, often `IOS_UPDATED_WDA_BUNDLE_ID` |
| Other | — | Developer Mode enabled on the device, device trusted, UI Automation enabled in Settings → Developer |

## Extra capabilities

To use any capability without a dedicated setting (locale, orientation, cloud-provider options),
put it in a JSON file and point `CAPABILITIES_FILE` at it:

```json
{
  "appium:language": "en",
  "appium:locale": "US",
  "appium:orientation": "PORTRAIT"
}
```

The file is merged **last**, so it can override any value above. If it contains cloud credentials,
keep it out of version control (`config/*.local.json` is git-ignored) or inject it in CI from a secret.

## Cucumber and Maven options

These are not framework settings. They are passed straight to Cucumber or Maven:

| Option | Purpose | Example |
|---|---|---|
| `-Dcucumber.filter.tags=...` | Tag expression that selects scenarios. The Makefile sets it from `TAGS`. | `"@smoke and not @ios"` |
| `-Dtestng.suite=...` | TestNG suite file. `testng-unit.xml` (default) or `testng-e2e.xml`. | `src/test/resources/testng-e2e.xml` |
| `-Dcucumber.features=...` | Run specific feature files or directories. | `src/test/resources/features/smoke` |
