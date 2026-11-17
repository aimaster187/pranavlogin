#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android/sdk}"
ANDROID_JAR="${SDK_ROOT}/platforms/android-36/android.jar"
D8_BIN="${SDK_ROOT}/build-tools/36.0.0/d8"
BUILD_DIR="${ROOT_DIR}/build"
CLASSES_DIR="${BUILD_DIR}/classes"

if [[ ! -f "$ANDROID_JAR" ]]; then
  echo "Missing android.jar at $ANDROID_JAR" >&2
  exit 1
fi
if [[ ! -x "$D8_BIN" ]]; then
  echo "Missing d8 tool at $D8_BIN" >&2
  exit 1
fi

rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR" "$BUILD_DIR"

javac --release 11 -cp "$ANDROID_JAR" -d "$CLASSES_DIR" \
  $(find "${ROOT_DIR}/src" -name "*.java")

"$D8_BIN" --min-api 24 --output "$BUILD_DIR" \
  $(find "$CLASSES_DIR" -name "*.class")

mv "${BUILD_DIR}/classes.dex" "${BUILD_DIR}/pranav-sdk-1.4.0.dex"
SHA256="$(sha256sum "${BUILD_DIR}/pranav-sdk-1.4.0.dex" | awk '{print $1}')"
echo "$SHA256" > "${BUILD_DIR}/pranav-sdk-1.4.0.dex.sha256"
echo "Built ${BUILD_DIR}/pranav-sdk-1.4.0.dex (SHA-256: $SHA256)"
