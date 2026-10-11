package io.github.wahajt.mobile.framework.ui;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

/** Touch gestures implemented with W3C actions, which work on both UiAutomator2 and XCUITest. */
public class Gestures {

  /** Direction the finger moves. Swiping {@link #UP} scrolls the content down. */
  public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT
  }

  private static final Duration SWIPE_DURATION = Duration.ofMillis(600);

  private final AppiumDriver driver;

  public Gestures(AppiumDriver driver) {
    this.driver = driver;
  }

  public void tap(Point point) {
    PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
    Sequence tap =
        new Sequence(finger, 0)
            .addAction(
                finger.createPointerMove(
                    Duration.ZERO, PointerInput.Origin.viewport(), point.getX(), point.getY()))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
    driver.perform(List.of(tap));
  }

  public void longPress(WebElement element, Duration holdFor) {
    Point center = center(element);
    PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
    Sequence press =
        new Sequence(finger, 0)
            .addAction(
                finger.createPointerMove(
                    Duration.ZERO, PointerInput.Origin.viewport(), center.getX(), center.getY()))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(
                finger.createPointerMove(
                    holdFor, PointerInput.Origin.viewport(), center.getX(), center.getY()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
    driver.perform(List.of(press));
  }

  /** Swipes across the middle of the screen, covering 60% of its height or width. */
  public void swipe(Direction direction) {
    Dimension size = driver.manage().window().getSize();
    int midX = size.getWidth() / 2;
    int midY = size.getHeight() / 2;
    int dy = (int) (size.getHeight() * 0.3);
    int dx = (int) (size.getWidth() * 0.3);
    switch (direction) {
      case UP:
        swipe(new Point(midX, midY + dy), new Point(midX, midY - dy));
        break;
      case DOWN:
        swipe(new Point(midX, midY - dy), new Point(midX, midY + dy));
        break;
      case LEFT:
        swipe(new Point(midX + dx, midY), new Point(midX - dx, midY));
        break;
      case RIGHT:
        swipe(new Point(midX - dx, midY), new Point(midX + dx, midY));
        break;
      default:
        throw new IllegalArgumentException("Unknown direction " + direction);
    }
  }

  public void swipe(Point from, Point to) {
    PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
    Sequence swipe =
        new Sequence(finger, 0)
            .addAction(
                finger.createPointerMove(
                    Duration.ZERO, PointerInput.Origin.viewport(), from.getX(), from.getY()))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(
                finger.createPointerMove(
                    SWIPE_DURATION, PointerInput.Origin.viewport(), to.getX(), to.getY()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
    driver.perform(List.of(swipe));
  }

  /**
   * Swipes up until an element matching {@code locator} is displayed.
   *
   * @throws NoSuchElementException when the element is still missing after {@code maxSwipes}
   */
  public WebElement scrollUntilVisible(By locator, int maxSwipes) {
    for (int attempt = 0; attempt <= maxSwipes; attempt++) {
      for (WebElement element : driver.findElements(locator)) {
        if (element.isDisplayed()) {
          return element;
        }
      }
      if (attempt < maxSwipes) {
        swipe(Direction.UP);
      }
    }
    throw new NoSuchElementException(
        "Element " + locator + " not visible after " + maxSwipes + " swipes");
  }

  /**
   * Scrolls to the first element whose visible text equals {@code text}. On Android this uses
   * UiScrollable; on iOS it swipes until a label, name or value matches.
   */
  public WebElement scrollToText(String text, int maxSwipes) {
    if (driver instanceof AndroidDriver) {
      String selector =
          "new UiScrollable(new UiSelector().scrollable(true)).setMaxSearchSwipes("
              + maxSwipes
              + ").scrollIntoView(new UiSelector().text(\""
              + escapeUiSelector(text)
              + "\"))";
      return driver.findElement(AppiumBy.androidUIAutomator(selector));
    }
    String quoted = escapePredicate(text);
    String predicate =
        "label == '" + quoted + "' OR name == '" + quoted + "' OR value == '" + quoted + "'";
    return scrollUntilVisible(AppiumBy.iOSNsPredicateString(predicate), maxSwipes);
  }

  static String escapeUiSelector(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  static String escapePredicate(String text) {
    return text.replace("\\", "\\\\").replace("'", "\\'");
  }

  private static Point center(WebElement element) {
    Point location = element.getLocation();
    Dimension size = element.getSize();
    return new Point(location.getX() + size.getWidth() / 2, location.getY() + size.getHeight() / 2);
  }
}
