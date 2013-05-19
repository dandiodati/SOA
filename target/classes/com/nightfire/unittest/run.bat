rem ####### NOTE: Set PRJ_ROOT to your root package location!!! #######
set PRJROOT=D:\Footprint\nfcommon
set THIRDPARTY=D:\Footprint\thirdparty

set JDK=%THIRDPARTY%\jdk1.1.7B\lib
set XML=%THIRDPARTY%\xml4j
set ORACLE=%THIRDPARTY%\orant8.0\jdbc\lib
set VISIGENIC=%THIRDPARTY%\visigenic3.3\vbroker\lib


set CLASSPATH=%JDK%\classes.zip;%PRJROOT%;%XML%\xml4j_1_1_14.jar;%ORACLE%\classes111.zip;


java -classpath %CLASSPATH% com.nightfire.unittest.ExampleUnitTest
