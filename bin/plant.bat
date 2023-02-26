echo OFF
REM ------------------------------------------------------------------------------
REM 
REM   PLANT - beetRoot CRUD generator
REM   Version: 1.0
REM 
REM ------------------------------------------------------------------------------



REM 
REM  ROOT path
REM 
for %%d in (%~dp0..) do set ROOT=%%~fd
cd ..

REM 
REM  Build the classpath
REM 
set CLASSPATH=%ROOT%/lib/*



REM 
REM  Encode
REM 
java -DROOTPATH="%ROOT%" -classpath "%CLASSPATH%" ch.autumo.beetroot.plant.Plant %*

