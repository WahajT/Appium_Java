#!/usr/bin/env bash
# Fast checks before an end-to-end run so that missing prerequisites produce a clear message
# instead of a long Java stack trace. Usage: scripts/preflight.sh android|ios
# Set SKIP_PREFLIGHT=1 to bypass (e.g. for cloud providers with custom endpoints).
set -u
cd "$(dirname "$0")/.." || exit 1
. scripts/lib.sh

platform="${1:-}"
case "$platform" in
  android|ios) ;;
  *) echo "usage: $0 android|ios" >&2; exit 2 ;;
esac

if [ "${SKIP_PREFLIGHT:-0}" = "1" ]; then
  echo "Preflight skipped (SKIP_PREFLIGHT=1)"
  exit 0
fi

errors=0
url="$(read_setting APPIUM_SERVER_URL http://127.0.0.1:4723)"
autostart="$(read_setting APPIUM_SERVER_START false)"

echo "Preflight for $platform (Appium: $(printf '%s' "$url" | sed -E 's#(://)[^/@]+@#\1****@#'))"

if [ "$autostart" = "true" ]; then
  if have appium; then ok "appium installed; the framework will start it (APPIUM_SERVER_START=true)"
  else fail "APPIUM_SERVER_START=true but 'appium' is not installed (npm install -g appium)"; errors=$((errors + 1)); fi
else
  appium_status_ok "$url"
  case $? in
    0) ok "Appium server reachable" ;;
    2) warn "curl not found; skipping server check" ;;
    *) fail "No Appium server at $url. Start one in another terminal with 'appium', or set APPIUM_SERVER_START=true."
       errors=$((errors + 1)) ;;
  esac
fi

app_var="ANDROID_APP"; id_var="ANDROID_APP_PACKAGE"
[ "$platform" = "ios" ] && { app_var="IOS_APP"; id_var="IOS_BUNDLE_ID"; }
app="$(read_setting "$app_var" "")"
app_id="$(read_setting "$id_var" "")"
if [ -z "$app" ] && [ -z "$app_id" ]; then
  fail "Set $app_var (app file) and/or $id_var (installed app) in .env - see docs/CONFIGURATION.md"
  errors=$((errors + 1))
elif [ -n "$app" ]; then
  case "$app" in
    *://*) ok "$app_var is a URL" ;;
    *) if [ -e "$app" ]; then ok "$app_var found: $app"; else fail "$app_var does not exist: $app"; errors=$((errors + 1)); fi ;;
  esac
else
  ok "$id_var=$app_id (app must already be installed)"
fi

if is_local_url "$url"; then
  if [ "$platform" = "android" ]; then
    avd="$(read_setting ANDROID_AVD "")"
    if [ -n "$avd" ]; then
      ok "ANDROID_AVD=$avd (Appium will boot it if needed)"
    elif have adb; then
      count="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')"
      if [ "$count" -gt 0 ]; then ok "$count Android device(s) online"
      else fail "No Android device online. Start an emulator (emulator -avd <name>) or connect a device, or set ANDROID_AVD."; errors=$((errors + 1)); fi
    else
      fail "adb not found; install Android platform-tools and add them to PATH"; errors=$((errors + 1))
    fi
  else
    if [ "$(uname -s)" != "Darwin" ]; then
      fail "iOS tests need macOS + Xcode for a local Appium server. Run on a Mac, or point APPIUM_SERVER_URL at a remote Mac/cloud."
      errors=$((errors + 1))
    elif ! have xcodebuild; then
      fail "Xcode not found (xcode-select --install and install Xcode from the App Store)"; errors=$((errors + 1))
    else
      ok "macOS with Xcode detected"
      if [ -z "$(read_setting IOS_UDID "")" ] && [ -z "$(read_setting IOS_DEVICE_NAME "")" ]; then
        fail "Set IOS_DEVICE_NAME (simulator) or IOS_UDID (simulator/real device)"; errors=$((errors + 1))
      fi
    fi
  fi
else
  info "Remote Appium server: skipping local device checks"
fi

if [ "$errors" -gt 0 ]; then
  echo
  fail "Preflight failed ($errors problem(s)). Fix the above, run 'make doctor', or bypass with SKIP_PREFLIGHT=1."
  exit 1
fi
