# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.1.0] - 2026-10-11

### Added
- Layered configuration (`-D` property → environment variable → `.env` → default) covering
  Appium server, Android (UiAutomator2) and iOS (XCUITest) devices, apps, timeouts, signing,
  reporting and retries. Extra capabilities can be supplied through a JSON file.
- `CapabilitiesFactory` built on `UiAutomator2Options` / `XCUITestOptions`, with per-platform
  validation and clear error messages.
- Thread-safe `DriverManager`, plus optional local Appium server management.
- `BasePage`, `Locator`, `ElementActions` (explicit waits), `Gestures` (W3C actions),
  `DeviceActions`, and an Android ANR-dialog watcher.
- Cucumber hooks with failure screenshots and optional screen recording, attached to the Cucumber
  and Allure reports.
- Opt-in TestNG retry (`RETRY_COUNT`).
- App-agnostic smoke feature, plus a template login page object, steps and feature.
- Unit tests for the framework that need no device.
- Makefile (`help`, `doctor`, `setup`, `build`, `test`, `test-android`, `test-ios`,
  `check-features`, `lint`, `format`, `clean`, `report`, `report-serve`, `wait-emulator`), Maven
  wrapper, and GitHub Actions CI on JDK 17 and 21.
- Documentation: README, configuration reference, Makefile guide, contributing guide, security
  policy, code of conduct.
