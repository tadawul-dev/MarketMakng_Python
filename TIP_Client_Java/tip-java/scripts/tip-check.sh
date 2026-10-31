#!/usr/bin/env bash
# Connectivity check against one or more TIP servers; see README ("Connectivity check").
#   scripts/tip-check.sh -a 10.0.0.5:4000 -u user -p secret
# Uses target/ (mvn package) or build/classes (scripts/run-tests.sh); extra jars via CLASSPATH_EXTRA.
cd "$(dirname "$0")/.."
CP="target/tip-client-1.0.0.jar:target/lib/*"
[[ -d build/classes ]] && CP="build/classes:$CP"
exec java -cp "$CP${CLASSPATH_EXTRA:+:$CLASSPATH_EXTRA}" tip.client.TipConnectCheck "$@"
