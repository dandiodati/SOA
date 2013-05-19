package com.nightfire.comms.jms.handler;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.comms.handlers.RequestHandlerBase;

public class JMSConsumerRequestHandler extends RequestHandlerBase
{
    private String driverKey;
    
    private String driverType;
    
    /**
     * creates a new JMSConsumerRequestHandler.
     * @param driverKey as String
     * @param driverType as String
     */
    public JMSConsumerRequestHandler(String driverKey, String driverType) {
        super();
        this.driverKey = driverKey;
        this.driverType = driverType;
    }

    /**
     * creates a new JMSConsumerRequestHandler.
     * @param driverKey as String
     * @param driverType as String
     * @param instanceId as String
     */
    public JMSConsumerRequestHandler(String driverKey, String driverType,String instanceId) {
    	this(driverKey,driverType);
    	super.setInstanceId(instanceId);
   }

    /**
     * initializes the driver key and type which are used to identify the message processing chain.
     * 
     * @param header CH header 
     */
    protected void initializeDriverKeyType(String header, String request) throws ProcessingException 
    {
        super.setDriverKey(driverKey);
        super.setDriverType(driverType);
        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, 
                    "Fetched DriverKey [" + driverKey + "] DriverType[" + driverType + "].");
        
        if( !StringUtils.hasValue(driverKey) || !StringUtils.hasValue(driverType) )
        {
            throw new ProcessingException("DriverKey or DriverType found null");
        }
    }
}
