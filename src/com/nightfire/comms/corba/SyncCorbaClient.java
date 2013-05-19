/*
 * Copyright(c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.corba;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.spi.common.communications.*;

import com.nightfire.framework.corba.*;
import com.nightfire.spi.common.supervisor.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;

import com.nightfire.spi.common.driver.MessageProcessorContext;

/** Synchronous Corba Client invokes synchronous
 * processing method of Server Objects
 */
public class SyncCorbaClient extends MessageProcessorBase 
{
    /**
     * Loads ServerName from Persistent Property
     */
    public static final String SERVER_NAME_PROP = "SERVER_NAME";

    private String serverName;
    
    /**
     * Constructor
     */
    public SyncCorbaClient() 
    {
    }


    /**
     * Initializes this object given the Key  and Type
     * @param key  property key to load persistent properties
     * @param type  property type to load persistent properties
     * @exception ProcessingException Throws when Initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException 
    {
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG,"Configuring SyncCorbaClient ...");

        super.initialize(key,type);
        
        serverName =(String) adapterProperties.get(SERVER_NAME_PROP);
        
        if (serverName==null) 
            throw new ProcessingException("ERROR SyncCorbaClient: NULL values found for configuration properties");


        if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS,"S Y N C     C O R B A    C L I E N T    I N I T    E N D ");
    }


    /**
     * Process the input message(DOM or String) and(optionally) return
     * a value.
     * 
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  Optional output message, or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public NVPair[] execute(MessageProcessorContext mpcontext,java.lang.Object input)
        throws ProcessingException, MessageException
    {
        org.omg.CORBA.StringHolder response = new org.omg.CORBA.StringHolder("");

        CorbaPortabilityLayer      cpl;
        
        try
        {
            cpl = new CorbaPortabilityLayer(null, null, null);
        }
        catch (Exception e)
        {
            throw new ProcessingException("ERROR: SyncCorbaClient.execute(): Failed to create a CorbaPortabilityLayer object:\n" + e.getMessage());    
        }
        
        org.omg.CORBA.ORB orb = cpl.getORB();

        org.omg.CORBA.Object server= null;

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, "SYNCCORBASERVER -> CONFIG : Sync Server Name is " +serverName);
        
        if(input == null) return null;

        if(input instanceof String)
        {
            try 
            {
                ObjectLocator objLocator = new ObjectLocator(orb);
                server = objLocator.find(serverName);
            }
            catch(CorbaException e) 
            {
                throw new ProcessingException(e.getMessage());
            }
            
            RequestHandler handler = RequestHandlerHelper.narrow(server);
            
            try 
            {
                String header = CustomerContext.getInstance().propagate( (String)null );

                if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
                    Debug.log( Debug.IO_STATUS, "Header value:\n" + header );

                handler.processSync( header, (String)input, response );
            }
            catch(Exception e) 
            {
                
                Debug.log(Debug.ALL_ERRORS, "SyncCorbaClient -> Error: Failed during processing Reason: " +e.toString());
            }
        }    
        else
        {
            
            throw new ProcessingException("ERROR: SyncCorbaClient: Only String Type Inputs are currently supported");
        }

        if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS,"SYNCCORBACLIENT -> DATA: The response Obtained from ProcessSync is " +response.value);

        return formatNVPair(response.value);
    }
    
}
