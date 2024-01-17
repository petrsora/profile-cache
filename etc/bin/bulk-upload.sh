#!/bin/bash
JAVA_HOME=TODO

BULK_UPLOAD_JAR=`find /app01/profcach/lib/ -name "bulk-upload*jar" -print | sort -r |  head -1`
[ -z "$BULK_UPLOAD_JAR" ] && { echo "Missing bulk upload jar file. Please, check /app01/profcach/lib/ directory"; exit 1; }

echo "Starting jar \"$BULK_UPLOAD_JAR\". Are you sure? [y/n]"
read ANSWER
[ "$ANSWER" == "y" ] || exit 1

$JAVA_HOME/bin/java -Dlog4j.configuration=file:///app01/profcach/conf/bulk-upload-log4j.xml -jar $BULK_UPLOAD_JAR $* 2>/app01/profcach/log/bulk-upload.err.`date +%y%m%d%H%M%S`
