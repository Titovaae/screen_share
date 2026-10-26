#!/bin/sh
set -e
# Если экран хоста не проброшен (DISPLAY не задан), поднимаем виртуальный
if [ -z "$DISPLAY" ]; then
    Xvfb :99 -screen 0 "${SCREEN_RESOLUTION:-1280x720x24}" -nolisten tcp &
    export DISPLAY=:99
    sleep 1
fi
exec java $JAVA_OPTS -jar /app/app.jar
