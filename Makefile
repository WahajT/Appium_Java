# Convenience interface over Maven. Maven (via ./mvnw) still owns dependencies, compilation and
# test execution; every target below prints or runs a plain Maven command you can use directly.
# Works with GNU Make 3.81+ (macOS default) and bash 3.2+.

SHELL := /bin/bash
.DEFAULT_GOAL := help

# ---- Overridable variables (make VAR=value target, or export them) --------------------------
MVN        ?= ./mvnw
MVN_FLAGS  ?= -B
MVN_ARGS   ?=
TAGS       ?=
ANDROID_TAGS ?= not @template and not @ios
IOS_TAGS     ?= not @template and not @android
E2E_SUITE  ?= src/test/resources/testng-e2e.xml
SKIP_PREFLIGHT ?= 0
EMULATOR_TIMEOUT ?= 300
export SKIP_PREFLIGHT

ALLURE_RESULTS := target/allure-results
ALLURE_REPORT  := target/site/allure-maven-plugin/index.html

.PHONY: help setup doctor build test test-android test-ios check-features lint format clean \
        report report-serve wait-emulator

help: ## List available targets and variables
	@echo "Usage: make <target> [VAR=value]"
	@echo
	@echo "Targets:"
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  %-16s %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo
	@echo "Variables:"
	@echo "  TAGS            Cucumber tag expression; replaces the platform default (e.g. TAGS=\"@smoke\")"
	@echo "  ANDROID_TAGS    Default tags for test-android  [$(ANDROID_TAGS)]"
	@echo "  IOS_TAGS        Default tags for test-ios      [$(IOS_TAGS)]"
	@echo "  MVN_ARGS        Extra Maven arguments, e.g. MVN_ARGS=\"-Dandroid.udid=emulator-5554\""
	@echo "  SKIP_PREFLIGHT  1 = skip device/server checks before e2e runs [$(SKIP_PREFLIGHT)]"
	@echo "  MVN             Maven command [$(MVN)]"
	@echo
	@echo "App/device settings come from .env or environment variables - see docs/CONFIGURATION.md"

setup: ## Create .env from the template (if missing) and download dependencies
	@if [ -f .env ]; then echo ".env already exists - left untouched"; \
	else cp .env.example .env && echo "Created .env from .env.example - edit it for your app/device"; fi
	@chmod +x mvnw scripts/*.sh
	@./scripts/doctor.sh >/dev/null || { echo "Missing build prerequisites - run 'make doctor' for details"; exit 1; }
	$(MVN) $(MVN_FLAGS) -DskipTests test-compile
	@echo "Setup complete. Next: edit .env, start Appium, then run 'make test-android' or 'make test-ios'."

doctor: ## Check installed tools (JDK, Appium, drivers, Android SDK, Xcode) and report gaps
	@./scripts/doctor.sh

build: ## Compile and package the framework jar (no tests)
	$(MVN) $(MVN_FLAGS) -DskipTests package $(MVN_ARGS)

test: ## Run framework unit tests (no device or Appium server needed)
	$(MVN) $(MVN_FLAGS) test $(MVN_ARGS)

test-android: ## Run Cucumber scenarios on Android (needs Appium + emulator/device)
	@./scripts/preflight.sh android
	$(MVN) $(MVN_FLAGS) test -Dtestng.suite=$(E2E_SUITE) -Dplatform=android \
		"-Dcucumber.filter.tags=$(if $(TAGS),$(TAGS),$(ANDROID_TAGS))" $(MVN_ARGS)

test-ios: ## Run Cucumber scenarios on iOS (needs Appium on macOS + simulator/device)
	@./scripts/preflight.sh ios
	$(MVN) $(MVN_FLAGS) test -Dtestng.suite=$(E2E_SUITE) -Dplatform=ios \
		"-Dcucumber.filter.tags=$(if $(TAGS),$(TAGS),$(IOS_TAGS))" $(MVN_ARGS)

check-features: ## Dry-run every feature file to verify all steps have definitions (no device)
	$(MVN) $(MVN_FLAGS) test -Dtestng.suite=$(E2E_SUITE) -Dcucumber.execution.dry-run=true \
		"-Dcucumber.filter.tags=$(if $(TAGS),$(TAGS),@template or not @template)" $(MVN_ARGS)

lint: ## Check formatting (google-java-format) and fail on compiler warnings
	$(MVN) $(MVN_FLAGS) spotless:check
	$(MVN) $(MVN_FLAGS) -Dmaven.compiler.failOnWarning=true clean test-compile
	@if command -v shellcheck >/dev/null 2>&1; then shellcheck -x scripts/*.sh && echo "shellcheck: OK"; \
	else echo "shellcheck not installed - skipped shell script linting"; fi

format: ## Reformat Java sources in place (google-java-format)
	$(MVN) $(MVN_FLAGS) spotless:apply

clean: ## Remove build output, reports, screenshots and videos (target/ only)
	$(MVN) $(MVN_FLAGS) -q clean
	@echo "Removed target/"

report: ## Generate the Allure HTML report from the last e2e run
	@if [ ! -d "$(ALLURE_RESULTS)" ] || [ -z "$$(ls -A $(ALLURE_RESULTS) 2>/dev/null)" ]; then \
		echo "No Allure results in $(ALLURE_RESULTS). Run 'make test-android' or 'make test-ios' first."; exit 1; fi
	$(MVN) $(MVN_FLAGS) allure:report
	@echo "Allure report: $(ALLURE_REPORT)"
	@echo "Cucumber report: target/cucumber-reports/cucumber.html"

report-serve: ## Generate the Allure report and open it in a browser (Ctrl+C to stop)
	@if [ ! -d "$(ALLURE_RESULTS)" ]; then \
		echo "No Allure results in $(ALLURE_RESULTS). Run 'make test-android' or 'make test-ios' first."; exit 1; fi
	$(MVN) $(MVN_FLAGS) allure:serve

wait-emulator: ## Block until an Android emulator/device has booted (EMULATOR_TIMEOUT seconds)
	@./scripts/wait-for-emulator.sh $(EMULATOR_TIMEOUT)
