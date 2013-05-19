
REM Used to test the PropertyChainUtil.java class
REM
REM 1.need the PropertyChainUtil.properties file
REM  a. need to modify this properties file to points to
REM     the right database with correct password etc. 
REM
REM 2. need a database that was created with the property editor
REM  a. need to import the PropertyChainUtil.db.settings file.
REM
REM
REM 
REM
REM USAGE: takes in two parameters, the key and type of the 
REM        table that you want to look for chains in.
REM
REM   key    type        test
REM   ---    ----  	---------------------
REM   GTE    ORDERS        test for no chains
REM PACBELL   orders       test for key and type 
REM PACBELL   user_info    test for key and using same type(user_info)
REM PACBELL   order_info   test for type, same key(PACBELL), and a double chain
REM circle    first        test a circular reference
REM GMOrders Test          checks if same combined key and type causes circular reference.
REM
REM output goes to test.log
REM


@echo off

REM need to set the projroot variable
if "%PRJROOT%"=="" set PRJROOT=D:\p4work
if "%THIRDPARTY%"=="" set THIRDPARTY=%PRJROOT%\thirdparty
if "%NFCOMMON%"=="" set NFCOMMON=%PRJROOT%\nfcommon

if "%JDK%"=="" set JDK=%THIRDPARTY%\jdk1.1.5
if "%SWING%"=="" set SWING=%THIRDPARTY%\swing-1.0.1
if "%BUILD%"=="" set BUILD=%PRJROOT%\nfcommon\com\nightfire\framework\util

set ORACLE=%THIRDPARTY%\orant8.0\jdbc\lib

set CLASSPATH=%JDK%\lib\classes.zip;%BUILD%;%SWING%\swingall.jar;%ORACLE%\classes111.zip;.;%NFCOMMON%

echo PRJROOT=%PRJROOT%
echo BUILD=%BUILD%
echo THIRDPARTY=%THIRDPARTY%
echo NFCOMMON=%NFCOMMON%
echo JDK=%JDK%
echo SWING=%SWING%

@echo on


%JDK%\bin\jre -classpath %CLASSPATH% com.nightfire.framework.db.PropertyChainUtil PropertyChainUtil.properties %1 %2


