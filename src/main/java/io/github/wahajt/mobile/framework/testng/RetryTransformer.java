package io.github.wahajt.mobile.framework.testng;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/** TestNG listener that attaches {@link RetryAnalyzer} to every test method. */
public class RetryTransformer implements IAnnotationTransformer {
  @Override
  @SuppressWarnings("rawtypes")
  public void transform(
      ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
    annotation.setRetryAnalyzer(RetryAnalyzer.class);
  }
}
