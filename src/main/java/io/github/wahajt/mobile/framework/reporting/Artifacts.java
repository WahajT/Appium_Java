package io.github.wahajt.mobile.framework.reporting;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;
import io.appium.java_client.ios.IOSStartScreenRecordingOptions;
import io.appium.java_client.screenrecording.CanRecordScreen;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.openqa.selenium.OutputType;

/**
 * Captures screenshots and screen recordings and writes them to disk.
 *
 * <p>This class is independent of any reporting library: it returns the bytes and the file it
 * wrote, so callers can attach them to Cucumber, Allure, or any other report.
 */
public final class Artifacts {
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
  private static final Duration MAX_RECORDING = Duration.ofMinutes(10);

  private Artifacts() {}

  /** A captured artifact: its raw bytes and where it was saved. */
  public static final class Artifact {
    private final byte[] bytes;
    private final Path file;

    Artifact(byte[] bytes, Path file) {
      this.bytes = bytes;
      this.file = file;
    }

    public byte[] bytes() {
      return bytes.clone();
    }

    public Path file() {
      return file;
    }
  }

  public static Artifact screenshot(AppiumDriver driver, Path directory, String name) {
    byte[] png = driver.getScreenshotAs(OutputType.BYTES);
    return new Artifact(png, write(directory, name, ".png", png));
  }

  public static void startRecording(AppiumDriver driver) {
    if (driver instanceof AndroidDriver) {
      ((CanRecordScreen) driver)
          .startRecordingScreen(
              new AndroidStartScreenRecordingOptions().withTimeLimit(MAX_RECORDING));
    } else if (driver instanceof CanRecordScreen) {
      ((CanRecordScreen) driver)
          .startRecordingScreen(new IOSStartScreenRecordingOptions().withTimeLimit(MAX_RECORDING));
    }
  }

  public static Artifact stopRecording(AppiumDriver driver, Path directory, String name) {
    String base64 = ((CanRecordScreen) driver).stopRecordingScreen();
    byte[] mp4 = Base64.getMimeDecoder().decode(base64);
    return new Artifact(mp4, write(directory, name, ".mp4", mp4));
  }

  /** Turns arbitrary text (e.g. a scenario name) into a safe file name. */
  public static String safeFileName(String name) {
    String cleaned = name == null ? "" : name.replaceAll("[^A-Za-z0-9._-]+", "_");
    cleaned = cleaned.replaceAll("^_+|_+$", "");
    if (cleaned.isEmpty()) {
      cleaned = "artifact";
    }
    return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
  }

  private static Path write(Path directory, String name, String extension, byte[] data) {
    try {
      Files.createDirectories(directory);
      Path file =
          directory.resolve(
              safeFileName(name) + "-" + LocalDateTime.now().format(STAMP) + extension);
      return Files.write(file, data);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write artifact to " + directory, e);
    }
  }
}
