#!/usr/bin/env bash
# TIP Web Listener launcher (Java 25). Pure Java: no libtipclient.so / JNI any more.
# Usage: bin/run-listener.sh [path/to/efp.ini]
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
INI="${1:-$APP_HOME/config/efp.ini}"
LOG_DIR="${TIP_LOG_DIR:-/WebsiteJobs/Mohan/Tip/log}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

# Heap: the working set is small (a few hundred instruments + queues); a fixed heap avoids resizing.
HEAP_OPTS="${HEAP_OPTS:--Xms512m -Xmx512m}"

# GC: Generational ZGC keeps pauses well under 1 ms, so the reader thread never stalls on GC.
# (In JDK 25, -XX:+UseZGC is generational; non-generational ZGC was removed.)
# Use -XX:+UseG1GC -XX:MaxGCPauseMillis=20 instead if the host has less than 2 cores.
GC_OPTS="${GC_OPTS:--XX:+UseZGC -XX:+AlwaysPreTouch}"

# JDK 25+: compact object headers (JEP 519) - smaller VOs, less GC work. Not available on JDK 21.
JAVA_MAJOR="$("$JAVA" -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.specification.version = //p')"
if [ "${JAVA_MAJOR:-21}" -ge 25 ]; then
  JVM_OPTS="${JVM_OPTS:--XX:+UseCompactObjectHeaders}"
else
  JVM_OPTS="${JVM_OPTS:-}"
fi

exec "$JAVA" \
  $HEAP_OPTS $GC_OPTS $JVM_OPTS \
  -Xlog:gc*:file="$LOG_DIR/gc.log":time,uptime:filecount=5,filesize=20m \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="$LOG_DIR" \
  -Dlog4j2.configurationFile="$APP_HOME/config/log4j2.xml" \
  -Dlog4j2.contextSelector=org.apache.logging.log4j.core.selector.BasicContextSelector \
  -Dtip.log.dir="$LOG_DIR" \
  -DLISTENER_INI_FILE="$INI" \
  -cp "$APP_HOME/tip-web-listener-2.1.0.jar:$APP_HOME/lib/*" \
  sa.com.tadawul.applications.tip.isi.efp.analyser.AnalyserReader
