#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper
# ----------------------------------------------------------------------------

MAVEN_PROJECTBASEDIR=${MAVEN_PROJECTBASEDIR:-"$(pwd)"}
MAVEN_OPTS="${MAVEN_OPTS:--Xmx1024m}"

if [ -f "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" ]; then
    exec "$JAVA_HOME/bin/java" \
        $MAVEN_OPTS \
        -classpath "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" \
        org.apache.maven.wrapper.MavenWrapperMain "$@"
else
    echo "Maven wrapper jar not found"
    exit 1
fi