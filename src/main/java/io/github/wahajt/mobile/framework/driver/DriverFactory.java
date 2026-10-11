package io.github.wahajt.mobile.framework.driver;

import io.appium.java_client.AppiumClientConfig;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.options.BaseOptions;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import io.github.wahajt.mobile.framework.config.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates a new Appium session for the configured platform. */
public final class DriverFactory {
  private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);

  private DriverFactory() {}

  public static AppiumDriver create(FrameworkConfig config) {
    BaseOptions<?> options = CapabilitiesFactory.forConfig(config);
    AppiumClientConfig clientConfig =
        AppiumClientConfig.defaultConfig()
            .baseUrl(config.appiumServerUrl())
            .readTimeout(config.getSeconds(Setting.TIMEOUT_SERVER_READ));

    LOG.info(
        "Starting {} session on {} (capability names: {})",
        config.platform(),
        config.describe().get(Setting.APPIUM_SERVER_URL.envVar()),
        options.asMap().keySet());

    switch (config.platform()) {
      case ANDROID:
        return new AndroidDriver(clientConfig, options);
      case IOS:
        return new IOSDriver(clientConfig, options);
      default:
        throw new IllegalStateException("Unsupported platform " + config.platform());
    }
  }
}
