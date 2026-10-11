#!/usr/bin/env bash
# Shared helpers for scripts/*.sh. Compatible with bash 3.2 (macOS default).

if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  C_OK=$'\033[32m'; C_WARN=$'\033[33m'; C_ERR=$'\033[31m'; C_DIM=$'\033[2m'; C_OFF=$'\033[0m'
else
  C_OK=""; C_WARN=""; C_ERR=""; C_DIM=""; C_OFF=""
fi

ok()   { printf '  %s✔%s %s\n' "$C_OK" "$C_OFF" "$*"; }
warn() { printf '  %s!%s %s\n' "$C_WARN" "$C_OFF" "$*"; }
fail() { printf '  %s✘%s %s\n' "$C_ERR" "$C_OFF" "$*"; }
info() { printf '    %s%s%s\n' "$C_DIM" "$*" "$C_OFF"; }

have() { command -v "$1" >/dev/null 2>&1; }

# read_setting ENV_NAME DEFAULT
# Resolves a setting the same way the framework does for env + .env: environment variable,
# then the .env file (or $CONFIG_FILE), then DEFAULT. (-D system properties are Java-only.)
read_setting() {
  local name="$1" default="$2" value file
  value="$(printenv "$name" 2>/dev/null || true)"
  if [ -z "$value" ]; then
    file="${CONFIG_FILE:-.env}"
    if [ -f "$file" ]; then
      value="$(grep -E "^[[:space:]]*(export[[:space:]]+)?${name}=" "$file" | tail -n 1 \
        | sed -E "s/^[[:space:]]*(export[[:space:]]+)?${name}=//; s/[[:space:]]+#.*$//; s/^[\"'](.*)[\"']$/\1/")"
    fi
  fi
  if [ -z "$value" ]; then value="$default"; fi
  printf '%s' "$value"
}

# is_local_url URL -> success when the host is this machine.
is_local_url() {
  case "$1" in
    *://localhost*|*://127.0.0.1*|*://0.0.0.0*|*://\[::1\]*) return 0 ;;
    *) return 1 ;;
  esac
}

# appium_status_ok URL -> success when GET URL/status answers.
appium_status_ok() {
  local url="${1%/}"
  have curl || return 2
  curl -fsS --max-time 5 "$url/status" >/dev/null 2>&1
}
