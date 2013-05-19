package com.nightfire.webgui.gateway.test;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.beans.SessionInfoBean;

public class TestHttpSession implements HttpSession {

    Map<String,Object> params = new HashMap<String,Object>();
    
    public TestHttpSession()
    {
        SessionInfoBean bean = new SessionInfoBean("DEFAULT", "nfuser", "",System.currentTimeMillis());
        params.put(ServletConstants.SESSION_BEAN, bean);
    }
    
    public ServletContext getServletContext()
    {
        return new TestServletContext();
    }
    
    public Object getAttribute(String arg0) {
        return params.get(arg0);
    }

    public Enumeration getAttributeNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getCreationTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getLastAccessedTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getMaxInactiveInterval() {
        // TODO Auto-generated method stub
        return 0;
    }

    public HttpSessionContext getSessionContext() {
        
        return new TestHttpSessionContext();
    }

    public Object getValue(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getValueNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public void invalidate() {
        // TODO Auto-generated method stub

    }

    public boolean isNew() {
        // TODO Auto-generated method stub
        return false;
    }

    public void putValue(String arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    public void removeAttribute(String arg0) {
        // TODO Auto-generated method stub

    }

    public void removeValue(String arg0) {
        // TODO Auto-generated method stub

    }

    public void setAttribute(String arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    public void setMaxInactiveInterval(int arg0) {
        // TODO Auto-generated method stub

    }

    public static class TestHttpSessionContext implements HttpSessionContext
    {

        public Enumeration getIds() {
            // TODO Auto-generated method stub
            return null;
        }

        public HttpSession getSession(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
