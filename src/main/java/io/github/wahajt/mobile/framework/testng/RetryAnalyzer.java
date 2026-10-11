package io.github.wahajt.mobile.framework.testng;

import io.github.wahajt.mobile.framework.config.FrameworkConfig;
import io.github.wahajt.mobile.framework.config.Setting;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed test up to {@code RETRY_COUNT} times (default 0 = no retries). Enable it for
 * every test with {@link RetryTransformer}.
 */
public class RetryAnalyzer implements IRetryAnalyzer {
  private final AtomicInteger attempts = new AtomicInteger();

  @Override
  public boolean retry(ITestResult result) {
    int limit = FrameworkConfig.get().getInt(Setting.RETRY_COUNT);
    return attempts.getAndIncrement() < limit;
  }
}
