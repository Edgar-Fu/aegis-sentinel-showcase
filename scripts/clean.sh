#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
rm -rf "$ROOT/build"
find "$ROOT/runtime/evidence" -mindepth 1 ! -name '.gitkeep' -exec rm -rf {} + 2>/dev/null || true
echo "Removed generated build and runtime evidence."
