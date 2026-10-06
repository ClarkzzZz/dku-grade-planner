#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$PROJECT_ROOT/scripts/build.sh"
exec java -jar "$PROJECT_ROOT/build/dku-grade-planner.jar"
