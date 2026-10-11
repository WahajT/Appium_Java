package io.github.wahajt.mobile.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Runs the Cucumber features through TestNG.
 *
 * <p>Override the tag filter from the command line, e.g. {@code -Dcucumber.filter.tags="@smoke and
 * not @ios"}; {@code make test-android} and {@code make test-ios} do this for you.
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"io.github.wahajt.mobile.framework.cucumber", "io.github.wahajt.mobile.examples.steps"},
    tags = "not @template",
    plugin = {
      "pretty",
      "summary",
      "html:target/cucumber-reports/cucumber.html",
      "json:target/cucumber-reports/cucumber.json",
      "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
    })
public class CucumberTestRunner extends AbstractTestNGCucumberTests {}
