rem ####### NOTE: Set PRJ_ROOT to your root package location!!! #######

if "%PRJROOT%"=="" set PRJROOT=D:\gumbo

if "%NFCOMMON%"=="" set NFCOMMON=%PRJROOT%\nfcommon

if "%THIRDPARTY%"=="" set THIRDPARTY=D:\tp

set JDK=%THIRDPARTY%\jdk1.2.2\lib
set XML=%THIRDPARTY%\jaxp-1.1\crimson-1.1.3
set ORACLE=%THIRDPARTY%\orant8.0\jdbc\lib
set VISIGENIC=%THIRDPARTY%\visigenic3.4\vbroker\lib


set CLASSPATH=%JDK%\classes.zip;%NFCOMMON%;%XML%\crimson.jar;

java -classpath %CLASSPATH% com.nightfire.framework.test.XMLParseGen %1 %2
