#
# Copyright (c) 2000 NightFire Software, Inc.
# All rights reserved.
#
# GNU Makefile
##############################################################################

# location of Debug.java, or equivalent
ifeq (,$(strip $(LEVELS_SRC)))
LEVELS_SRC = ../Debug.java
endif

# location of jdk
ifeq (,$(strip $(JAVA_HOME)))
JAVA_HOME = /usr/local/jdk1.2.1
endif

# include paths
CFLAGS += -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/solaris
CFLAGS += -I$(JAVA_HOME)/include/win32

# require thread safety
THREADSAFE = true

# source files
SOURCES = NFLogger.C \
	prepareTable.C

STLIB = nflog
SHLIB = nflog

# include the mother makefile
include NightFire.mk

# these rules/dependencies cannot be auto determined
prepareTable.C : $(LEVELS_SRC)
	@echo Auto-generating $(@F)...
	$(BEQUIET)$(PERL) GenCLogLevels $<

NFLoggerLevel.h : $(LEVELS_SRC)
	@echo Auto-generating $(@F)...
	$(BEQUIET)$(PERL) GenCLogLevels $<
