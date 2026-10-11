package io.github.wahajt.mobile.framework.cucumber;

import io.appium.java_client.AppiumDriver;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import io.github.wahajt.mobile.framework.config.Setting;
import io.github.wahajt.mobile.framework.driver.AppiumServerManager;
import io.github.wahajt.mobile.framework.driver.DriverManager;
import io.github.wahajt.mobile.framework.reporting.Artifacts;
import io.github.wahajt.mobile.framework.ui.AndroidSystemDialogs;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scenario lifecycle: one fresh Appium session per scenario, failure screenshots, optional screen
 * recording, and guaranteed cleanup. Add this package to your Cucumber glue to use it.
 *
 * <p>Tag a scenario with {@code @record} to record its screen even when {@code VIDEO_RECORD} is
 * false.
 */
public class Hooks {
  private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);
  private static final String RECORD_TAG = "@record";

  private boolean recording;

  @BeforeAll
  public static void beforeAll() {
    FrameworkConfig config = FrameworkConfig.get();
    LOG.info("Effective configuration: {}", config.describe());
    AppiumServerManager.startIfConfigured(config);
  }

  @AfterAll
  public static void afterAll() {
    AppiumServerManager.stop();
  }

  @Before(order = 0)
  public void startSession(Scenario scenario) {
    FrameworkConfig config = FrameworkConfig.get();
    AppiumDriver driver = DriverManager.start(config);
    if (config.getBoolean(Setting.ANDROID_ANR_WATCHER)) {
      AndroidSystemDialogs.dismissAnrIfPresent(driver);
    }
    if (config.getBoolean(Setting.VIDEO_RECORD)
        || scenario.getSourceTagNames().contains(RECORD_TAG)) {
      try {
        Artifacts.startRecording(driver);
        recording = true;
      } catch (RuntimeException e) {
        LOG.warn("Screen recording could not be started: {}", e.getMessage());
      }
    }
  }

  @After(order = 0)
  public void endSession(Scenario scenario) {
    FrameworkConfig config = FrameworkConfig.get();
    try {
      if (DriverManager.hasDriver()) {
        AppiumDriver driver = DriverManager.driver();
        String name = Artifacts.safeFileName(scenario.getName());
        if (scenario.isFailed() && config.getBoolean(Setting.SCREENSHOT_ON_FAILURE)) {
          attachScreenshot(scenario, driver, config, name);
        }
        if (recording) {
          attachRecording(scenario, driver, config, name);
        }
      }
    } finally {
      recording = false;
      DriverManager.quit();
    }
  }

  private static void attachScreenshot(
      Scenario scenario, AppiumDriver driver, FrameworkConfig config, String name) {
    try {
      Artifacts.Artifact shot =
          Artifacts.screenshot(driver, Paths.get(config.require(Setting.SCREENSHOT_DIR)), name);
      scenario.attach(shot.bytes(), "image/png", "Screenshot on failure");
      LOG.info("Failure screenshot saved to {}", shot.file());
    } catch (RuntimeException e) {
      LOG.warn("Could not capture failure screenshot: {}", e.getMessage());
    }
  }

  private static void attachRecording(
      Scenario scenario, AppiumDriver driver, FrameworkConfig config, String name) {
    try {
      Artifacts.Artifact video =
          Artifacts.stopRecording(driver, Paths.get(config.require(Setting.VIDEO_DIR)), name);
      scenario.attach(video.bytes(), "video/mp4", "Screen recording");
      LOG.info("Screen recording saved to {}", video.file());
    } catch (RuntimeException e) {
      LOG.warn("Could not save screen recording: {}", e.getMessage());
    }
  }
}
