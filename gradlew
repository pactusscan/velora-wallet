#!/bin/sh
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
MAX_FD="maximum"
warn() { echo "$*"; }
die() { echo; echo "ERROR: $*"; echo; exit 1; }
if [ "$1" = "--add-opens" ] ; then shift; fi
PRG="$0"
while [ -h "$PRG" ] ; do ls=$(ls -ld "$PRG"); link=$(expr "$ls" : '.*-> \(.*\)$'); if expr "$link" : '/.*' > /dev/null; then PRG="$link"; else PRG=$(dirname "$PRG")"/$link"; fi; done
SAVED=$(pwd)
cd "$(dirname "$PRG")/" >/dev/null
APP_HOME=$(pwd -P)
cd "$SAVED" >/dev/null
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_EXE="java"
exec "$JAVA_EXE" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
