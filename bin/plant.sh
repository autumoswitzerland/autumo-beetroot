#!/bin/bash


#------------------------------------------------------------------------------
#
#  PLANT - beetRoot CRUD generator
#  Version: 2.1
#
#------------------------------------------------------------------------------


#
# ROOT path
#
cd "$(dirname "$0")/.."
ROOT=`pwd`

#
# Base classpath
#
CLASSPATH=${ROOT}

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
# Encode 
#
java \
	-DROOTPATH="${ROOT}" \
	-cp "${CLASSPATH}" \
	ch.autumo.beetroot.plant.Plant $*


cd "$(dirname "$0")/bin"
