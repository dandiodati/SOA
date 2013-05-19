#
# Copyright(c) 2000 NightFire Software, Inc.
# All rights reserved.
#
# GNU Makefile
#******************************************************************************

#
# Variables not dependent on the platform (that may be overriden by the
# platform)
#

# determine our OS and platform
OSNAME   := $(shell uname -s)
PLATFORM := $(shell uname -sr | sed 's/ /-/g')

# set various output locations
DEP_LOC    = $(PLATFORM)/dep
OBJ_LOC    = $(PLATFORM)/obj
TARGET_LOC = $(PLATFORM)

# if not debugging, allow some commands to produce less output
ifndef DEBUG_MAKEFILE
BEQUIET = @
endif

#
# Include various project/platform specific rules, definitions
#

# include all .mkr files
ifndef RULES
RULES = .
endif
MAKE_RULES := $(wildcard $(RULES)/*.mkr)
ifneq (,$(strip $(MAKE_RULES)))
include $(MAKE_RULES)
endif

#
# Define the various intermediate files
#

# deduce objects from sources
OBJECTS := $(addprefix $(OBJ_LOC)/,$(addsuffix $(OBJ_EXT),\
		$(notdir $(basename $(SOURCES)))))

# deduce dependencies from sources
ifndef NO_DEPENDS
DEPENDS = $(addprefix $(DEP_LOC)/,$(addsuffix $(DEP_EXT),\
		$(notdir $(basename $(SOURCES)))))
endif

#
# Define targets
#

# static library
ifdef STLIB
STLIB_TARGET    = $(TARGET_LOC)/$(STLIB_PREF)$(STLIB)$(STLIB_SUFF)
endif

# shared library
ifdef SHLIB
SHLIB_TARGET  = $(TARGET_LOC)/$(SHLIB_PREF)$(SHLIB)$(SHLIB_SUFF)
endif

# executable
ifdef EXEC
EXEC_TARGET = $(TARGET_LOC)/$(EXEC)$(EXEC_SUFF)
endif

#
# All rule:
#  Make static library
#  Make shared library
#  Make executable

all:: $(STLIB_TARGET) $(SHLIB_TARGET) $(EXEC_TARGET)

#
# depend rule:
#  Only generates dependencies
depend:: $(DEPENDS)

# rules for generating dependencies from source files
$(DEP_LOC)/%$(DEP_EXT):       %.C
	$(GENERATE_DEPEND)
$(DEP_LOC)/%$(DEP_EXT):       %.c
	$(GENERATE_DEPEND)
$(DEP_LOC)/%$(DEP_EXT):       %.cc
	$(GENERATE_DEPEND)
$(DEP_LOC)/%$(DEP_EXT):       %.cpp
	$(GENERATE_DEPEND)
$(DEP_LOC)/%$(DEP_EXT):       %.cxx
	$(GENERATE_DEPEND)

# include all of the dependencies
ifndef NO_DEPENDS
ifneq (,$(strip $(DEPENDS)))
-include $(DEPENDS)
endif
endif

# rules for generating objects from source files
$(OBJ_LOC)/%$(OBJ_EXT):    %.C
	$(GENERATE_OBJECT)
$(OBJ_LOC)/%$(OBJ_EXT):    %.c
	$(GENERATE_OBJECT)
$(OBJ_LOC)/%$(OBJ_EXT):    %.cc
	$(GENERATE_OBJECT)
$(OBJ_LOC)/%$(OBJ_EXT):    %.cpp
	$(GENERATE_OBJECT)
$(OBJ_LOC)/%$(OBJ_EXT):    %.cxx
	$(GENERATE_OBJECT)

#
# Top-level targets
# 

# static library
$(STLIB_TARGET): $(OBJECTS)
	$(GENERATE_ARCHIVE)

# shared library
$(SHLIB_TARGET): $(OBJECTS)
	$(GENERATE_SHARCHIVE)

# executable
$(EXEC_TARGET): $(OBJECTS)
	$(GENERATE_EXEC)

#
# Clean rule
#

clean::
	$(RMDIR) $(PLATFORM)
