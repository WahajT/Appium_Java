package io.github.wahajt.mobile.examples.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import io.appium.java_client.appmanagement.ApplicationState;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import io.github.wahajt.mobile.framework.config.Setting;
import io.github.wahajt.mobile.framework.driver.DriverManager;
import io.github.wahajt.mobile.framework.reporting.Artifacts;
import io.github.wahajt.mobile.framework.ui.DeviceActions;
import java.nio.file.Paths;
import java.time.Duration;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * App-agnostic steps: they work against whatever application is configured, without any
 * app-specific locators. Use them as a first smoke test and as a reference for writing steps.
 */
public class AppSteps {
  private Scenario scenario;

  @Before
  public void rememberScenario(Scenario scenario) {
    this.scenario = scenario;
  }

  private DeviceActions device() {
    return new DeviceActions(DriverManager.driver(), FrameworkConfig.get());
  }

  @Given("the application is launched")
  public void theApplicationIsLaunched() {
    // The Cucumber hooks start the session, which installs (if needed) and launches the app.
    DriverManager.driver();
  }

  @When("the application is sent to the background for {int} seconds")
  public void sendToBackground(int seconds) {
    device().sendAppToBackground(Duration.ofSeconds(seconds));
  }

  @Then("the application is running in the foreground")
  public void runningInForeground() {
    DeviceActions device = device();
    FrameworkConfig config = FrameworkConfig.get();
    // Apps can take a moment to settle after launch or resume, so poll instead of sleeping.
    ApplicationState state =
        new WebDriverWait(DriverManager.driver(), config.getSeconds(Setting.TIMEOUT_EXPLICIT_WAIT))
            .withMessage("app " + device.appId() + " to be running in the foreground")
            .until(
                d -> {
                  ApplicationState current = device.appState();
                  return current == ApplicationState.RUNNING_IN_FOREGROUND ? current : null;
                });
    assertEquals(state, ApplicationState.RUNNING_IN_FOREGROUND, "App state of " + device.appId());
  }

  @Then("the current screen has visible content")
  public void screenHasContent() {
    String source = DriverManager.driver().getPageSource();
    assertFalse(source == null || source.isBlank(), "Page source should not be empty");
  }

  @Then("I capture a screenshot named {string}")
  public void captureScreenshot(String name) {
    Artifacts.Artifact shot =
        Artifacts.screenshot(
            DriverManager.driver(),
            Paths.get(FrameworkConfig.get().require(Setting.SCREENSHOT_DIR)),
            name);
    scenario.attach(shot.bytes(), "image/png", name);
  }
}
