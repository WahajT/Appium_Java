package io.github.wahajt.mobile.examples.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.github.wahajt.mobile.framework.pages.BasePage;
import io.github.wahajt.mobile.framework.pages.Locator;

/**
 * TEMPLATE page object for a typical sign-in screen.
 *
 * <p>The accessibility ids below are placeholders. Replace them with the ids of your own app
 * (inspect it with Appium Inspector) before running {@code features/templates/login.feature}.
 * Prefer accessibility ids: they are fast and map to {@code content-desc} on Android and {@code
 * accessibilityIdentifier} on iOS. Use {@link Locator#of} when the platforms differ.
 */
public class LoginPage extends BasePage {
  private static final Locator USERNAME = Locator.both(AppiumBy.accessibilityId("username_input"));
  private static final Locator PASSWORD = Locator.both(AppiumBy.accessibilityId("password_input"));
  private static final Locator SUBMIT =
      Locator.of(
          AppiumBy.androidUIAutomator("new UiSelector().resourceIdMatches(\".*:id/login_button\")"),
          AppiumBy.iOSClassChain("**/XCUIElementTypeButton[`name == \"login_button\"`]"));
  private static final Locator ERROR = Locator.both(AppiumBy.accessibilityId("login_error"));

  public LoginPage(AppiumDriver driver) {
    super(driver);
  }

  @Override
  public boolean isDisplayed() {
    return actions.isDisplayedWithin(by(USERNAME), actions.timeout());
  }

  public HomePage loginAs(String user, String pass) {
    actions.type(by(USERNAME), user);
    actions.type(by(PASSWORD), pass);
    device.hideKeyboard();
    actions.click(by(SUBMIT));
    return new HomePage(driver);
  }

  public String errorMessage() {
    return actions.text(by(ERROR));
  }
}
