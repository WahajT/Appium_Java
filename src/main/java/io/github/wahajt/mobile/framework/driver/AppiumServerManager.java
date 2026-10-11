package io.github.wahajt.mobile.framework.driver;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import io.github.wahajt.mobile.framework.config.Setting;
import java.io.File;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optionally starts a local Appium server for the test run when {@code APPIUM_SERVER_START=true}.
 *
 * <p>The server listens on the host, port and base path of {@code APPIUM_SERVER_URL}. Appium must
 * be installed (for example {@code npm install -g appium}); set {@code APPIUM_SERVER_MAIN_JS} when
 * it cannot be located automatically.
 */
public final class AppiumServerManager {
  private static final Logger LOG = LoggerFactory.getLogger(AppiumServerManager.class);
  private static AppiumDriverLocalService service;

  private AppiumServerManager() {}

  public static synchronized void startIfConfigured(FrameworkConfig config) {
    if (!config.getBoolean(Setting.APPIUM_SERVER_START) || service != null) {
      return;
    }
    URL url = config.appiumServerUrl();
    AppiumServiceBuilder builder =
        new AppiumServiceBuilder()
            .withIPAddress(url.getHost())
            .usingPort(url.getPort() > 0 ? url.getPort() : 4723)
            .withLogFile(new File(config.require(Setting.APPIUM_SERVER_LOG)));
    String basePath = url.getPath();
    if (basePath != null && !basePath.isEmpty() && !"/".equals(basePath)) {
      builder.withArgument(GeneralServerFlag.BASEPATH, basePath);
    }
    config.find(Setting.APPIUM_SERVER_MAIN_JS).ifPresent(js -> builder.withAppiumJS(new File(js)));

    service = AppiumDriverLocalService.buildService(builder);
    LOG.info("Starting local Appium server at {}", url);
    service.start();
  }

  public static synchronized void stop() {
    if (service != null) {
      try {
        if (service.isRunning()) {
          service.stop();
        }
      } finally {
        service = null;
      }
    }
  }
}
