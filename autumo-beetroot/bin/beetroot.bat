echo OFF
REM ------------------------------------------------------------------------------
REM 
REM   beetRoot Server
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
set CLASSPATH=%ROOT%;%ROOT%/web;%ROOT%/lib/*



REM 
REM  Run : run.bat <ifx-base-path> <proc-mode>
REM 
java -DROOTPATH="%ROOT%" -Djdk.tls.client.protocols=TLSv1.2 -classpath "%CLASSPATH%" ch.autumo.beetrootserver.server.BeetRootServer %*

