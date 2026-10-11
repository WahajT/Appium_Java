package io.github.wahajt.mobile.framework.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable, layered view of the framework configuration.
 *
 * <p>Resolution order for every {@link Setting} (first non-blank value wins):
 *
 * <ol>
 *   <li>JVM system property, e.g. {@code -Dandroid.app=/path/app.apk}
 *   <li>Environment variable, e.g. {@code ANDROID_APP=/path/app.apk}
 *   <li>Local {@code .env} file (path overridable with {@code -Dconfig.file} or {@code
 *       CONFIG_FILE})
 *   <li>Built-in default from {@link Setting#defaultValue()}
 * </ol>
 */
public final class FrameworkConfig {

  public static final String CONFIG_FILE_PROPERTY = "config.file";
  public static final String CONFIG_FILE_ENV = "CONFIG_FILE";
  public static final String DEFAULT_CONFIG_FILE = ".env";

  private static volatile FrameworkConfig current;

  private final Map<String, String> systemProperties;
  private final Map<String, String> environment;
  private final Map<String, String> fileValues;

  public FrameworkConfig(
      Map<String, String> systemProperties,
      Map<String, String> environment,
      Map<String, String> fileValues) {
    this.systemProperties = Map.copyOf(systemProperties);
    this.environment = Map.copyOf(environment);
    this.fileValues = Map.copyOf(fileValues);
  }

  /** Lazily loads the configuration once per JVM from the real system properties, env and file. */
  public static FrameworkConfig get() {
    FrameworkConfig config = current;
    if (config == null) {
      synchronized (FrameworkConfig.class) {
        config = current;
        if (config == null) {
          config = load();
          current = config;
        }
      }
    }
    return config;
  }

  /** Reads system properties, environment variables and the optional {@code .env} file. */
  public static FrameworkConfig load() {
    Map<String, String> sys = new HashMap<>();
    System.getProperties().forEach((k, v) -> sys.put(String.valueOf(k), String.valueOf(v)));
    Map<String, String> env = System.getenv();

    String file = firstNonBlank(sys.get(CONFIG_FILE_PROPERTY), env.get(CONFIG_FILE_ENV));
    Path path = Paths.get(file != null ? file : DEFAULT_CONFIG_FILE);
    if (file != null && !Files.isRegularFile(path)) {
      throw new ConfigurationException("Configuration file not found: " + path.toAbsolutePath());
    }
    return new FrameworkConfig(sys, env, Files.isRegularFile(path) ? readEnvFile(path) : Map.of());
  }

  /**
   * Parses a dotenv-style file: {@code KEY=value} lines, {@code #} comments, optional {@code
   * export} prefix and optional single or double quotes around the value.
   */
  static Map<String, String> readEnvFile(Path path) {
    try {
      return parseEnv(Files.readAllLines(path, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read configuration file " + path, e);
    }
  }

  static Map<String, String> parseEnv(List<String> lines) {
    Map<String, String> values = new LinkedHashMap<>();
    for (String raw : lines) {
      String line = raw.strip();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      if (line.startsWith("export ")) {
        line = line.substring("export ".length()).strip();
      }
      int eq = line.indexOf('=');
      if (eq <= 0) {
        continue;
      }
      String key = line.substring(0, eq).strip();
      String value = line.substring(eq + 1).strip();
      if (value.length() >= 2
          && (value.startsWith("\"") && value.endsWith("\"")
              || value.startsWith("'") && value.endsWith("'"))) {
        value = value.substring(1, value.length() - 1);
      } else {
        int comment = value.indexOf(" #");
        if (comment >= 0) {
          value = value.substring(0, comment).strip();
        }
      }
      values.put(key, value);
    }
    return values;
  }

  /** Resolved value, or empty when the setting is unset and has no default. */
  public Optional<String> find(Setting setting) {
    return Optional.ofNullable(
        firstNonBlank(
            systemProperties.get(setting.property()),
            environment.get(setting.envVar()),
            fileValues.get(setting.envVar()),
            setting.defaultValue()));
  }

  /** Resolved value; throws a descriptive {@link ConfigurationException} when unset. */
  public String require(Setting setting) {
    return find(setting)
        .orElseThrow(
            () ->
                new ConfigurationException(
                    "Missing required setting "
                        + setting.envVar()
                        + " (system property -D"
                        + setting.property()
                        + "). See docs/CONFIGURATION.md."));
  }

  public boolean getBoolean(Setting setting) {
    String value = find(setting).orElse("false").trim().toLowerCase(Locale.ROOT);
    switch (value) {
      case "true":
      case "yes":
      case "1":
        return true;
      case "false":
      case "no":
      case "0":
        return false;
      default:
        throw new ConfigurationException(
            setting.envVar() + " must be true or false but was '" + value + "'");
    }
  }

  public Optional<Integer> findInt(Setting setting) {
    return find(setting).map(v -> parseInt(setting, v));
  }

  public int getInt(Setting setting) {
    return parseInt(setting, require(setting));
  }

  /** Reads an integer setting expressed in seconds. */
  public Optional<Duration> findSeconds(Setting setting) {
    return findInt(setting).map(Duration::ofSeconds);
  }

  public Duration getSeconds(Setting setting) {
    return Duration.ofSeconds(getInt(setting));
  }

  public Platform platform() {
    return Platform.from(require(Setting.PLATFORM));
  }

  public URL appiumServerUrl() {
    String value = require(Setting.APPIUM_SERVER_URL);
    try {
      return new URI(value).toURL();
    } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
      throw new ConfigurationException("APPIUM_SERVER_URL is not a valid URL: " + mask(value), e);
    }
  }

  /**
   * Effective configuration for logging. Values that may carry credentials (URLs with user info)
   * are masked.
   */
  public Map<String, String> describe() {
    Map<String, String> out = new LinkedHashMap<>();
    for (Setting setting : Setting.values()) {
      find(setting).ifPresent(v -> out.put(setting.envVar(), mask(v)));
    }
    return Collections.unmodifiableMap(out);
  }

  /** Removes {@code user:password@} from URL-like values so they can be logged safely. */
  static String mask(String value) {
    return value.replaceAll("(?i)^([a-z][a-z0-9+.-]*://)[^/@\\s]+@", "$1****@");
  }

  private static int parseInt(Setting setting, String value) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new ConfigurationException(
          setting.envVar() + " must be an integer but was '" + value + "'", e);
    }
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }
}
