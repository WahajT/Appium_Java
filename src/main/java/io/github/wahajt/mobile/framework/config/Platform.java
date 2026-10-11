package io.github.wahajt.mobile.framework.config;

import java.util.Locale;

/** Mobile platforms supported by the framework. */
public enum Platform {
  ANDROID,
  IOS;

  public static Platform from(String value) {
    if (value != null) {
      switch (value.trim().toLowerCase(Locale.ROOT)) {
        case "android":
          return ANDROID;
        case "ios":
          return IOS;
        default:
          break;
      }
    }
    throw new ConfigurationException("PLATFORM must be 'android' or 'ios' but was '" + value + "'");
  }
}
