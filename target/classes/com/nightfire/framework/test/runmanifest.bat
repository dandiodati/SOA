rem ####### NOTE: Set NFCOMMON to your root package location!!! #######

if "%NFCOMMON%"=="" set NFCOMMON=D:\gumbo\nfcommon

if "%THIRDPARTY%"=="" set THIRDPARTY=D:\tp

set JDK=%THIRDPARTY%\jdk1.1.8\lib
set XML=%THIRDPARTY%\jaxp-1.1\crimson-1.1.3
set ORACLE=%THIRDPARTY%\orant8.0\jdbc\lib
set VISIGENIC=%THIRDPARTY%\visigenic3.3\vbroker\lib


set CLASSPATH=%JDK%\classes.zip;%NFCOMMON%;%XML%\crimson.jar;%ORACLE%\classes111.zip;%VISIGENIC%\vbjorb.jar;%VISIGENIC%\vbjapp.jar;%VISIGENIC%\vbjcosev.jar;%VISIGENIC%\vbjcosnm.jar

java -classpath %CLASSPATH% com.nightfire.framework.test.XMLNodeManifest %1 %2
