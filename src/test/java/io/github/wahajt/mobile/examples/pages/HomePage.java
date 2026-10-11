package io.github.wahajt.mobile.examples.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.github.wahajt.mobile.framework.pages.BasePage;
import io.github.wahajt.mobile.framework.pages.Locator;

/** TEMPLATE page object for the screen shown after signing in. Replace the placeholder id. */
public class HomePage extends BasePage {
  private static final Locator ROOT = Locator.both(AppiumBy.accessibilityId("home_screen"));

  public HomePage(AppiumDriver driver) {
    super(driver);
  }

  @Override
  public boolean isDisplayed() {
    return actions.isDisplayedWithin(by(ROOT), actions.timeout());
  }
}
