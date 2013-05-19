
ifndef THIRDPARTY
THIRDPARTY=D:/p4dev/thirdparty
endif

UNIX=SUN

ifdef UNIX
# Note the libxfer.so needs to be compiled with cc4.2 or we end up having null 
# file names passed into functions
#   CC=CC
#   CFLAGS = -g -G -DUNIX -D_FILE_OFFSET_BITS=64 -Kpic -mt -D__EXTENSIONS__ 
#-DEA_SOLARIS251 -DEA_62 -DEA_DLL
#   CC=g++
#   CC=cc
   CC=CC
   CFLAGS = -g -G -DUNIX -D_FILE_OFFSET_BITS=64 -Kpic -mt -D__EXTENSIONS__ 
#   CFLAGS = -g -Wall -DUNIX -fPIC -D_FILE_OFFSET_BITS=64 -pedantic -shared
# SYSLFLAGS = -lxfersolar26 -lssockapi -lcpsql -lm -lw -lthread -lpthread -lsocket -lnsl
 SYSLFLAGS = -lbtdao -lcpcert -lmdndb -lrouter -lslogmsg -lssockapi -lsqlapi -lcpsql -lxfersolar26sc5 -lm -lw -lthread -lpthread -lsocket -lnsl

   OFLAGS= -c
   JDK_HOME=/usr/local/jdk1.2.1
#   JDK_HOME=/usr/local/jdk1.3.1_02
   IFLAGS = -I$(JDK_HOME)/include -I$(JDK_HOME)/include/solaris -ISOLARIS/headers -LSOLARIS/libs 
   # LFLAGS = -lrimport -ltipem -lbsafe -lcpftpssl -lea2ksolar26d -lcomm -lssl -lcpcert -ltipem -lbsafe -lnsl -lsocket -lxnet
   #
else
   CC=cl
   JDK_HOME=$(THIRDPARTY)/jdk1.2.2
   IFLAGS= -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -INT/headers
   OFLAGS= -c
   LFLAGS= /link /LIBPATH:NT/libs /NODEFAULTLIB:LIBC /DEFAULTLIB:ea2kw95d
endif


all: 
	@echo Need to specify platform: 
	@echo make SUN - Solaris, need to define variable UNIX in your environment
	@echo make NT - Microsoft NT, if using MSVC make sure to run vcvars32.exe

SUN:  FtpSslLayerInterconnectJni.h libFtpSslLayerInterconnectNative.so 

NT:  FtpSslLayerInterconnectJni.h FtpSslLayerInterconnectNative.dll 

clean:
	@echo Need to specify platform: 
	@echo make SUN_clean - Solaris
	@echo make NT_clean - Microsoft NT

SUN_clean:
	rm -f *.o
	rm FtpSslLayerInterconnectJni.h
	rm libFtpSslLayerInterconnectNative.so

NT_clean:
	rm -f *.obj
	rm FtpSslLayerInterconnectJni.h
	rm FtpSslLayerInterconnectNative.dll
	rm FtpSslLayerInterconnectNative.exp
	rm FtpSslLayerInterconnectNative.lib


FtpSslLayerInterconnectJni.h: ../FtpSslLayerInterconnect.class
	javah  -verbose -o FtpSslLayerInterconnectJni.h com.nightfire.comms.util.ftp.ssl.FtpSslLayerInterconnect


FtpSslLayerInterconnectNative.dll: XferReference.cpp FtpSslLayerInterconnect.cpp FtpSslLayerInterconnectJni.h
	@echo #### Need to set THIRDPARTY to build on NT. ####
	$(CC) $(IFLAGS) $(OFLAGS) XferReference.cpp -FoXferReference.obj
	$(CC) $(CFLAGS) FtpSslLayerInterconnect.cpp XferReference.obj -LD -Fe$@ $(IFLAGS) $(LFLAGS)


libFtpSslLayerInterconnectNative.so: XferReference.cpp FtpSslLayerInterconnectJni.h FtpSslLayerInterconnect.cpp
	$(CC) $(CFLAGS) $(IFLAGS) $(OFLAGS) XferReference.cpp
	$(CC) $(CFLAGS) $(IFLAGS) FtpSslLayerInterconnect.cpp XferReference.o -o $@ $(SYSLFLAGS)
