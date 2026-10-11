package io.github.wahajt.mobile.framework.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import io.github.wahajt.mobile.framework.config.Setting;
import io.github.wahajt.mobile.framework.ui.DeviceActions;
import io.github.wahajt.mobile.framework.ui.ElementActions;
import io.github.wahajt.mobile.framework.ui.Gestures;
import org.openqa.selenium.By;

/**
 * Base class for page (screen) objects.
 *
 * <p>Subclasses declare {@link Locator} constants for both platforms, resolve them with {@link
 * #by(Locator)}, and expose intention-revealing methods ({@code loginAs(user, pass)}) rather than
 * raw element access. See {@code src/test/java/io/github/wahajt/mobile/examples/pages} for a
 * complete example.
 */
public abstract class BasePage {
  protected final AppiumDriver driver;
  protected final FrameworkConfig config;
  protected final ElementActions actions;
  protected final Gestures gestures;
  protected final DeviceActions device;

  protected BasePage(AppiumDriver driver) {
    this(driver, FrameworkConfig.get());
  }

  protected BasePage(AppiumDriver driver, FrameworkConfig config) {
    this.driver = driver;
    this.config = config;
    this.actions = new ElementActions(driver, config.getSeconds(Setting.TIMEOUT_EXPLICIT_WAIT));
    this.gestures = new Gestures(driver);
    this.device = new DeviceActions(driver, config);
  }

  /** {@code true} when the screen's identifying element is visible. */
  public abstract boolean isDisplayed();

  protected final boolean isAndroid() {
    return driver instanceof AndroidDriver;
  }

  /** Picks the locator matching the driver's platform. */
  protected final By by(Locator locator) {
    return isAndroid() ? locator.android() : locator.ios();
  }
}
