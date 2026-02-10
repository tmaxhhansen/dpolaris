#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
INPUT_DIR="$BUILD_DIR/package-input"
DMG_OUTPUT_DIR="$BUILD_DIR/macos-dmg"
APP_NAME="dPolarisJava"
MAIN_JAR="dpolaris-java-app.jar"

# Build fresh jar first.
bash "$ROOT_DIR/build.sh"

rm -rf "$INPUT_DIR" "$DMG_OUTPUT_DIR"
mkdir -p "$INPUT_DIR" "$DMG_OUTPUT_DIR"

cp "$BUILD_DIR/$MAIN_JAR" "$INPUT_DIR/$MAIN_JAR"

jpackage \
  --type dmg \
  --name "$APP_NAME" \
  --dest "$DMG_OUTPUT_DIR" \
  --input "$INPUT_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class com.dpolaris.javaapp.DPolarisJavaApp \
  --vendor "dPolaris" \
  --app-version "1.0.0"

echo
echo "Created macOS installer:"
echo "  $DMG_OUTPUT_DIR/$APP_NAME-1.0.0.dmg"
