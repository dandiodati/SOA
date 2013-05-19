package com.nightfire.webgui.gateway.test;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class TestServletContextEvent extends ServletContextEvent{

    public TestServletContextEvent(ServletContext arg0) {
        super(arg0);
    }
}
