
setlocal

set CLASSPATH=%CP%;


vbj -DORBservices=CosNaming -DSVCnameroot=Nightfire -classpath %CLASSPATH% com.nightfire.servers.CosEvent.test.EventUnitTest %1

endlocal

