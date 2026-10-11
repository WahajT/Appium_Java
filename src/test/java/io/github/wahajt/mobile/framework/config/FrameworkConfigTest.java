package io.github.wahajt.mobile.framework.config;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.testng.annotations.Test;

public class FrameworkConfigTest {

  @Test
  public void systemPropertyBeatsEnvironmentBeatsFileBeatsDefault() {
    Map<String, String> file = Map.of("APPIUM_SERVER_URL", "http://file:1");
    Map<String, String> env = Map.of("APPIUM_SERVER_URL", "http://env:2");
    Map<String, String> sys = Map.of("appium.server.url", "http://sys:3");

    assertEquals(
        new FrameworkConfig(sys, env, file).require(Setting.APPIUM_SERVER_URL), "http://sys:3");
    assertEquals(
        new FrameworkConfig(Map.of(), env, file).require(Setting.APPIUM_SERVER_URL),
        "http://env:2");
    assertEquals(
        new FrameworkConfig(Map.of(), Map.of(), file).require(Setting.APPIUM_SERVER_URL),
        "http://file:1");
    assertEquals(
        new FrameworkConfig(Map.of(), Map.of(), Map.of()).require(Setting.APPIUM_SERVER_URL),
        "http://127.0.0.1:4723");
  }

  @Test
  public void blankValuesFallThroughToNextSource() {
    FrameworkConfig config =
        new FrameworkConfig(
            Map.of("android.app", "  "), Map.of("ANDROID_APP", ""), Map.of("ANDROID_APP", "a.apk"));
    assertEquals(config.find(Setting.ANDROID_APP), Optional.of("a.apk"));
  }

  @Test
  public void unsetSettingWithoutDefaultIsEmptyAndRequireExplains() {
    FrameworkConfig config = new FrameworkConfig(Map.of(), Map.of(), Map.of());
    assertFalse(config.find(Setting.IOS_UDID).isPresent());
    ConfigurationException e =
        expectThrows(ConfigurationException.class, () -> config.require(Setting.IOS_UDID));
    assertTrue(e.getMessage().contains("IOS_UDID"));
    assertTrue(e.getMessage().contains("-Dios.udid"));
  }

  @Test
  public void parsesTypedValues() {
    FrameworkConfig config =
        new FrameworkConfig(
            Map.of(),
            Map.of("TIMEOUT_EXPLICIT_WAIT", "7", "VIDEO_RECORD", "YES", "PLATFORM", "iOS"),
            Map.of());
    assertEquals(config.getSeconds(Setting.TIMEOUT_EXPLICIT_WAIT), Duration.ofSeconds(7));
    assertTrue(config.getBoolean(Setting.VIDEO_RECORD));
    assertFalse(config.getBoolean(Setting.APP_NO_RESET));
    assertEquals(config.platform(), Platform.IOS);
  }

  @Test
  public void rejectsInvalidValuesWithTheSettingName() {
    FrameworkConfig config =
        new FrameworkConfig(
            Map.of(),
            Map.of("RETRY_COUNT", "two", "VIDEO_RECORD", "maybe", "PLATFORM", "windows"),
            Map.of());
    assertTrue(
        expectThrows(ConfigurationException.class, () -> config.getInt(Setting.RETRY_COUNT))
            .getMessage()
            .contains("RETRY_COUNT"));
    assertTrue(
        expectThrows(ConfigurationException.class, () -> config.getBoolean(Setting.VIDEO_RECORD))
            .getMessage()
            .contains("VIDEO_RECORD"));
    assertThrows(ConfigurationException.class, config::platform);
  }

  @Test
  public void rejectsMalformedServerUrl() {
    FrameworkConfig config =
        new FrameworkConfig(Map.of(), Map.of("APPIUM_SERVER_URL", "not a url"), Map.of());
    assertThrows(ConfigurationException.class, config::appiumServerUrl);
  }

  @Test
  public void parsesDotenvSyntax() {
    Map<String, String> values =
        FrameworkConfig.parseEnv(
            List.of(
                "# comment",
                "",
                "PLATFORM=ios",
                "export IOS_DEVICE_NAME=\"iPhone Simulator\"",
                "IOS_BUNDLE_ID='com.example.app'",
                "TIMEOUT_EXPLICIT_WAIT=20 # inline comment",
                "not a pair",
                "EMPTY="));
    assertEquals(values.get("PLATFORM"), "ios");
    assertEquals(values.get("IOS_DEVICE_NAME"), "iPhone Simulator");
    assertEquals(values.get("IOS_BUNDLE_ID"), "com.example.app");
    assertEquals(values.get("TIMEOUT_EXPLICIT_WAIT"), "20");
    assertEquals(values.get("EMPTY"), "");
    assertEquals(values.size(), 5);
  }

  @Test
  public void describeMasksCredentialsInUrls() {
    FrameworkConfig config =
        new FrameworkConfig(
            Map.of(),
            Map.of("APPIUM_SERVER_URL", "https://user:s3cret@grid.example.com/wd/hub"),
            Map.of());
    String described = config.describe().get("APPIUM_SERVER_URL");
    assertEquals(described, "https://****@grid.example.com/wd/hub");
    assertEquals(FrameworkConfig.mask("http://127.0.0.1:4723"), "http://127.0.0.1:4723");
  }

  @Test
  public void envVarNamesMatchPropertyNames() {
    for (Setting setting : Setting.values()) {
      assertEquals(
          setting.envVar(),
          setting.property().toUpperCase(Locale.ROOT).replace('.', '_'),
          "Setting " + setting + " breaks the property <-> env var naming rule");
    }
  }
}
