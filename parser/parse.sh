#!/bin/sh

MVN_COMMAND="mvn exec:java -Dexec.mainClass=\"com.wirelust.appleworks.Parser\" -Dexec.args=\"$@\""
echo $MVN_COMMAND

eval $MVN_COMMAND
