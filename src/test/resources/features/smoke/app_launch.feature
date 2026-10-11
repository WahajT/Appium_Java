@smoke
Feature: Application launch
  App-agnostic checks that run against whichever application is configured
  (ANDROID_APP / ANDROID_APP_PACKAGE or IOS_APP / IOS_BUNDLE_ID). No app-specific locators.

  Scenario: The application starts and renders a screen
    Given the application is launched
    Then the application is running in the foreground
    And the current screen has visible content
    And I capture a screenshot named "launch-screen"

  Scenario: The application returns to the foreground after being backgrounded
    Given the application is launched
    When the application is sent to the background for 2 seconds
    Then the application is running in the foreground
