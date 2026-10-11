package io.github.wahajt.mobile.framework.driver;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import io.appium.java_client.remote.options.BaseOptions;
import io.github.wahajt.mobile.framework.config.ConfigurationException;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CapabilitiesFactoryTest {
  private Path tempDir;
  private Path apk;
  private Path iosApp;

  @BeforeClass
  public void createFiles() throws IOException {
    tempDir = Files.createTempDirectory("caps-test");
    apk = Files.createFile(tempDir.resolve("app.apk"));
    iosApp = Files.createDirectory(tempDir.resolve("App.app"));
  }

  @AfterClass(alwaysRun = true)
  public void deleteFiles() throws IOException {
    Files.deleteIfExists(apk);
    Files.deleteIfExists(iosApp);
    Files.deleteIfExists(tempDir.resolve("caps.json"));
    Files.deleteIfExists(tempDir);
  }

  private static FrameworkConfig config(Map<String, String> env) {
    return new FrameworkConfig(Map.of(), env, Map.of());
  }

  @Test
  public void androidUsesUiAutomator2AndOnlyAndroidCapabilities() {
    Map<String, String> env = new HashMap<>();
    env.put("PLATFORM", "android");
    env.put("ANDROID_APP", apk.toString());
    env.put("ANDROID_APP_PACKAGE", "com.example.app");
    env.put("ANDROID_APP_ACTIVITY", ".MainActivity");
    env.put("ANDROID_UDID", "emulator-5554");
    env.put("ANDROID_SYSTEM_PORT", "8201");
    env.put("ANDROID_INSTALL_TIMEOUT", "180");
    env.put("IOS_BUNDLE_ID", "must.not.leak");

    Map<String, Object> caps = CapabilitiesFactory.forConfig(config(env)).asMap();

    assertEquals(String.valueOf(caps.get("platformName")).toLowerCase(Locale.ROOT), "android");
    assertTrue("UiAutomator2".equalsIgnoreCase((String) caps.get("appium:automationName")));
    assertEquals(caps.get("appium:app"), apk.toAbsolutePath().toString());
    assertEquals(caps.get("appium:appPackage"), "com.example.app");
    assertEquals(caps.get("appium:appActivity"), ".MainActivity");
    assertEquals(caps.get("appium:udid"), "emulator-5554");
    assertEquals(caps.get("appium:deviceName"), "Android Emulator");
    assertEquals(caps.get("appium:systemPort"), 8201);
    assertEquals(caps.get("appium:androidInstallTimeout"), 180_000L);
    assertEquals(caps.get("appium:autoGrantPermissions"), true);
    assertEquals(caps.get("appium:newCommandTimeout"), 120L);
    assertEquals(caps.get("appium:noReset"), false);
    assertNull(caps.get("appium:bundleId"));
    assertFalse(caps.containsKey("appium:autoAcceptAlerts"));
  }

  @Test
  public void iosUsesXcuiTestAndOnlyIosCapabilities() {
    Map<String, String> env = new HashMap<>();
    env.put("PLATFORM", "ios");
    env.put("IOS_APP", iosApp.toString());
    env.put("IOS_DEVICE_NAME", "iPhone Simulator");
    env.put("IOS_PLATFORM_VERSION", "17.5");
    env.put("IOS_WDA_LAUNCH_TIMEOUT", "90");
    env.put("ANDROID_APP_PACKAGE", "must.not.leak");

    Map<String, Object> caps = CapabilitiesFactory.forConfig(config(env)).asMap();

    assertEquals(String.valueOf(caps.get("platformName")).toLowerCase(Locale.ROOT), "ios");
    assertTrue("XCUITest".equalsIgnoreCase((String) caps.get("appium:automationName")));
    assertEquals(caps.get("appium:app"), iosApp.toAbsolutePath().toString());
    assertEquals(caps.get("appium:deviceName"), "iPhone Simulator");
    assertEquals(caps.get("appium:platformVersion"), "17.5");
    assertEquals(caps.get("appium:wdaLaunchTimeout"), 90_000L);
    assertEquals(caps.get("appium:autoAcceptAlerts"), false);
    assertNull(caps.get("appium:appPackage"));
    assertFalse(caps.containsKey("appium:autoGrantPermissions"));
    assertFalse(caps.containsKey("appium:xcodeOrgId"), "signing is only set for real devices");
  }

  @Test
  public void iosRealDeviceSigningIsAppliedWhenTeamIdIsSet() {
    Map<String, Object> caps =
        CapabilitiesFactory.forConfig(
                config(
                    Map.of(
                        "PLATFORM", "ios",
                        "IOS_BUNDLE_ID", "com.example.app",
                        "IOS_UDID", "00000000-0000000000000000",
                        "IOS_XCODE_ORG_ID", "TEAMID1234",
                        "IOS_UPDATED_WDA_BUNDLE_ID", "com.example.WebDriverAgentRunner")))
            .asMap();
    assertEquals(caps.get("appium:xcodeOrgId"), "TEAMID1234");
    assertEquals(caps.get("appium:xcodeSigningId"), "Apple Development");
    assertEquals(caps.get("appium:updatedWDABundleId"), "com.example.WebDriverAgentRunner");
    assertFalse(caps.containsKey("appium:app"));
  }

  @Test
  public void missingAppIsReportedPerPlatform() {
    ConfigurationException android =
        expectThrows(
            ConfigurationException.class,
            () -> CapabilitiesFactory.forConfig(config(Map.of("PLATFORM", "android"))));
    assertTrue(android.getMessage().contains("ANDROID_APP"));

    ConfigurationException ios =
        expectThrows(
            ConfigurationException.class,
            () ->
                CapabilitiesFactory.forConfig(
                    config(Map.of("PLATFORM", "ios", "IOS_DEVICE_NAME", "iPhone"))));
    assertTrue(ios.getMessage().contains("IOS_APP"));
  }

  @Test
  public void iosRequiresDeviceNameOrUdid() {
    ConfigurationException e =
        expectThrows(
            ConfigurationException.class,
            () ->
                CapabilitiesFactory.forConfig(
                    config(Map.of("PLATFORM", "ios", "IOS_BUNDLE_ID", "com.example.app"))));
    assertTrue(e.getMessage().contains("IOS_UDID"));
  }

  @Test
  public void nonexistentLocalAppFileFailsFastButUrlsPassThrough() {
    assertThrows(
        ConfigurationException.class,
        () -> CapabilitiesFactory.resolveApp(tempDir.resolve("missing.apk").toString()));
    assertEquals(
        CapabilitiesFactory.resolveApp("https://downloads.example.com/app.apk"),
        "https://downloads.example.com/app.apk");
    assertEquals(
        CapabilitiesFactory.resolveApp("storage:filename=app.apk"), "storage:filename=app.apk");
  }

  @Test
  public void capabilitiesFileIsMergedLastAndCanOverride() throws IOException {
    Path json = tempDir.resolve("caps.json");
    Files.writeString(
        json,
        "{\"appium:noReset\": true, \"language\": \"fr\", \"appium:orientation\": \"PORTRAIT\"}");
    BaseOptions<?> options =
        CapabilitiesFactory.forConfig(
            config(
                Map.of(
                    "PLATFORM", "android",
                    "ANDROID_APP_PACKAGE", "com.example.app",
                    "CAPABILITIES_FILE", json.toString())));
    Map<String, Object> caps = options.asMap();
    assertEquals(caps.get("appium:noReset"), true);
    assertEquals(caps.get("appium:language"), "fr", "non-W3C names get the appium: prefix");
    assertEquals(caps.get("appium:orientation"), "PORTRAIT");
  }

  @Test
  public void invalidCapabilitiesFileIsRejected() throws IOException {
    Path json = tempDir.resolve("caps.json");
    Files.writeString(json, "[1, 2]");
    assertThrows(
        ConfigurationException.class, () -> CapabilitiesFactory.readCapabilitiesFile(json));
    assertThrows(
        ConfigurationException.class,
        () -> CapabilitiesFactory.readCapabilitiesFile(tempDir.resolve("nope.json")));
  }
}
