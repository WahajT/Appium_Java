package io.github.wahajt.mobile.framework;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import io.github.wahajt.mobile.framework.config.ConfigurationException;
import io.github.wahajt.mobile.framework.config.Platform;
import io.github.wahajt.mobile.framework.data.TestData;
import io.github.wahajt.mobile.framework.driver.DriverManager;
import io.github.wahajt.mobile.framework.reporting.Artifacts;
import org.testng.annotations.Test;

public class UtilitiesTest {

  @Test
  public void randomDigitsHaveRequestedShape() {
    for (int i = 0; i < 500; i++) {
      String digits = TestData.randomDigits(10);
      assertEquals(digits.length(), 10);
      assertTrue(digits.matches("[1-9][0-9]{9}"), digits);
      assertFalse(digits.matches(".*(\\d)\\1\\1\\1.*"), "four repeated digits in " + digits);
    }
  }

  @Test
  public void randomEmailUsesReservedDomain() {
    assertTrue(TestData.randomEmail().matches("test-[a-z0-9]{10}@example\\.com"));
  }

  @Test
  public void randomIntIsInclusive() {
    for (int i = 0; i < 200; i++) {
      int value = TestData.randomInt(1, 3);
      assertTrue(value >= 1 && value <= 3);
    }
  }

  @Test
  public void safeFileNameStripsUnsafeCharacters() {
    assertEquals(Artifacts.safeFileName("Login: user/pass <ok>?"), "Login_user_pass_ok");
    assertEquals(Artifacts.safeFileName("///"), "artifact");
    assertEquals(Artifacts.safeFileName("x".repeat(300)).length(), 100);
  }

  @Test
  public void platformParsingIsCaseInsensitive() {
    assertEquals(Platform.from(" Android "), Platform.ANDROID);
    assertEquals(Platform.from("IOS"), Platform.IOS);
    assertThrows(ConfigurationException.class, () -> Platform.from(null));
  }

  @Test
  public void driverManagerFailsClearlyWithoutSession() {
    assertFalse(DriverManager.hasDriver());
    DriverManager.quit(); // must be a no-op
    IllegalStateException e = expectThrows(IllegalStateException.class, DriverManager::driver);
    assertTrue(e.getMessage().contains("No Appium session"));
  }
}
