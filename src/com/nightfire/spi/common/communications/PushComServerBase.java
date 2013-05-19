/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.spi.common.communications;

import java.util.*;

import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;



/**
 * Push Server Base Class.
 * Handles message processing as result of remote request invocation from clients.
 */
public abstract class PushComServerBase extends ComServerBase
{
    /**
     * Name for property giving SERVER NAME stored in persistent-property table.
     */
    public static final String SERVER_NAME_PROP          = "SERVER_NAME";
    
    /**   
     * Name for property giving HOST NAME stored in persistent-property table.
     */
    public static final String HOST_NAME_PROP            = "HOST_NAME";
    
    /**
     * Name for property giving PORT NUMBER stored in persistent-property table.
     */
    public static final String PORT_NUMBER_PROP          = "PORT_NUMBER";

    /**
     * Name for property giving INTERFACE_VERSION stored in persistent-property table (iterative).
     */
    public static final String INTERFACE_VERSION_PROP    = "INTERFACE_VERSION";

    /**
     * Name for property giving SERVICE_PROVIDER stored in persistent-property table (iterative).
     */
    public static final String SERVICE_PROVIDER_PROP     = "SERVICE_PROVIDER";

    /**
     * Name for property giving OPERATION stored in persistent-property table (iterative).
     */
    public static final String OPERATION_PROP            = "OPERATION";

    /**
     * Name for property giving ASYNCHRONOUS stored in persistent-property table (iterative).
     */
    public static final String ASYNCHRONOUS_PROP         = "ASYNCHRONOUS";
    
    /**
     * Stores the Usage Descriptions
     */
    private ArrayList usageList                          = null;
    
    /**
     * Gets Persistent Property Value SERVER_NAME
     */
    protected String mServerName                         = null;
    
    /**
     * Gets Persistent Property value PORT_NUMBER
     */
    protected String mPortNumber                         = null;
    
    /**
     * Gets Persistent Property value HOST_NAME
     */
    protected String mHostName                           = null;

    
   /**
     * Constructor that creates comm server object and loads its properties.
     *
     * @param  key  Key value used to access configuration properties.
     * @param  type  Type value used to access configuration properties.
     *
     * @exception  ProcessingException  Thrown on initialization errors.
     */
    protected PushComServerBase (String key, String type) throws ProcessingException 
    {
        super(key, type);

        Debug.log(Debug.NORMAL_STATUS, "PushComServerBase: Initializing ...");
    }
    
    
   /**
     * Initialize configured usage.descriptions.
     * 
     * @exception   ProcessingException  Thrown upon errors.
     */
    protected void initUsageDescriptions() throws ProcessingException
    {
        // Loading list of Usage Descriptions
        String tempKey = null;
        usageList = new ArrayList();
        
        for (int iter = 0; true; iter++)  
        {
            tempKey =  PersistentProperty.getPropNameIteration(SERVICE_PROVIDER_PROP,iter);
            String serviceProviderProp = getPropertyValue(tempKey);
            
            //Breaks out of the loop if any Property is found NULL
            //Service Provider Name is a 'REQUIRED' field for any interface., so check for NULL on it.
            if (!StringUtils.hasValue(serviceProviderProp))
            {
                if (iter == 0)
                    throw new ProcessingException(StringUtils.getClassName(this) +
                        ": Service Provider property is not set.");
                else
                    break;
            }

            tempKey = PersistentProperty.getPropNameIteration(INTERFACE_VERSION_PROP,iter);
            String interfaceVersionProp = getPropertyValue(tempKey);

            tempKey=PersistentProperty.getPropNameIteration(OPERATION_PROP,iter);
            String OperationTypeProp = getRequiredPropertyValue(tempKey);

            tempKey=PersistentProperty.getPropNameIteration(ASYNCHRONOUS_PROP,iter);
            String trueOrFalse = getRequiredPropertyValue(tempKey);
            
            // Converts the String obtained from the Database to a boolean value
            boolean asynchronousProp = false;
 
            try
            {
                asynchronousProp = StringUtils.getBoolean(trueOrFalse);
            }
            catch (Exception e)
            {
                throw new ProcessingException(e);
            }
            
            // Adds usages to the vector
            usageList.add(new CommonUsageDescription(serviceProviderProp, interfaceVersionProp,
                                                     OperationTypeProp, asynchronousProp));
        }
    }


