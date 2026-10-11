package io.github.wahajt.mobile.framework.config;

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

/** Keeps docs/CONFIGURATION.md and .env.example in sync with {@link Setting}. */
public class ConfigurationDocsTest {

  @Test
  public void everySettingIsDocumented() throws IOException {
    String docs = read("docs/CONFIGURATION.md");
    List<String> missing = new ArrayList<>();
    for (Setting setting : Setting.values()) {
      if (!docs.contains("`" + setting.envVar() + "`")
          || !docs.contains("`" + setting.property() + "`")) {
        missing.add(setting.envVar());
      }
    }
    assertTrue(missing.isEmpty(), "Settings missing from docs/CONFIGURATION.md: " + missing);
  }

  @Test
  public void everySettingAppearsInEnvExample() throws IOException {
    String example = read(".env.example");
    List<String> missing = new ArrayList<>();
    for (Setting setting : Setting.values()) {
      if (!example.contains(setting.envVar() + "=")) {
        missing.add(setting.envVar());
      }
    }
    assertTrue(missing.isEmpty(), "Settings missing from .env.example: " + missing);
  }

  private static String read(String relative) throws IOException {
    Path path = Paths.get(relative);
    assertTrue(Files.isRegularFile(path), relative + " should exist");
    return Files.readString(path, StandardCharsets.UTF_8);
  }
}
