package io.github.wahajt.mobile.framework.ui;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.HasOnScreenKeyboard;
import io.appium.java_client.HidesKeyboard;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.appmanagement.ApplicationState;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import io.github.wahajt.mobile.framework.config.Setting;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.openqa.selenium.WebDriverException;

/** App- and device-level operations that differ slightly between Android and iOS. */
public class DeviceActions {
  private final AppiumDriver driver;
  private final FrameworkConfig config;

  public DeviceActions(AppiumDriver driver, FrameworkConfig config) {
    this.driver = driver;
    this.config = config;
  }

  /**
   * Identifier of the app under test: the configured package / bundle id, or else the app that is
   * currently in the foreground.
   */
  public String appId() {
    Optional<String> configured =
        driver instanceof AndroidDriver
            ? config.find(Setting.ANDROID_APP_PACKAGE)
            : config.find(Setting.IOS_BUNDLE_ID);
    return configured.orElseGet(this::foregroundAppId);
  }

  /** Package (Android) or bundle id (iOS) of the app currently in the foreground. */
  public String foregroundAppId() {
    if (driver instanceof AndroidDriver) {
      return ((AndroidDriver) driver).getCurrentPackage();
    }
    Object info = driver.executeScript("mobile: activeAppInfo");
    if (info instanceof Map) {
      Object bundleId = ((Map<?, ?>) info).get("bundleId");
      if (bundleId != null) {
        return bundleId.toString();
      }
    }
    throw new WebDriverException("Could not determine the active iOS app: " + info);
  }

  public ApplicationState appState() {
    return ((InteractsWithApps) driver).queryAppState(appId());
  }

  public void sendAppToBackground(Duration duration) {
    ((InteractsWithApps) driver).runAppInBackground(duration);
  }

  public void activateApp() {
    ((InteractsWithApps) driver).activateApp(appId());
  }

  public boolean terminateApp() {
    return ((InteractsWithApps) driver).terminateApp(appId());
  }

  public boolean isKeyboardShown() {
    return ((HasOnScreenKeyboard) driver).isKeyboardShown();
  }

  /** Hides the soft keyboard if it is visible. */
  public void hideKeyboard() {
    if (isKeyboardShown()) {
      ((HidesKeyboard) driver).hideKeyboard();
    }
  }

  /** Android system back. On iOS this maps to the navigation-bar back action when available. */
  public void back() {
    driver.navigate().back();
  }
}
