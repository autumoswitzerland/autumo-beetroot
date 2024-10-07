echo OFF
REM ------------------------------------------------------------------------------
REM 
REM   beetRoot Update Check
REM   Version: 1.0
REM 
REM ------------------------------------------------------------------------------


pushd %cd%

REM 
REM  ROOT path
REM 
for %%d in (%~dp0..) do set ROOT=%%~fd
cd %ROOT%

REM 
REM  Build the classpath
REM 
set CLASSPATH=%ROOT%/lib/*



REM 
REM  Encode
REM 
java -DROOTPATH="%ROOT%" -classpath "%CLASSPATH%" ch.autumo.beetroot.utils.system.Update %*

popd
