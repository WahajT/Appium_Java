# TEMPLATE: excluded by default via the "not @template" tag filter.
# 1. Replace the placeholder accessibility ids in examples/pages/LoginPage.java and HomePage.java.
# 2. Export TEST_USERNAME and TEST_PASSWORD (never commit them).
# 3. Run: make test-android TAGS="@login"
@template @login
Feature: Sign in

  Scenario: A registered user can sign in
    Given the sign-in screen is displayed
    When I sign in with the test account
    Then the home screen is displayed
