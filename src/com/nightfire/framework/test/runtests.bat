rem ####### NOTE: Set NFCOMMON to your root package location!!! #######

if "%NFCOMMON%"=="" set NFCOMMON=D:\NFCore\nfcommon

if "%THIRDPARTY%"=="" set THIRDPARTY=D:\tp

set JDK=%THIRDPARTY%\jdk1.2.2\lib
set XML=%THIRDPARTY%\jaxp-1.1\crimson-1.1.3
set ORACLE=%THIRDPARTY%\orant8.0\jdbc\lib
set VISIGENIC=%THIRDPARTY%\visigenic3.4\vbroker\lib


set CLASSPATH=%JDK%\dt.jar;%NFCOMMON%;%XML%\crimson.jar;%ORACLE%\816classes12b.zip;%VISIGENIC%\vbjorb.jar;%VISIGENIC%\vbjapp.jar;%VISIGENIC%\vbjcosev.jar;%VISIGENIC%\vbjcosnm.jar;%THIRDPARTY%\jce1.2\lib\jce1_2-do.jar;

if "%1"=="util" goto util
if "%1"=="msg" goto msg
if "%1"=="db" goto db
if "%1"=="pool" goto pool
if "%1"=="rules" goto rules
if "%1"=="naming" goto naming
if "%1"=="event" goto event

:util
java -classpath %CLASSPATH% -DDEBUG_LOG_LEVELS=all -DMAX_DEBUG_WRITES=5 com.nightfire.framework.test.TestUtils
if "%1"=="util" goto end

:db
vbj -DORBservices=CosNaming -DSVCnameroot=Nightfire -classpath %CLASSPATH% com.nightfire.framework.test.TestDatabase
if "%1"=="db" goto end

:pool
vbj -DORBservices=CosNaming -DSVCnameroot=Nightfire -classpath %CLASSPATH% com.nightfire.framework.test.TestDatabasePools
if "%1"=="pool" goto end

:msg
java -classpath %CLASSPATH% com.nightfire.framework.test.TestMessaging
if "%1"=="msg" goto end

:rules
java -classpath %CLASSPATH% com.nightfire.framework.test.TestRules ruleset.xml msg_api_test.xml
if "%1"=="rules" goto end

:naming
vbj -DORBservices=CosNaming -DSVCnameroot=Nightfire -classpath %CLASSPATH% com.nightfire.framework.test.TestNameService
if "%1"=="naming" goto end

:event
vbj -DORBservices=CosNaming -DSVCnameroot=Nightfire -classpath %CLASSPATH% com.nightfire.framework.test.TestEventClient
if "%1"=="event" goto end

:end