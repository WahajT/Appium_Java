package io.github.wahajt.mobile.framework.driver;

import io.appium.java_client.AppiumDriver;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds one Appium session per thread so scenarios running on different threads never share a
 * driver. Always pair {@link #start(FrameworkConfig)} with {@link #quit()} (the Cucumber hooks do
 * this for you).
 */
public final class DriverManager {
  private static final Logger LOG = LoggerFactory.getLogger(DriverManager.class);
  private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

  private DriverManager() {}

  /** Starts a new session for the current thread, quitting any session left over on it. */
  public static AppiumDriver start(FrameworkConfig config) {
    quit();
    AppiumDriver driver = DriverFactory.create(config);
    DRIVER.set(driver);
    return driver;
  }

  /** Session for the current thread; fails fast when no session was started. */
  public static AppiumDriver driver() {
    AppiumDriver driver = DRIVER.get();
    if (driver == null) {
      throw new IllegalStateException(
          "No Appium session on thread '"
              + Thread.currentThread().getName()
              + "'. Start one with DriverManager.start(config) or run through the Cucumber hooks.");
    }
    return driver;
  }

  public static boolean hasDriver() {
    return DRIVER.get() != null;
  }

  /** Ends the current thread's session. Safe to call when no session exists. */
  public static void quit() {
    AppiumDriver driver = DRIVER.get();
    DRIVER.remove();
    if (driver != null) {
      try {
        driver.quit();
      } catch (RuntimeException e) {
        LOG.warn("Ignoring error while quitting the Appium session: {}", e.getMessage());
      }
    }
  }
}
