#!/bin/bash
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo '{"async": true, "asyncTimeout": 300000}'

cd "$CLAUDE_PROJECT_DIR"

# Generate gradlew if missing
if [ ! -f gradlew ]; then
  echo "Downloading gradlew..."
  curl -s -L "https://raw.githubusercontent.com/gradle/gradle/v8.6.0/gradlew" -o gradlew
  chmod +x gradlew
fi

# Download gradle-wrapper.jar if missing
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "Downloading gradle-wrapper.jar..."
  mkdir -p gradle/wrapper
  curl -s -L "https://github.com/gradle/gradle/raw/v8.6.0/gradle/wrapper/gradle-wrapper.jar" \
    -o gradle/wrapper/gradle-wrapper.jar
fi

# Warm up Gradle and download dependencies (compileDebugKotlin is sufficient to pull most deps)
echo "Warming up Gradle dependencies..."
./gradlew dependencies --configuration debugRuntimeClasspath --no-daemon -q 2>/dev/null || true
