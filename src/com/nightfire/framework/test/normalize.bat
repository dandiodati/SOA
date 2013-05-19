rem ####### NOTE: Set PRJROOT to your root package location!!! #######

# Root of nfcommon library.
if "%NFCOMMON%"=="" set NFCOMMON=D:\gumbo\nfcommon

if "%THIRDPARTY%"=="" set THIRDPARTY=D:\tp

set JDK=%THIRDPARTY%\jdk1.1.8\lib
set XML=%THIRDPARTY%\jaxp-1.1\crimson-1.1.3


set CLASSPATH=%JDK%\classes.zip;%XML%\crimson.jar;%NFCOMMON%;

java -classpath %CLASSPATH% -Xmx200m -Xms20m com.nightfire.framework.test.XMLNormalizer %1