    /**
     * Handles processing of asynchronous requests.
     *
     * @param  header  Request header in XML format
     * @param  message  The request message to processed.
     *
     * @exception ProcessingException  Thrown if processing fails due to system error.
     * @exception  MessageException  Thrown if request message is invalid.
     */
    protected void processAsyncRequest(String header, String message)
        throws ProcessingException, MessageException
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
        {
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": the header passed to processAsync is:\n" + header);
            
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                           ": processAsync: Received message:\n " + message);
        }
        
        String[] drvConfig = getDriverConfiguration(header);

        if (isAsync()) 
            process(header, message, drvConfig[0], drvConfig[1]);
        else 
            throw new ProcessingException ("ERROR: PushComServerBase: " +
                "Asynchronous process method does not support synchronous processing.");

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Async Processing done.");                                     
    }

    
    /**
     * Handles processing of synchronous requests.
     *
     * @param  header  Request header in XML format
     * @param  message  The request message to processed.
     *
     * @return  The response message.
     *
     * @exception ProcessingException  Thrown if processing fails due to system error.
     * @exception  MessageException  Thrown if request message is invalid.
     */
    protected String processSyncRequest(String header, String request)
        throws ProcessingException, MessageException 
    {
        String result = null;
        
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
        {
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": the header passed to processSync is:\n" + header);
            
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                           ": processSync: Received message:\n " + request);
        }
        
        String[] drvConfig = getDriverConfiguration(header);

        if (!isAsync()) 
        {
            ResponseObject res = process(header, request, drvConfig[0], drvConfig[1]);

            result = Converter.getString(res.message);

            if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                Debug.log(Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                                               ": The response obtained from Process Sync method is:\n "
                                               + result);
        }
        else 
            throw new ProcessingException("ERROR: PushComServerBase: " +
                "Process sync method does not support asynchronous processing.");
        
        if (StringUtils.hasValue(result))
            return result;
        else
            throw new ProcessingException("Null Response Obtained");
    }

    /**
     * Handles processing of synchronous requests that provide a return value that
     * contains the response body and a response header.
     *
     * @param  header  Request header in XML format
     * @param  message  The request message to processed.
     *
     * @return  The response, which contains a header and message.
     * For more information on where the header is retrieved, refer to the process method in ComServerBase.
     *
     * @see ComServerBase#process(java.lang.String, java.lang.Object, java.lang.String, java.lang.String)
     *
     *
     * @exception ProcessingException  Thrown if processing fails due to system error.
     * @exception  MessageException  Thrown if request message is invalid.
     */
    protected MessageData processSynchronousRequest(String header, String request)
        throws ProcessingException, MessageException
    {
        // We return a MessageData Object here making an assumtion that at this point 
        // we will always be returning xml string headers and bodies.


        MessageData data;

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
        {
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": the header passed to processSynchronous is:\n" + header);

            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                           ": processSynchronous: Received message:\n " + request);
        }

        String[] drvConfig = getDriverConfiguration(header);

        if (!isAsync())
        {
            ResponseObject res =  process(header, request, drvConfig[0], drvConfig[1]);

            data = new MessageData(Converter.getString(res.header),
                                   Converter.getString(res.message) );

            if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE)) {
               Debug.log(Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                                               ": The response header obtained from Process Synchronous method is:\n "
                                               + data.header);
               Debug.log(Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                                               ": The response body obtained from Process Synchronous method is:\n "
                                               + data.body);
            }
        }
        else
            throw new ProcessingException("ERROR: PushComServerBase: " +
                "Process synchronous method does not support asynchronous processing.");

        //This is not a sufficient check on the response, but we would like this method to be
        //used for various types of responses.
        if (data != null && data.header != null && data.body != null )
            return data;
        else
            throw new ProcessingException("Null Response Obtained");
    }

    /**
     * Returns a list of usage-descriptions.
     */
    protected ArrayList getUsageList()
    {
        return usageList;
    }


    /**
     * Gets the Driver Configuration Based on the values in the header.
     *
     * @param   header  Header passed to one of the process methods from which
     *                  data will be extracted to obtain the driver configuration.
     * 
     * @return  Two-element string array, where the first element is the driver
     *          configuration key and the second element is the driver configuration type.
     * 
     * @exception ProcessingException  Thrown if configuration can't be obtained.
     */
    protected String[] getDriverConfiguration(String header) throws ProcessingException 
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Getting driver configuration ...");
        
        String[] result = new String[2];
        String operation = null;

        if ( StringUtils.hasValue( header ) )
        {
            //Creates a Message Parser Object for the "header" argument
            try 
            {
                XMLMessageParser parser = new XMLMessageParser( header );
                
                if ( parser.exists( OPERATION_PROP ) )
                    operation = parser.getValue( OPERATION_PROP );
            } 
            catch (MessageParserException mpe) 
            {
                throw new ProcessingException(mpe.getMessage());
            }
        }

        if ( StringUtils.hasValue( operation ) )
        {
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": The OPERATION element obtained from header is [" 
                          + operation + "]\nChecking the usage descriptions for operation value ...");
            
            for (int iter = 0; true;  iter ++)
            {
                String operationTypeProp = getPropertyValue(PersistentProperty.getPropNameIteration(OPERATION_PROP,iter));
                
                // We're done if no more operation types are available.
                if (!StringUtils.hasValue(operationTypeProp)) 
                    break;
                
                //Gets the corresponding Driver Key and Type for that Particular 
                //Operation requested
                if (Debug.isLevelEnabled(Debug.IO_STATUS))
                    Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Comparing header op-type [" 
                              + operation +"] usage descriptions opt-type [" 
                              + operationTypeProp +"] at iteration [" +iter +"]");
                
                if (operationTypeProp.equals(operation)) 
                {
                    if (Debug.isLevelEnabled(Debug.IO_STATUS))
                        Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                  ": Matching OPERATION found.\nThe asynchronous flag for this operation is set to [" +
                                  isAsync() +"]");
                    
                    result[0] = getRequiredPropertyValue(PersistentProperty.getPropNameIteration(MPD_KEY_PROP, iter));
                    result[1] = getRequiredPropertyValue(PersistentProperty.getPropNameIteration(MPD_TYPE_PROP, iter));
                    
                    if (Debug.isLevelEnabled(Debug.IO_STATUS))
                        Debug.log(Debug.DB_DATA, StringUtils.getClassName(this) + ": Key and Type for driver obtained are [" 
                                  + result[0] + "] and [" + result[1] + "]");
                    
                    // Break out of the loop once the driver key and type are available.
                    return result;
                }
            }
        }
        
        // Matching operation cannot be found. Try getting driver configuration with deafult parameters.
        result[0] = getPropertyValue(MPD_KEY_PROP);

        if ( !StringUtils.hasValue( result[0] ) )
            result[0] = getPropertyValue(PersistentProperty.getPropNameIteration(MPD_KEY_PROP, 0));

        result[1] = getPropertyValue(MPD_TYPE_PROP);
        
        if ( !StringUtils.hasValue( result[1] ) )
            result[1] = getPropertyValue(PersistentProperty.getPropNameIteration(MPD_TYPE_PROP, 0));

        if (StringUtils.hasValue(result[0]) && StringUtils.hasValue(result[1]))
            return result;
        else
            throw new ProcessingException(StringUtils.getClassName(this) + ": Cannot find matching operation for [" + operation + "].");
    }
    
    
    /**
     * Get type of processing supported (synchronous/asynchronous).
     * 
     * @return True if asynchronous, otherwise false.
     */
    public abstract boolean isAsync();
    
    
    /**
     * Usage Description class
     * Common between CORBA and RMI implementations
     */
    final protected class CommonUsageDescription
    {
        private String serviceProvider;
        private String interfaceVersion;
        private String OperationType;
        private boolean asynchronous;
        
        private CommonUsageDescription() 
        {
        }
        
        public CommonUsageDescription(String serviceProvider,
                                      String interfaceVersion,
                                      String OperationType,
                                      boolean asynchronous) 
        {
            this.serviceProvider = serviceProvider;
            this.interfaceVersion = interfaceVersion;
            this.OperationType = OperationType;
            this.asynchronous = asynchronous;
        }
        
        
        public String getServiceProvider()
        {
            return serviceProvider;
        }
        
        
        public String getInterfaceVersion()
        {
            return interfaceVersion;
        }
        
        
        public String getOperationType()
        {
            return OperationType;
        }
        
        
        public boolean isAsynchronous()
        {
            return asynchronous;
        }
    }
}
