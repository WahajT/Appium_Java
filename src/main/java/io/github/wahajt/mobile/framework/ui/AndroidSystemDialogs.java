package io.github.wahajt.mobile.framework.ui;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Android system dialogs that are not part of the app under test, such as "Application
 * isn't responding" (ANR) prompts that frequently appear on slow emulators.
 */
public final class AndroidSystemDialogs {
  private static final Logger LOG = LoggerFactory.getLogger(AndroidSystemDialogs.class);
  private static final By ANR_WAIT_BUTTON = AppiumBy.id("android:id/aerr_wait");

  private AndroidSystemDialogs() {}

  /**
   * Presses "Wait" on any visible ANR dialog. Does nothing on iOS or when no dialog is shown.
   *
   * @return {@code true} when a dialog was dismissed
   */
  public static boolean dismissAnrIfPresent(AppiumDriver driver) {
    if (!(driver instanceof AndroidDriver)) {
      return false;
    }
    List<WebElement> buttons = driver.findElements(ANR_WAIT_BUTTON);
    if (buttons.isEmpty()) {
      return false;
    }
    LOG.warn("Android 'not responding' dialog detected; pressing Wait");
    buttons.get(0).click();
    return true;
  }
}
