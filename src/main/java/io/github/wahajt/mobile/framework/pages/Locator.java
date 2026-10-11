package io.github.wahajt.mobile.framework.pages;

import java.util.Objects;
import org.openqa.selenium.By;

/**
 * A locator with one {@link By} per platform. Declare these as {@code static final} constants in
 * page objects and resolve them with {@link BasePage#by(Locator)}.
 */
public final class Locator {
  private final By android;
  private final By ios;

  private Locator(By android, By ios) {
    this.android = Objects.requireNonNull(android, "android locator");
    this.ios = Objects.requireNonNull(ios, "ios locator");
  }

  /** Different locators per platform. */
  public static Locator of(By android, By ios) {
    return new Locator(android, ios);
  }

  /** The same locator on both platforms (e.g. a shared accessibility id). */
  public static Locator both(By locator) {
    return new Locator(locator, locator);
  }

  public By android() {
    return android;
  }

  public By ios() {
    return ios;
  }

  @Override
  public String toString() {
    return "Locator{android=" + android + ", ios=" + ios + "}";
  }
}
