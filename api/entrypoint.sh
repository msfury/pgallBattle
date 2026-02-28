#!/bin/sh
# /data/pgallbattle.db가 없으면 기본 DB 파일 복사
if [ ! -f "$SQLITE_PATH" ]; then
  echo "DB not found at $SQLITE_PATH, copying default..."
  cp /app/pgallbattle.db.default "$SQLITE_PATH"
fi
exec java --enable-native-access=ALL-UNNAMED -jar /app/app.jar
