#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"
bash scripts/build.sh
mkdir -p build/test-classes
find src/test/java -name '*.java' -print > build/test-sources.txt
javac --release 21 -encoding UTF-8 -cp build/classes -d build/test-classes @build/test-sources.txt
java -Djava.awt.headless=true -cp build/classes:build/test-classes edu.dku.gradeplanner.GradePlannerTest
