#!/usr/bin/env bash
# Blocks until an Android emulator/device reports boot completed, or fails after a timeout.
# Usage: scripts/wait-for-emulator.sh [timeout_seconds]   (default 300; serial via ANDROID_SERIAL)
set -u
timeout="${1:-300}"
interval=5
elapsed=0

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found; add \$ANDROID_HOME/platform-tools to PATH" >&2
  exit 1
fi

adb wait-for-device >/dev/null 2>&1 &
wait_pid=$!
while kill -0 "$wait_pid" 2>/dev/null; do
  if [ "$elapsed" -ge "$timeout" ]; then
    kill "$wait_pid" 2>/dev/null
    echo "No device appeared within ${timeout}s" >&2
    exit 1
  fi
  sleep "$interval"; elapsed=$((elapsed + interval))
done

until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  if [ "$elapsed" -ge "$timeout" ]; then
    echo "Device did not finish booting within ${timeout}s" >&2
    exit 1
  fi
  echo "Waiting for device to boot... (${elapsed}s)"
  sleep "$interval"; elapsed=$((elapsed + interval))
done
echo "Device booted after ~${elapsed}s"
