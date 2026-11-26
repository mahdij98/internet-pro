#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/.." && pwd)"

: "${TAPSELL_APP_KEY:?Set TAPSELL_APP_KEY from your Tapsell dashboard}"
: "${TAPSELL_ZONE_ID:?Set TAPSELL_ZONE_ID for the standard banner}"
: "${INTERNETPRO_KEYSTORE:?Set INTERNETPRO_KEYSTORE to the app signing keystore}"
: "${INTERNETPRO_STORE_PASSWORD:?Set INTERNETPRO_STORE_PASSWORD}"
: "${INTERNETPRO_KEY_ALIAS:?Set INTERNETPRO_KEY_ALIAS}"
: "${INTERNETPRO_KEY_PASSWORD:?Set INTERNETPRO_KEY_PASSWORD}"

cd "$HERE"
./gradlew --no-daemon --max-workers=1 :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk "$REPO/releases/InternetPro-latest.apk"
printf 'Built APK: %s\n' "$REPO/releases/InternetPro-latest.apk"
