#!/bin/sh


#------------------------------------------------------------------------------
#
#  beetRoot Server
#  Version: 2.0
#
#------------------------------------------------------------------------------


pushd() {
  command pushd "$@" > /dev/null
}
popd() {
  command popd > /dev/null
}

pushd `pwd`

#
# ROOT path
#
cd "$(dirname "$0")/.."
ROOT=`pwd`

#
# Base classpath
#
CLASSPATH=${ROOT}:${ROOT}/web

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
	-Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2 \
	-cp "${CLASSPATH}" ch.autumo.beetroot.server.BeetRootServer $* &

popd
