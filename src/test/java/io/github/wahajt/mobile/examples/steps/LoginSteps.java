package io.github.wahajt.mobile.examples.steps;

import static org.testng.Assert.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.wahajt.mobile.examples.pages.HomePage;
import io.github.wahajt.mobile.examples.pages.LoginPage;
import io.github.wahajt.mobile.framework.driver.DriverManager;

/**
 * TEMPLATE step definitions for {@code features/templates/login.feature}.
 *
 * <p>Credentials are read from the environment ({@code TEST_USERNAME} / {@code TEST_PASSWORD}) so
 * they never live in feature files or source control.
 */
public class LoginSteps {
  private HomePage homePage;

  @Given("the sign-in screen is displayed")
  public void signInScreenDisplayed() {
    assertTrue(new LoginPage(DriverManager.driver()).isDisplayed(), "Sign-in screen not shown");
  }

  @When("I sign in with the test account")
  public void signInWithTestAccount() {
    homePage =
        new LoginPage(DriverManager.driver())
            .loginAs(requiredEnv("TEST_USERNAME"), requiredEnv("TEST_PASSWORD"));
  }

  @Then("the home screen is displayed")
  public void homeScreenDisplayed() {
    assertTrue(homePage.isDisplayed(), "Home screen not shown after sign-in");
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Set the " + name + " environment variable");
    }
    return value;
  }
}
