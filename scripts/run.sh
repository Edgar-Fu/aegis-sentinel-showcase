#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
"$ROOT/scripts/build.sh"
cd "$ROOT"
echo "Open http://localhost:${PORT:-8080}"
exec java --add-modules jdk.httpserver -Daegis.root="$ROOT" \
  -jar "$ROOT/build/aegis-sentinel-public-showcase.jar"
