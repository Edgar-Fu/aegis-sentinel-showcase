#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
"$ROOT/scripts/build.sh"
rm -rf "$ROOT/build/test-classes"
mkdir -p "$ROOT/build/test-classes"
find "$ROOT/src/test/java" -name '*.java' -print0 \
  | xargs -0 javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 \
      -cp "$ROOT/build/classes" -d "$ROOT/build/test-classes"
cd "$ROOT"
java --add-modules jdk.httpserver -Daegis.root="$ROOT" \
  -cp "$ROOT/build/classes:$ROOT/build/test-classes" com.aegis.showcase.ShowcaseSelfTest
