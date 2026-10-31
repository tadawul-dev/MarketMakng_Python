#!/usr/bin/env bash
# Compiles the library and the test suite with plain javac and runs it with JUnit 4 -- no Maven needed.
#
#   scripts/run-tests.sh                 run everything
#   scripts/run-tests.sh tip.xm.XmCryptTest tip.client.LoggingTest     run selected classes
#
# Needs JDK 21+, junit-4.13.2.jar, hamcrest-core-1.3.jar, log4j-api-2.x.jar and log4j-core-2.x.jar.
# Each is looked up in $JUNIT_JAR / $HAMCREST_JAR / $LOG4J_API_JAR / $LOG4J_CORE_JAR, then in ~/.m2,
# then in $GRADLE_HOME/lib, /opt/gradle*/lib and /usr/share/java.
set -euo pipefail
cd "$(dirname "$0")/.."

find_jar() {   # name-pattern
    local v="$1" p="$2" f
    if [[ -n "$v" && -f "$v" ]]; then echo "$v"; return; fi
    for d in "$HOME/.m2/repository" "${GRADLE_HOME:-/nonexistent}/lib" /opt/gradle*/lib /usr/share/java; do
        f=$(find "$d" -name "$p" 2>/dev/null | head -1 || true)
        if [[ -n "$f" ]]; then echo "$f"; return; fi
    done
    echo "cannot find $p (set JUNIT_JAR / HAMCREST_JAR)" >&2; exit 1
}
JUNIT=$(find_jar "${JUNIT_JAR:-}" 'junit-4.13*.jar')
HAMCREST=$(find_jar "${HAMCREST_JAR:-}" 'hamcrest-core-1.3*.jar')
LOG4J_API=$(find_jar "${LOG4J_API_JAR:-}" 'log4j-api-2*.jar')
LOG4J_CORE=$(find_jar "${LOG4J_CORE_JAR:-}" 'log4j-core-2*.jar')

if command -v javac >/dev/null 2>&1; then JAVAC=(javac); else JAVAC=(java -m jdk.compiler/com.sun.tools.javac.Main); fi

LOGCP="$LOG4J_API:$LOG4J_CORE"
OUT=build/classes; TOUT=build/test-classes
rm -rf build; mkdir -p "$OUT" "$TOUT"
CP="$OUT:$LOGCP:$JUNIT:$HAMCREST"
"${JAVAC[@]}" -Xlint:all -cp "$LOGCP" -d "$OUT" $(find src/main -name '*.java')
cp -r src/main/resources/* "$OUT"/
"${JAVAC[@]}" -cp "$CP" -d "$TOUT" $(find src/test/java -name '*.java')
cp -r src/test/resources/* "$TOUT"/

if [[ $# -gt 0 ]]; then CLASSES=("$@"); else
    mapfile -t CLASSES < <(cd src/test/java && find tip -name '*Test.java' ! -name 'LoopbackSelfTest.java' | sed 's/\.java$//; s#/#.#g' | sort)
fi
java -cp "$TOUT:$CP" org.junit.runner.JUnitCore "${CLASSES[@]}"

if [[ $# -eq 0 ]]; then
    echo "--- end-to-end self test"
    java -cp "$TOUT:$CP" tip.LoopbackSelfTest
fi
