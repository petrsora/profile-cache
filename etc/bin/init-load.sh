#!/bin/bash
JAVA_HOME=TODO

INIT_LOAD_JAR=`find /app01/profcach/lib/ -name "init-load*jar" -print | sort -r |  head -1`
[ -z "$INIT_LOAD_JAR" ] && { echo "Missing init-load jar file. Please, check /app01/profcach/lib/ directory"; exit 1; }

echo "Starting jar \"$INIT_LOAD_JAR\". Are you sure? [y/n]"
read ANSWER
[ "$ANSWER" == "y" ] || exit 1

$JAVA_HOME/bin/java -Dlog4j.configuration=file:///app01/profcach/conf/init-load-log4j.xml -jar $INIT_LOAD_JAR $* 2>/app01/profcach/log/init-load.err.`date +%y%m%d%H%M%S`
