package com.nightfire.order.common;

import java.sql.Connection;
import java.util.HashMap;

import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;
import com.nightfire.order.utils.CHOrderConstants;
import com.nightfire.order.utils.CHOrderException;
import com.nightfire.order.CHOrderLoggerBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;


public class CHOrderContext {

    private Long oid;
    private Long lastTransOid;
    private Long eventOid;
    private String product;
    private String inputMessage;
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    private MessageProcessorContext mpc;
    private boolean transactionLogging;
    private String eventCode;
    private CHOrderLoggerBase chOrderLoggerBaseObj;

    public CHOrderContext(MessageProcessorContext mpc, boolean transactionLogging)
    {
        this.mpc = mpc;
        this.transactionLogging = transactionLogging;
    }

    public Connection getDbConnection() throws CHOrderException
    {
        try {
            if(this.transactionLogging)
                return mpc.getDBConnection();
            else
                return DBConnectionPool.getInstance( true ).acquireConnection( );
            } catch (Exception e) {
                throw new CHOrderException(e);
            }
    }

    public void setOid(Long oid)
    {
        if(this.oid!=null)
            throw new IllegalStateException("Order oid is already set !!");

        this.oid = oid;
    }

    public void setLastTransOid(Long oid)
    {
        if(this.lastTransOid!=null)
            throw new IllegalStateException("LastTransOid is already set !!");

        this.lastTransOid = oid;
    }

    public void setEventTransOid(Long oid)
    {
        if(this.eventOid!=null)
            throw new IllegalStateException("EventOid is already set !!");

        this.eventOid = oid;
    }

    private String customerId;
    public void setCustomerId(String customerId)
    {
        this.customerId = customerId;
    }

    public String getCustomerId()
    {
        return customerId;
    }

    private String user;
    public void setCurrentUser(String user)
    {
        this.user = user;
    }

    public String getCurrentUser()
    {
        return user;
    }

    public void setAttribute(String attrNm, Object attrVal) 
    {
        attributes.put(attrNm, attrVal);
    }

    public HashMap<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String attrNm) 
    {
        if(attributes.containsKey(attrNm))
            return attributes.get(attrNm);
        else
        {
            if(!mpc.exists(attrNm))
                return null;
            
            try
            {
                return mpc.get(attrNm);
            }
            catch(FrameworkException e)
            {
                return null;
            }
        }
    }

    public String getInputMessage() {
        return inputMessage;
    }

    public void setInputMessage(String inputMsg) {
        this.inputMessage = inputMsg;

    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        if(this.product!=null)
            throw new IllegalStateException("Product is already set !!");
        this.product = product;

    }

    public boolean logSysdateInUTC()
    {
        String loginUTC = (String)getAttribute(CHOrderConstants.LOG_SYSDATE_IN_UTC);
        return Boolean.parseBoolean(loginUTC);
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;

    }
    public void setAttributeInMPC(String name,Object attribute)
    {
        try{
        this.mpc.set(name,attribute);
        }catch(Exception ex)
        {
            Debug.log(Debug.MSG_WARNING,"Could not set attribute in MPC via CHOrderContext name["+name+"] and attribute["+attribute+"]");
        }                                      
    }

    public CHOrderLoggerBase getOrderLogger() {
           return chOrderLoggerBaseObj;
    }

    public void setOrderLogger(CHOrderLoggerBase chOrderLoggerBase) {
        this.chOrderLoggerBaseObj = chOrderLoggerBase;

    }
}
