#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "Downloading the official Gradle 8.9 wrapper JAR..."
  if command -v curl >/dev/null 2>&1; then
    curl -L --fail -o "$JAR" https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$JAR" https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar
  else
    echo "Install curl/wget or run: gradle wrapper --gradle-version 8.9" >&2
    exit 1
  fi
fi
exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
