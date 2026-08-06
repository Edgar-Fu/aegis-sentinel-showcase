#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD="$ROOT/build"
rm -rf "$BUILD/classes"
mkdir -p "$BUILD/classes"
find "$ROOT/src/main/java" -name '*.java' -print0 \
  | xargs -0 javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d "$BUILD/classes"
jar --create --file "$BUILD/aegis-sentinel-public-showcase.jar" \
  --main-class com.aegis.showcase.AegisShowcaseApplication -C "$BUILD/classes" .
echo "Built $BUILD/aegis-sentinel-public-showcase.jar"
