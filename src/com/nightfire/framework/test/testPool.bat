rem ####### NOTE: Set PRJ_ROOT to your root package location!!! #######

if "%THIRDPARTY%"=="" set THIRDPARTY=D:\tp

set JDK=%THIRDPARTY%\jdk1.2.2\lib
set XML=%THIRDPARTY%\jaxp-1.1\crimson-1.1.3
set ORACLE=%THIRDPARTY%\orant8.0\jdbc\lib
set VISIGENIC=%THIRDPARTY%\visigenic3.4\vbroker\lib


if "%NFCOMMON%"=="" set NFCOMMON=%PRJROOT%\nfcommon
if "%GATEWAY%"=="" set GATEWAY=%PRJROOT%\gateway
if "%GATEWAY_TEST%"=="" set GATEWAY_TEST=%GATEWAY%\runtime


set CLASSPATH=.;%JDK%\classes.zip;%XML%\crimson.jar;%ORACLE%\816classes12b.zip;%VISIGENIC%\vbjorb.jar;%VISIGENIC%\vbjapp.jar;%VISIGENIC%\vbjcosev.jar;%VISIGENIC%\vbjcosnm.jar;%NFCOMMON%;%GATEWAY%;%GATEWAY_TEST%;..\..\..;

set CLASSPATH=.;%JDK%\classes.zip;%XML%\xml.jar;%ORACLE%\816classes12b.zip;%VISIGENIC%\vbjorb.jar;%VISIGENIC%\vbjapp.jar;%VISIGENIC%\vbjcosev.jar;%VISIGENIC%\vbjcosnm.jar;%NFCOMMON%;%GATEWAY%;%GATEWAY_TEST%;..\..\..;

java -classpath %CLASSPATH% -DIDLE_DBCONNECTION_CLEANUP_WAIT_TIME=1 -DINIT_DBCONNECTION_SIZE=2 com.nightfire.framework.test.TestPool jdbc:oracle:thin:@192.168.10.11:1521:orcl bentley bentley

