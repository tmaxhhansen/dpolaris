#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"
OUT_DIR="$ROOT_DIR/build/out"

mkdir -p "$OUT_DIR"

find "$OUT_DIR" -type f -name '*.class' -delete

javac -encoding UTF-8 -d "$OUT_DIR" $(find "$SRC_DIR" -name '*.java')
java -cp "$OUT_DIR" com.dpolaris.javaapp.DPolarisJavaApp

