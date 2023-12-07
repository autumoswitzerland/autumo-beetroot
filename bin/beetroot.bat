echo OFF
REM ------------------------------------------------------------------------------
REM 
REM   beetRoot Server
REM   Version: 2.0
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
set CLASSPATH=%ROOT%;%ROOT%/web;%ROOT%/lib/*



REM 
REM  Run : run.bat <ifx-base-path> <proc-mode>
REM 
java -DROOTPATH="%ROOT%" -Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2,TLSv1.3 -classpath "%CLASSPATH%" ch.autumo.beetroot.server.BeetRootServer %*

popd
