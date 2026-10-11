package io.github.wahajt.mobile.framework.driver;

import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.ios.options.wda.XcodeCertificate;
import io.appium.java_client.remote.options.BaseOptions;
import io.github.wahajt.mobile.framework.config.ConfigurationException;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import io.github.wahajt.mobile.framework.config.Setting;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonException;

/**
 * Builds platform-specific Appium options from {@link FrameworkConfig}.
 *
 * <p>Only capabilities that apply to the selected platform are set, and optional settings are left
 * to Appium's defaults when unset. Arbitrary extra capabilities can be merged from the JSON file
 * named by {@link Setting#CAPABILITIES_FILE}; they are applied last and win over everything else.
 */
public final class CapabilitiesFactory {

  private CapabilitiesFactory() {}

  public static BaseOptions<?> forConfig(FrameworkConfig config) {
    switch (config.platform()) {
      case ANDROID:
        return android(config);
      case IOS:
        return ios(config);
      default:
        throw new IllegalStateException("Unsupported platform " + config.platform());
    }
  }

  public static UiAutomator2Options android(FrameworkConfig config) {
    UiAutomator2Options options = new UiAutomator2Options();
    options.setDeviceName(config.require(Setting.ANDROID_DEVICE_NAME));
    config.find(Setting.ANDROID_UDID).ifPresent(options::setUdid);
    config.find(Setting.ANDROID_PLATFORM_VERSION).ifPresent(options::setPlatformVersion);
    config.find(Setting.ANDROID_AVD).ifPresent(options::setAvd);

    var app = config.find(Setting.ANDROID_APP);
    var appPackage = config.find(Setting.ANDROID_APP_PACKAGE);
    if (app.isEmpty() && appPackage.isEmpty()) {
      throw new ConfigurationException(
          "Android needs ANDROID_APP (path or URL to an .apk) and/or ANDROID_APP_PACKAGE"
              + " (an app already installed on the device). See docs/CONFIGURATION.md.");
    }
    app.map(CapabilitiesFactory::resolveApp).ifPresent(options::setApp);
    appPackage.ifPresent(options::setAppPackage);
    config.find(Setting.ANDROID_APP_ACTIVITY).ifPresent(options::setAppActivity);
    config.find(Setting.ANDROID_APP_WAIT_ACTIVITY).ifPresent(options::setAppWaitActivity);

    options.setAutoGrantPermissions(config.getBoolean(Setting.ANDROID_AUTO_GRANT_PERMISSIONS));
    config.findInt(Setting.ANDROID_SYSTEM_PORT).ifPresent(options::setSystemPort);
    config.findSeconds(Setting.ANDROID_ADB_EXEC_TIMEOUT).ifPresent(options::setAdbExecTimeout);
    config
        .findSeconds(Setting.ANDROID_INSTALL_TIMEOUT)
        .ifPresent(options::setAndroidInstallTimeout);

    applyCommon(options, config);
    return options;
  }

  public static XCUITestOptions ios(FrameworkConfig config) {
    XCUITestOptions options = new XCUITestOptions();
    var udid = config.find(Setting.IOS_UDID);
    var deviceName = config.find(Setting.IOS_DEVICE_NAME);
    if (udid.isEmpty() && deviceName.isEmpty()) {
      throw new ConfigurationException(
          "iOS needs IOS_UDID (real device or simulator) or IOS_DEVICE_NAME (simulator name,"
              + " e.g. the output of `xcrun simctl list devices`). See docs/CONFIGURATION.md.");
    }
    udid.ifPresent(options::setUdid);
    deviceName.ifPresent(options::setDeviceName);
    config.find(Setting.IOS_PLATFORM_VERSION).ifPresent(options::setPlatformVersion);

    var app = config.find(Setting.IOS_APP);
    var bundleId = config.find(Setting.IOS_BUNDLE_ID);
    if (app.isEmpty() && bundleId.isEmpty()) {
      throw new ConfigurationException(
          "iOS needs IOS_APP (.app for simulators, .ipa for real devices) and/or IOS_BUNDLE_ID"
              + " (an app already installed). See docs/CONFIGURATION.md.");
    }
    app.map(CapabilitiesFactory::resolveApp).ifPresent(options::setApp);
    bundleId.ifPresent(options::setBundleId);

    options.setAutoAcceptAlerts(config.getBoolean(Setting.IOS_AUTO_ACCEPT_ALERTS));
    config.findSeconds(Setting.IOS_WDA_LAUNCH_TIMEOUT).ifPresent(options::setWdaLaunchTimeout);
    config.findInt(Setting.IOS_WDA_LOCAL_PORT).ifPresent(options::setWdaLocalPort);
    if (config.getBoolean(Setting.IOS_USE_PREBUILT_WDA)) {
      options.usePrebuiltWda();
    }
    // Signing settings only matter for real devices; they are applied when a team id is given.
    config
        .find(Setting.IOS_XCODE_ORG_ID)
        .ifPresent(
            orgId ->
                options.setXcodeCertificate(
                    new XcodeCertificate(orgId, config.require(Setting.IOS_XCODE_SIGNING_ID))));
    config.find(Setting.IOS_UPDATED_WDA_BUNDLE_ID).ifPresent(options::setUpdatedWdaBundleId);

    applyCommon(options, config);
    return options;
  }

  private static void applyCommon(BaseOptions<?> options, FrameworkConfig config) {
    options.setNewCommandTimeout(config.getSeconds(Setting.TIMEOUT_NEW_COMMAND));
    options.setCapability("appium:noReset", config.getBoolean(Setting.APP_NO_RESET));
    options.setCapability("appium:fullReset", config.getBoolean(Setting.APP_FULL_RESET));
    config
        .find(Setting.CAPABILITIES_FILE)
        .map(Paths::get)
        .map(CapabilitiesFactory::readCapabilitiesFile)
        .ifPresent(extra -> extra.forEach(options::setCapability));
  }

  /**
   * URLs and device-farm references ({@code https://...}, {@code <scheme>:...}) are passed through
   * untouched; local paths are made absolute so they still work when the Appium server runs from a
   * different working directory.
   */
  static String resolveApp(String app) {
    // Two or more characters before ':' so Windows drive letters (C:\...) stay local paths.
    if (app.matches("^[A-Za-z][A-Za-z0-9+.-]+:.*")) {
      return app;
    }
    Path path = Paths.get(app).toAbsolutePath().normalize();
    if (!Files.exists(path)) {
      throw new ConfigurationException("Application file not found: " + path);
    }
    return path.toString();
  }

  static Map<String, Object> readCapabilitiesFile(Path path) {
    if (!Files.isRegularFile(path)) {
      throw new ConfigurationException("CAPABILITIES_FILE not found: " + path.toAbsolutePath());
    }
    try {
      String json = Files.readString(path, StandardCharsets.UTF_8);
      Map<String, Object> caps = new Json().toType(json, Json.MAP_TYPE);
      if (caps == null) {
        throw new ConfigurationException("CAPABILITIES_FILE must contain a JSON object: " + path);
      }
      return caps;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read CAPABILITIES_FILE " + path, e);
    } catch (JsonException | ClassCastException e) {
      throw new ConfigurationException("CAPABILITIES_FILE is not a JSON object: " + path, e);
    }
  }
}
