#
# Copyright (c) 2000 NightFire Software, Inc.
# All rights reserved.
#
# GNU Makefile
##############################################################################

# PGP location
ifndef PRJROOT
PRJROOT=$(HOME)/Perforce
endif

ifndef THIRDPARTY
THIRDPARTY=$(PRJROOT)/thirdparty
endif

# location of jdk
ifndef JAVA_HOME
JAVA_HOME = /usr/local/jdk1.2.1
endif

# location of logging library
ifndef NFCOMMON
NFCOMMON=$(PRJROOT)/nfcommon
endif

ifndef LOG_LIB
LOG_LIB = $(NFCOMMON)/com/nightfire/framework/util/jni
endif

# include paths
JAVA_INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/solaris \
		-I$(JAVA_HOME)/include/win32
PGP_INCLUDES = -I$(THIRDPARTY)/pgp6.5.1i/include/unix
LOG_INCLUDES = -I$(LOG_LIB)
CFLAGS  += $(JAVA_INCLUDES) $(PGP_INCLUDES) $(LOG_INCLUDES)

# libraries
LDFLAGS += -L$(THIRDPARTY)/pgp6.5.1i/lib/Debug -L$(LOG_LIB)/$(PLATFORM)
LIBRARIES += -lPGPsdk -lPGPsdkNetwork -lm -lnflog

# require thread safety
THREADSAFE = true

SOURCES =	PGPBase.C \
		PGPException.C \
		PGPInit.C 

# build a shared library
SHLIB = pgp

# include master rules
include NightFire.mk
