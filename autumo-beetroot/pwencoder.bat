echo OFF
REM ------------------------------------------------------------------------------
REM 
REM   beetRoot PW Encoder
REM   Version: 1.0
REM 
REM ------------------------------------------------------------------------------


REM 
REM  TODO: ROOT path
REM 
set ROOT=%CD%



REM 
REM  Build the classpath
REM 
set CLASSPATH=%ROOT%/lib/*



REM 
REM  Encode
REM 
java -DROOTPATH="%ROOT%" -classpath "%CLASSPATH%" ch.autumo.beetroot.PWEncoder %*

