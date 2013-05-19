PACKAGE = "com.nightfire.comms.ejb"

PACKAGES = \

FILES = \
	GatewayBean.java \
	GatewayBeanImpl.java \
	GatewayBeanHome.java \
	GatewayBeanClient.java \

TOP=../../../..

ifndef MAKE_HOME
MAKE_HOME=$(TOP)/makefiles
endif

include $(MAKE_HOME)/package.mk

CLASSPATH:=$(CLASSPATH)\;.
