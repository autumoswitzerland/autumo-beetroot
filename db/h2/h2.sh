#!/bin/sh
dir=$(dirname "$0")
java -cp "$dir/h2-2.3.230.jar:$H2DRIVERS:$CLASSPATH" org.h2.tools.Console "$@"
