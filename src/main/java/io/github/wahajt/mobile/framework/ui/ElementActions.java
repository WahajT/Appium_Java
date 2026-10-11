package io.github.wahajt.mobile.framework.ui;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Element interactions built on explicit waits. Use these instead of {@code Thread.sleep}: every
 * action waits until the element is in the required state or the timeout expires.
 */
public class ElementActions {
  private final WebDriver driver;
  private final Duration timeout;

  public ElementActions(WebDriver driver, Duration timeout) {
    this.driver = driver;
    this.timeout = timeout;
  }

  public WebElement waitVisible(By locator) {
    return waitFor(timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
  }

  public WebElement waitClickable(By locator) {
    return waitFor(timeout).until(ExpectedConditions.elementToBeClickable(locator));
  }

  /** Waits until no matching element is visible. */
  public boolean waitInvisible(By locator) {
    return waitFor(timeout).until(ExpectedConditions.invisibilityOfElementLocated(locator));
  }

  public void click(By locator) {
    waitClickable(locator).click();
  }

  /** Clears the field and types {@code text}. */
  public void type(By locator, CharSequence text) {
    WebElement element = waitVisible(locator);
    element.clear();
    element.sendKeys(text);
  }

  public String text(By locator) {
    return waitVisible(locator).getText();
  }

  /** Returns immediately (no waiting) whether a matching element is currently displayed. */
  public boolean isDisplayed(By locator) {
    List<WebElement> elements = driver.findElements(locator);
    for (WebElement element : elements) {
      try {
        if (element.isDisplayed()) {
          return true;
        }
      } catch (RuntimeException ignored) {
        // Element went stale between lookup and check; treat as not displayed.
      }
    }
    return false;
  }

  /** Waits up to {@code maxWait} for the element to become visible; never throws on timeout. */
  public boolean isDisplayedWithin(By locator, Duration maxWait) {
    try {
      waitFor(maxWait).until(ExpectedConditions.visibilityOfElementLocated(locator));
      return true;
    } catch (org.openqa.selenium.TimeoutException e) {
      return false;
    }
  }

  public WebDriverWait waitFor(Duration duration) {
    return new WebDriverWait(driver, duration);
  }

  public Duration timeout() {
    return timeout;
  }
}
