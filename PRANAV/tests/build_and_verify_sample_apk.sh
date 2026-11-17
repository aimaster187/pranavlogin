#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SDK_DIR="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
BUILD_TOOLS="$(ls -d "$SDK_DIR"/build-tools/* 2>/dev/null | sort -V | tail -n1)"
PLATFORM_JAR="$(ls -d "$SDK_DIR"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -n1)"

if [ -z "$BUILD_TOOLS" ] || [ -z "$PLATFORM_JAR" ]; then
  echo "ERROR: Android SDK build-tools or android.jar not found under $SDK_DIR"
  exit 1
fi

OUT_DIR="$ROOT_DIR/tests/output/sample-apk"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/classes" "$OUT_DIR/dex" "$OUT_DIR/zip"

echo "[1/6] Building PRANAV Core DEX (classes2.dex payload)..."
bash "$ROOT_DIR/dex/build-dex.sh"

echo "[2/6] Compiling separate PRANAV Hook + Sample Target host Java sources..."
find "$ROOT_DIR/dex/src" "$ROOT_DIR/integration/application" "$ROOT_DIR/tests/sample-target-app/src" -name "*.java" > "$OUT_DIR/sources.txt"
javac --release 11 -cp "$PLATFORM_JAR" -d "$OUT_DIR/classes" @"$OUT_DIR/sources.txt"

echo "[3/6] Compiling host classes + Hook into classes.dex (multi-DEX slot 1)..."
"$BUILD_TOOLS/d8" --min-api 24 --lib "$PLATFORM_JAR" --output "$OUT_DIR/dex" \
  $(find "$OUT_DIR/classes/com/pranav/sampletarget" "$OUT_DIR/classes/com/pranav/integration" -name "*.class")

echo "[4/6] Packaging AndroidManifest.xml with aapt2 & attaching pranav-sdk as classes2.dex..."
"$BUILD_TOOLS/aapt2" link \
  -o "$OUT_DIR/unaligned.apk" \
  --manifest "$ROOT_DIR/tests/sample-target-app/AndroidManifest.xml" \
  -I "$PLATFORM_JAR"

mkdir -p "$OUT_DIR/zip"
cp "$OUT_DIR/dex/classes.dex" "$OUT_DIR/zip/classes.dex"
cp "$ROOT_DIR/dex/build/pranav-sdk-1.4.0.dex" "$OUT_DIR/zip/classes2.dex"
(cd "$OUT_DIR/zip" && python3 -c "
import zipfile
with zipfile.ZipFile('$OUT_DIR/unaligned.apk', 'a', zipfile.ZIP_DEFLATED) as zf:
    zf.write('classes.dex')
    zf.write('classes2.dex')
")

echo "[5/6] Aligning & signing authorized sample APK..."
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT_DIR/unaligned.apk" "$OUT_DIR/aligned.apk"

KEYSTORE="${ROOT_DIR}/../debug.keystore"
if [ ! -f "$KEYSTORE" ] && [ -f "${ROOT_DIR}/../debug.keystore.base64" ]; then
  base64 -d "${ROOT_DIR}/../debug.keystore.base64" > "$KEYSTORE"
fi

"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$OUT_DIR/sample-pranav-integrated.apk" \
  "$OUT_DIR/aligned.apk"

"$BUILD_TOOLS/apksigner" verify --verbose "$OUT_DIR/sample-pranav-integrated.apk"

echo "[6/6] Running PRANAV APK Inspector on integrated sample APK..."
node "$ROOT_DIR/integration/apk-assistant/pranav_apk_inspector.js" "$OUT_DIR/sample-pranav-integrated.apk"
