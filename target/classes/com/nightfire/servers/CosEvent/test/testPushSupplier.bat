REM $Header: //nfcommon/R4.4/com/nightfire/servers/CosEvent/test/testPushSupplier.bat#1 $

rem ####### NOTE: Set PRJ_ROOT to your root package location!!! #######
set PRJROOT=D:\Footprint\nfcommon
set THIRDPARTY=D:\Footprint\thirdparty

set JDK=%THIRDPARTY%\jdk1.1.7B\lib
set XML=%THIRDPARTY%\xml4j
set ORACLE=%THIRDPARTY%\orant8.0\jdbc\lib
set VISIGENIC=%THIRDPARTY%\visigenic3.3\vbroker\lib



set CLASSPATH=%JDK%\classes.zip;%PRJROOT%;%XML%\xml4j_1_1_14.jar;%ORACLE%\classes111.zip;%VISIGENIC%\vbjcosev.jar;%VISIGENIC%\vbjcosnm.jar;%VISIGENIC%\vbjorb.jar;%VISIGENIC%\vbjapp.jar;


vbj -DORBservices=CosNaming -DSVCnameroot=Nightfire -classpath %CLASSPATH% com.nightfire.servers.CosEvent.test.TestPushSupplier %1

