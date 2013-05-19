/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.manager.tag;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.manager.beans.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.beans.*;


import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.constants.*;

import  java.util.*;

import  javax.servlet.*;
import javax.servlet.http.*;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import  com.nightfire.framework.util.*;
import java.net.*;
import java.io.*;

import org.w3c.dom.*;
import com.nightfire.webgui.manager.svcmeta.ComponentDef;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.debug.DebugLogger;

/**
 * This is a holder class returned by some beans to provide information on an order.
 *
 */
public class OrderBeanInfo
{


    private String supplier;
    private ComponentDef service;
    private String version;
    private String name;
    private NFBean bean;
    




    public String toString()
    {
        String svcName = null;
        if (service != null)
            svcName = service.getID();


        return "OrderBeanInfo Name [" + getName() +"] Service [" + svcName +"] Supplier [" + getSupplier() +
            "] Version [" + getVersion() +"]";
    }



    public OrderBeanInfo(NFBean bean, ComponentDef service, String beanName)
    {
        this(service, null, null, beanName);
        this.bean = bean;
    }



    public OrderBeanInfo(ComponentDef service, String supplier, String version, String beanName)
    {
        setService(service);
        setSupplier(supplier);
        setVersion(version);
        setName(beanName);
        
        
    }




    public void setService(ComponentDef service)
    {
        this.service = service;
    }


    public ComponentDef getService()
    {
        return service;
    }



 
    public void setSupplier(String supplier)
    {
        if (bean != null) {
            try {
                
                bean.setHeaderValue(ServletConstants.SUPPLIER, supplier);
            }
            catch(Exception e ) {
                DebugLogger.getLoggerLastApp(this.getClass()).error("Failed to set supplier value [" + supplier+"]");
            }
            
        } 
        else
            this.supplier = supplier;
    }


    public String getSupplier()
    {
        if (bean != null)
            return bean.getHeaderValue(ServletConstants.SUPPLIER);
        else
            return supplier;
    }


    public void setVersion(String version)
    {
        if (bean != null) {
            
            try {
                bean.setHeaderValue(ServletConstants.INTERFACE_VER, version);
           }
            catch(Exception e ) {
                DebugLogger.getLoggerLastApp(this.getClass()).error("Failed to set interface version value [" + version+"]");
            }
        }
    
        else
            this.version = version;
    }

    public String getVersion()
    {
        if (bean != null)
            return bean.getHeaderValue(ServletConstants.INTERFACE_VER);
        else
            return version;
    }


    public void setName(String name)
    {
        this.name = name;

    }


    public String getName()
    {
        return name;
    }

    public NFBean getBean()
    {
        return bean;
    }


}
