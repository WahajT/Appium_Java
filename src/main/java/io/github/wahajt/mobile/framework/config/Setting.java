package io.github.wahajt.mobile.framework.config;

/**
 * Every configuration value the framework understands.
 *
 * <p>Each setting can be supplied as a JVM system property ({@link #property()}, e.g. {@code
 * -Dandroid.app=...}), an environment variable ({@link #envVar()}, e.g. {@code ANDROID_APP=...}),
 * or an entry in the local {@code .env} file. See {@link FrameworkConfig} for precedence and
 * docs/CONFIGURATION.md for the user-facing reference.
 */
public enum Setting {
  // ---- General -------------------------------------------------------------------------------
  PLATFORM("platform", "android"),
  APPIUM_SERVER_URL("appium.server.url", "http://127.0.0.1:4723"),
  APPIUM_SERVER_START("appium.server.start", "false"),
  APPIUM_SERVER_MAIN_JS("appium.server.main.js", null),
  APPIUM_SERVER_LOG("appium.server.log", "target/appium-server.log"),
  CAPABILITIES_FILE("capabilities.file", null),
  APP_NO_RESET("app.no.reset", "false"),
  APP_FULL_RESET("app.full.reset", "false"),

  // ---- Timeouts (seconds) --------------------------------------------------------------------
  TIMEOUT_NEW_COMMAND("timeout.new.command", "120"),
  TIMEOUT_EXPLICIT_WAIT("timeout.explicit.wait", "15"),
  TIMEOUT_SERVER_READ("timeout.server.read", "300"),

  // ---- Reporting & resilience ----------------------------------------------------------------
  SCREENSHOT_ON_FAILURE("screenshot.on.failure", "true"),
  SCREENSHOT_DIR("screenshot.dir", "target/screenshots"),
  VIDEO_RECORD("video.record", "false"),
  VIDEO_DIR("video.dir", "target/videos"),
  RETRY_COUNT("retry.count", "0"),

  // ---- Android (UiAutomator2) ----------------------------------------------------------------
  ANDROID_DEVICE_NAME("android.device.name", "Android Emulator"),
  ANDROID_UDID("android.udid", null),
  ANDROID_PLATFORM_VERSION("android.platform.version", null),
  ANDROID_AVD("android.avd", null),
  ANDROID_APP("android.app", null),
  ANDROID_APP_PACKAGE("android.app.package", null),
  ANDROID_APP_ACTIVITY("android.app.activity", null),
  ANDROID_APP_WAIT_ACTIVITY("android.app.wait.activity", null),
  ANDROID_AUTO_GRANT_PERMISSIONS("android.auto.grant.permissions", "true"),
  ANDROID_SYSTEM_PORT("android.system.port", null),
  ANDROID_ADB_EXEC_TIMEOUT("android.adb.exec.timeout", null),
  ANDROID_INSTALL_TIMEOUT("android.install.timeout", null),
  ANDROID_ANR_WATCHER("android.anr.watcher", "true"),

  // ---- iOS (XCUITest) ------------------------------------------------------------------------
  IOS_DEVICE_NAME("ios.device.name", null),
  IOS_UDID("ios.udid", null),
  IOS_PLATFORM_VERSION("ios.platform.version", null),
  IOS_APP("ios.app", null),
  IOS_BUNDLE_ID("ios.bundle.id", null),
  IOS_AUTO_ACCEPT_ALERTS("ios.auto.accept.alerts", "false"),
  IOS_WDA_LAUNCH_TIMEOUT("ios.wda.launch.timeout", null),
  IOS_WDA_LOCAL_PORT("ios.wda.local.port", null),
  IOS_USE_PREBUILT_WDA("ios.use.prebuilt.wda", "false"),
  IOS_XCODE_ORG_ID("ios.xcode.org.id", null),
  IOS_XCODE_SIGNING_ID("ios.xcode.signing.id", "Apple Development"),
  IOS_UPDATED_WDA_BUNDLE_ID("ios.updated.wda.bundle.id", null);

  private final String property;
  private final String defaultValue;

  Setting(String property, String defaultValue) {
    this.property = property;
    this.defaultValue = defaultValue;
  }

  /** JVM system property name, e.g. {@code android.app}. */
  public String property() {
    return property;
  }

  /** Environment variable / {@code .env} key, e.g. {@code ANDROID_APP}. */
  public String envVar() {
    return name();
  }

  /** Built-in default, or {@code null} when the setting is unset by default. */
  public String defaultValue() {
    return defaultValue;
  }
}
