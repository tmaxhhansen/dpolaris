#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"
OUT_DIR="$ROOT_DIR/build/out"
JAR_PATH="$ROOT_DIR/build/dpolaris-java-app.jar"

mkdir -p "$OUT_DIR"

find "$OUT_DIR" -type f -name '*.class' -delete

javac -encoding UTF-8 -d "$OUT_DIR" $(find "$SRC_DIR" -name '*.java')
jar --create --file "$JAR_PATH" --main-class com.dpolaris.javaapp.DPolarisJavaApp -C "$OUT_DIR" .

echo "Built: $JAR_PATH"

