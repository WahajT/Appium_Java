package io.github.wahajt.mobile.framework.config;

/** Thrown when configuration is missing or invalid. Messages name the setting to fix. */
public class ConfigurationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
