#!/bin/sh
set -e

GRADLE_VERSION=8.7
DIST="$HOME/.gradle/jarvis-gradle/gradle-$GRADLE_VERSION"

if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$HOME/.gradle/jarvis-gradle"
  curl -L --fail -o "$HOME/.gradle/jarvis-gradle/gradle.zip" \
    "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  unzip -q "$HOME/.gradle/jarvis-gradle/gradle.zip" \
    -d "$HOME/.gradle/jarvis-gradle"
  rm "$HOME/.gradle/jarvis-gradle/gradle.zip"
fi

exec "$DIST/bin/gradle" "$@"
