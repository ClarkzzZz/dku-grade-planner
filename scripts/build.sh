#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"
mkdir -p build/classes
find build/classes -name '*.class' -delete
find src/main/java -name '*.java' -print > build/sources.txt
javac --release 21 -encoding UTF-8 -d build/classes @build/sources.txt
jar --create --file build/dku-grade-planner.jar --main-class edu.dku.gradeplanner.App -C build/classes .
echo 'Build complete: build/dku-grade-planner.jar (Java 21 compatible)'
