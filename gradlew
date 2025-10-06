#!/usr/bin/env sh
BASE_DIR=$(cd "$(dirname "$0")" || exit 1; pwd)
WRAPPER_DIR="$BASE_DIR/gradle/wrapper"
if [ -f "$WRAPPER_DIR/gradle-wrapper.jar" ]; then
  exec java -jar "$WRAPPER_DIR/gradle-wrapper.jar" "$@"
else
  exec gradle "$@"
fi
