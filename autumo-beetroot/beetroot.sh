#!/bin/sh


#------------------------------------------------------------------------------
#
#  beetRoot Server
#  Version: 1.0
#
#------------------------------------------------------------------------------



#
# ROOT path: Current directory
#
ROOT=.


#
# Base classpath
#
CLASSPATH=${ROOT}:web/


#
# Dynamically build the classpath
#
COUNT=0
LIB_CLASSPATH=
for i in `ls ${ROOT}/lib/*.jar`
do
    if [ $COUNT -eq 0 ]; then
            LIB_CLASSPATH=${i}
    else
            LIB_CLASSPATH=${LIB_CLASSPATH}:${i}
    fi
COUNT=$((c+1))
done
CLASSPATH=${CLASSPATH}:${LIB_CLASSPATH}



#
# Run : server.sh start|stop
#
java \
	-DROOTPATH="${ROOT}" \
	-Djdk.tls.client.protocols=TLSv1.2 \
	-cp "${CLASSPATH}" ch.autumo.beetroot.BeetRootServer $*

