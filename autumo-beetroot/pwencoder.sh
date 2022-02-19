#!/bin/sh


#------------------------------------------------------------------------------
#
#  beetRoot PW Encoder
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
	-cp "${CLASSPATH}" \
	ch.autumo.beetroot.PWEncoder $*

