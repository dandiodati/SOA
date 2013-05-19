set JDK_HOME=d:/workspace/thirdparty/jdk1.2.2/jre
set EMAIL_HOME=d:/workspace/thirdparty/javamail-1.1.2/mail.jar;d:/workspace/thirdparty/jaf-1.0.1/activation.jar
set NFCOMMON_HOME=d:/workspace/nfcommon/main/lib

set CLASSPATH=%JDK_HOME%/lib/rt.jar;%EMAIL_HOME%;%NFCOMMON_HOME%

%JDK_HOME%/bin/java -classpath %CLASSPATH% com.nightfire.framework.test.TestSMTPEmailUtils %1 %2 %3 %4 %5
