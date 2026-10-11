#!/usr/bin/env bash
# Reports which prerequisites are installed. Exits non-zero only when the Java toolchain needed to
# build the project is missing; Android/iOS tooling is reported as warnings because you may only
# need one platform (or a remote Appium server).
set -u
cd "$(dirname "$0")/.." || exit 1
. scripts/lib.sh

errors=0

echo "Build toolchain"
# ./mvnw uses $JAVA_HOME when set, otherwise the java on PATH; check the same one.
java_cmd=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then java_cmd="$JAVA_HOME/bin/java"
elif have java; then java_cmd="java"; fi
if [ -n "$java_cmd" ]; then
  java_version="$("$java_cmd" -version 2>&1 | head -n 1)"
  major="$(printf '%s' "$java_version" | sed -E 's/.*version "([0-9]+).*/\1/')"
  if [ "${major:-0}" -ge 17 ] 2>/dev/null; then
    ok "Java: $java_version"
  else
    fail "Java 17+ required, found: $java_version"; errors=$((errors + 1))
  fi
else
  fail "java not found. Install a JDK 17+ (e.g. Temurin) and set JAVA_HOME."; errors=$((errors + 1))
fi
javac_cmd=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then javac_cmd="$JAVA_HOME/bin/javac"
elif have javac; then javac_cmd="javac"; fi
if [ -n "$javac_cmd" ]; then
  ok "javac: $("$javac_cmd" -version 2>&1)"
else
  fail "javac not found: a JDK (not just a JRE) is required. Install a full JDK and set JAVA_HOME."
  errors=$((errors + 1))
fi
if [ -x ./mvnw ]; then
  ok "Maven wrapper: ./mvnw (downloads Maven on first use)"
else
  fail "./mvnw missing or not executable (chmod +x mvnw)"; errors=$((errors + 1))
fi

echo
echo "Appium"
if have node; then ok "Node.js: $(node --version)"; else warn "Node.js not found (needed to install/run Appium locally)"; fi
if have appium; then
  ok "Appium: $(appium --version 2>/dev/null | tail -n 1)"
  drivers="$(appium driver list --installed 2>&1 || true)"
  for d in uiautomator2 xcuitest; do
    if printf '%s' "$drivers" | grep -qi "$d"; then ok "Appium driver: $d"; else warn "Appium driver '$d' not installed (appium driver install $d)"; fi
  done
else
  warn "appium not found (npm install -g appium) - fine if you use a remote Appium server"
fi
server_url="$(read_setting APPIUM_SERVER_URL http://127.0.0.1:4723)"
appium_status_ok "$server_url"
case $? in
  0) ok "Appium server reachable at $server_url" ;;
  2) warn "curl not found; cannot check $server_url" ;;
  *) warn "No Appium server answering at $server_url (start one with 'appium' or set APPIUM_SERVER_START=true)" ;;
esac

echo
echo "Android"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -n "$sdk" ]; then ok "ANDROID_HOME: $sdk"; else warn "ANDROID_HOME / ANDROID_SDK_ROOT not set"; fi
if have adb; then
  count="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')"
  ok "adb: $(adb version 2>/dev/null | head -n 1)"
  if [ "$count" -gt 0 ]; then ok "Android devices online: $count"; else warn "No Android device/emulator online (adb devices)"; fi
else
  warn "adb not found (add \$ANDROID_HOME/platform-tools to PATH)"
fi
if have emulator; then ok "emulator: $(emulator -list-avds 2>/dev/null | wc -l | tr -d ' ') AVD(s)"; else warn "emulator not on PATH (add \$ANDROID_HOME/emulator)"; fi

echo
echo "iOS"
if [ "$(uname -s)" = "Darwin" ]; then
  if have xcodebuild; then ok "$(xcodebuild -version 2>/dev/null | head -n 1)"; else warn "Xcode not found"; fi
  if have xcrun; then ok "Simulators available: $(xcrun simctl list devices available 2>/dev/null | grep -c '(')"; fi
else
  info "Skipped: iOS automation needs macOS with Xcode for the Appium server ($(uname -s) detected)."
fi

echo
echo "Project"
if [ -f .env ]; then ok ".env present"; else warn ".env missing (run: make setup, or cp .env.example .env)"; fi
if have allure; then ok "Allure CLI: $(allure --version 2>/dev/null)"; else info "Allure CLI not installed - not required, 'make report' downloads it via Maven"; fi

echo
if [ "$errors" -gt 0 ]; then
  fail "$errors required prerequisite(s) missing"
  exit 1
fi
ok "Build prerequisites satisfied (warnings above only matter for the platform you test)"
