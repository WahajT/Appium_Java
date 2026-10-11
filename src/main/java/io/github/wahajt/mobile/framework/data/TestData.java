package io.github.wahajt.mobile.framework.data;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Random test-data generators. Generated emails use the reserved {@code example.com} domain. */
public final class TestData {
  private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";

  private TestData() {}

  /** Uppercase letters, e.g. {@code QWHTZ}. */
  public static String randomLetters(int length) {
    return random(LETTERS, length);
  }

  /** Lowercase letters and digits. */
  public static String randomAlphanumeric(int length) {
    return random(ALPHANUMERIC, length);
  }

  /**
   * Digits only, never starting with {@code 0} and never containing four identical digits in a row
   * (many apps reject such values as phone numbers or PINs).
   */
  public static String randomDigits(int length) {
    if (length < 1) {
      throw new IllegalArgumentException("length must be positive");
    }
    ThreadLocalRandom random = ThreadLocalRandom.current();
    String value;
    do {
      StringBuilder digits = new StringBuilder(length);
      digits.append(1 + random.nextInt(9));
      for (int i = 1; i < length; i++) {
        digits.append(random.nextInt(10));
      }
      value = digits.toString();
    } while (hasRepeatedRun(value, 4));
    return value;
  }

  public static String randomEmail() {
    return randomEmail("example.com");
  }

  public static String randomEmail(String domain) {
    return "test-" + randomAlphanumeric(10) + "@" + domain.toLowerCase(Locale.ROOT);
  }

  /** Random integer in {@code [minInclusive, maxInclusive]}. */
  public static int randomInt(int minInclusive, int maxInclusive) {
    return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
  }

  static boolean hasRepeatedRun(String value, int runLength) {
    int run = 1;
    for (int i = 1; i < value.length(); i++) {
      run = value.charAt(i) == value.charAt(i - 1) ? run + 1 : 1;
      if (run >= runLength) {
        return true;
      }
    }
    return false;
  }

  private static String random(String alphabet, int length) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    StringBuilder out = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      out.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return out.toString();
  }
}
