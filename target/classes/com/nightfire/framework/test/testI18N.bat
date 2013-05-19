rem ####### NOTE: Set PRJ_ROOT to your root package location!!! #######

if "%PRJROOT%"=="" set PRJROOT=D:\work\footprint\nfcommon

if "%THIRDPARTY%"=="" set THIRDPARTY=D:\thirdparty

set JDK=%THIRDPARTY%\jdk1.1.8\lib


set CLASSPATH=.\;%JDK%\classes.zip;%PRJROOT%\nfcommon;


java -classpath %CLASSPATH% com.nightfire.framework.test.I18NTest



