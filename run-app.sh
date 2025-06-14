#!/bin/bash

echo "Вроде работает"

echo "С богом"
./mvnw clean package

if [ $? -ne 0 ]; then
    echo "Все фигня, давай по новой"
    exit 1
fi

echo "Ну вот 80 % вроде работает!"

# JVM options for Spark + JavaFX
JVM_OPTS="\
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.lang.invoke=ALL-UNNAMED \
--add-opens java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens java.base/java.io=ALL-UNNAMED \
--add-opens java.base/java.net=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED \
--add-opens java.base/java.util=ALL-UNNAMED \
--add-opens java.base/java.util.concurrent=ALL-UNNAMED \
--add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED \
--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens java.base/sun.nio.cs=ALL-UNNAMED \
--add-opens java.base/sun.security.action=ALL-UNNAMED \
--add-opens java.base/sun.util.calendar=ALL-UNNAMED \
--add-opens java.security.jgss/sun.security.krb5=ALL-UNNAMED \
--add-opens javafx.base/com.sun.javafx.runtime=ALL-UNNAMED \
--add-exports java.base/sun.nio.ch=ALL-UNNAMED \
--add-exports java.base/sun.security.util=ALL-UNNAMED \
-Xmx4g \
-Xss64m"

# Find the built jar
JAR_PATH="target/ml_and_fx-1.0-SNAPSHOT.jar"

# Find JavaFX jars in local Maven repo (Linux/Mac)
JAVAFX_VERSION="17.0.6"
JAVAFX_MODULES=(base controls fxml graphics media swing web)
JAVAFX_CP=""
JAVAFX_MP=""
for m in "${JAVAFX_MODULES[@]}"; do
  # API JAR
  JAR_API=$(find ~/.m2/repository/org/openjfx/javafx-$m/$JAVAFX_VERSION -name "javafx-$m-$JAVAFX_VERSION.jar" 2>/dev/null | head -n 1)
  # Platform-specific JAR (linux)
  JAR_LINUX=$(find ~/.m2/repository/org/openjfx/javafx-$m/$JAVAFX_VERSION -name "javafx-$m-$JAVAFX_VERSION-linux.jar" 2>/dev/null | head -n 1)
  if [ -n "$JAR_API" ]; then
    JAVAFX_MP="$JAVAFX_MP:$JAR_API"
  fi
  if [ -n "$JAR_LINUX" ]; then
    JAVAFX_MP="$JAVAFX_MP:$JAR_LINUX"
  fi
  MODULE_LIST+="javafx.$m,"
done
# Remove trailing comma
MODULE_LIST=${MODULE_LIST%,}

# Remove leading colon from JAVAFX_MP (if any)
JAVAFX_MP=${JAVAFX_MP#:}

if [ -z "$JAVAFX_MP" ]; then
  echo "Не найдены JavaFX JAR'ы для module-path! Проверьте наличие org.openjfx:javafx-*:$JAVAFX_VERSION в ~/.m2/repository."
  exit 2
else
  echo "ℹ️  JavaFX module-path: $JAVAFX_MP"
fi

# Get Maven runtime classpath (dependencies)
./mvnw dependency:build-classpath -Dmdep.includeScope=runtime -Dmdep.outputFile=cp.txt >/dev/null 2>&1
MAVEN_CP=$(cat cp.txt)

# Compose full classpath (Spark, app, and other deps)
FULL_CP="$JAR_PATH:$MAVEN_CP"

echo "Еще чуть чуть"

java $JVM_OPTS \
  --module-path "$JAVAFX_MP" \
  --add-modules=$MODULE_LIST \
  -cp "$FULL_CP" \
  amaneko.ml_and_fx.controller.MovieRecommendationApp

APP_EXIT_CODE=$?
# нужен для корректной работы
rm -f cp.txt

if [ $APP_EXIT_CODE -eq 0 ]; then
    echo "Победа, погнали"
else
    echo "Все плохо"
fi
