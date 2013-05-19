/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.corba;

import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.corba.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.spi.common.supervisor.*;

import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;

import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;


/**
 * Corba Server Class Which handles message processing through corba clients and message processors
 */
public abstract class CorbaServer extends PushComServerBase implements RequestHandlerOperations
{
    /**
     * Property Key for EVENT_CHANNEL_NAME in PersistentProperty Table
     */
    protected static final String EVENT_CHANNEL_PROP = "EVENT_CHANNEL_NAME";

    //Gets the request handler object
    protected RequestHandlerPOA handler              = null;

    //Initializes corba service
    protected CorbaPortabilityLayer cpl              = null;

    //Gets Persistent Property value EVENT_CHANNEL_NAME
    protected String eventChannelName                = null;

    //Naming Service Subscriber
    private ObjectLocator objLocator                 = null;

    //Push Supplier for publishing startup and shutdown Events
    private EventPushSupplier supplier               = null;

    
    /**
     * Constructor
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public CorbaServer (String key, String type) throws ProcessingException 
    {
        super(key, type);

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, 
                    "CorbaServer: Initializing the Communications Corba Server");

        // Initialize Usage Descriptions
        initUsageDescriptions();
        
        // The Properties are obtained from a protected Hashtable Object
        StringBuffer errorStrBuf = new StringBuffer();
        mServerName =  getRequiredPropertyValue(SERVER_NAME_PROP, errorStrBuf);
        eventChannelName = getRequiredPropertyValue(EVENT_CHANNEL_PROP, errorStrBuf);

        if (errorStrBuf.length() > 0)
            throw new ProcessingException(StringUtils.getClassName(this) + 
                ": Event Channel and Server Name not specified in persistent properties.\n" +
                errorStrBuf.toString());
        
        //Initialize Corba Service
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                       ": Getting the CorbaPortabilityLayer from Supervisor... ");
        cpl = getCPL();
        
        //Create a request handler object
        handler = new RequestHandlerPOATie(this);

        try
        {
            cpl.activateObject(mServerName, handler);
        }
        catch (CorbaException e)
        {
            String errorMessage = "ERROR: CorbaServer.CorbaServer(String, String): Failed to activate server [" + mServerName + "]:\n" + e.toString();
            
            Debug.error(errorMessage);
            
            throw new ProcessingException(errorMessage);
        }
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, StringUtils.getClassName(this) + 
                                       ": Registered to corba with name [" + mServerName +"]");
        
        //Creates a Naming Service Object
        objLocator = new ObjectLocator(cpl.getORB());
        
        //Exports the Object Name to Naming Service
        try 
        {
            //Exporting the SERVER_NAME obtained from the Persistent Property Table
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                           ": Exporting server name [" + mServerName + 
                                           "] to the Naming Service ...");
            
            objLocator.add(mServerName, cpl.getObjectReference(handler));
        }
        catch (CorbaException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                        ": Could not export the object name to the Naming Service.\n" +
                                        e.toString());
            throw new ProcessingException(e);
        }
        
        //Creates a Event Supplier
        try 
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, this.getClass().getName() + "." + this.getClass().getName() + "(): Creating an EventPushSupplier object ...");
            
            supplier = new EventPushSupplier(cpl.getORB(), eventChannelName , false);
        }
        catch (Throwable e)
        {
            Debug.log(Debug.ALL_WARNINGS, StringUtils.getClassName(this) +
                                        ": Could not create an event push supplier for channel [" + eventChannelName + "]. " +
                                        e.toString());
        }
    }
    

    /**
     * Implements the disconnect operation with the event supplier
     */
    public void disconnect_push_supplier() 
    {
        if (supplier != null)
            supplier.disconnect();
    }

    
    /**
     * Tests whether a given particular usage (interface) is supported
     * by matching the Operation Type i.e, AddressValidation
     * @param usage UsageDescription sent by the client
     * @return true when usage is available
     */
    public boolean supportsUsage(UsageDescription usage) 
    {
        //Gets all the descriptions for the particular server
        UsageDescription[] idlUsage = getUsageDescriptions();
        
        // Loop over all usages, checking for a match by comparing ServiceProviderName, 
        // OperationType, Version and Asynchronous.
        for (UsageDescription idlUsageObj: idlUsage)
        {
            if (idlUsageObj.OperationType.equals(usage.OperationType) &&
                idlUsageObj.serviceProvider.equals(usage.serviceProvider) &&
                idlUsageObj.interfaceVersion.equals(usage.interfaceVersion) &&
                idlUsageObj.asynchronous == usage.asynchronous)
                return true;
        }
        
        return false;
    }

    
    /**
     *    Gets all the usages (Interfaces) supported by the given SPI
     */
    public UsageDescription[] getUsageDescriptions() 
    {
        ArrayList usageList = getUsageList();
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                       ": The total number of usages is " +
                                       usageList.size());
        
        UsageDescription[] idlUsage = new UsageDescription[usageList.size()];

        for(int i=0 ; i < usageList.size(); i++)
        {
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                           ": Creating the " + (i+1) +"th usage.");

            CommonUsageDescription common = (CommonUsageDescription) usageList.get(i);
            idlUsage[i] = new UsageDescription(common.getServiceProvider(),
                                               common.getInterfaceVersion(),
                                               common.getOperationType(),
                                               common.isAsynchronous());
        }

        return idlUsage;
    }


    /**
     * Registers the Objects and waits for the client objects to call methods
     *
     */
    public void run() 
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": " +
                                       "Posting a Startup Event into Persistent Event Channel ...");
        try
        {
            postEvents(mServerName + "." + ComServerBase.STARTUP_TAG);
        }
        catch (CorbaServerException cse)
        {
            Debug.log (Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": " + cse.getMessage());
        }

        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, 
                    StringUtils.getClassName(this) + ": Waiting for client messages");
        
        waitForClients();
    }


    /**
     * Implements shutdown operation
     */
    public void shutdown() 
    {
        String shutdownEvent = mServerName + "." + ComServerBase.SHUTDOWN_TAG;

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": " +
                                       "Posting a Server Shutdown event ..." +
                                       shutdownEvent);
        try
        {
            postEvents(shutdownEvent);
        }
        catch (CorbaServerException cse)
        {
            Debug.log (Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": " + cse.getMessage());
        }

        if (supplier != null)
            supplier.disconnect();

        //Remove all References from the Naming Service
        try 
        {
            //Remove the final naming context
            objLocator.remove(mServerName);
        }
        catch (CorbaException ce) 
        {
            Debug.log (Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                         ": Cannot remove the final naming context" +
                                         ce.toString());
        }

        try
        {
            cpl.deactivateObject(handler);
        }
        catch (Exception e)
        {
            Debug.error("ERROR: CorbaServer.shutdown(): Unable to deactivate the RequestHandlerPOA instance.\n" + e.getMessage());
        }

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": " +
                                       "Successfully shutdown communications corba server.");
    }


    /**
     * Handles Asynchronous Processing
     *
     * @param  header Header in XML format
     *
     * @param  message The message which needs to be processed
     *
     * @exception InvalidDataException if it is a bad request
     * @exception CorbaServerException if is a server side error occurs
     */
    public void processAsync(String header, String message) 
        throws InvalidDataException, CorbaServerException 
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Starting Async Processing...");                                     
        
        try
        {
            processAsyncRequest(header, message);
        }
        catch (ProcessingException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": Asynchronous processing error: " + e.getMessage());
            throw new CorbaServerException(CorbaServerExceptionType.UnknownError, e.toString());
        }
        catch (MessageException e) 
        {
            // do not log BR error message again, as it should have been already logged by RuleProcessor
            if (ExceptionUtils.isBRError(e))
            {
                Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": Asynchronous processing invalid data error: BR error.");
            }
            else
            {
                Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": Asynchronous processing invalid data error: " + e.getMessage());
            }

            throw new InvalidDataException(InvalidDataExceptionType.UnknownDataError, " ", e.getMessage());
        }
        
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Async Processing completed.");                                     
    }

    
    /**
     * Handles Synchronous Processing
     *
     * @param header Header in XML format
     *
     * @param request  The message which needs to be processed
     *
     * @param response  Can be assigned a value , acts as return parameter for corba interface methods
     *
     * @exception InvalidDataException if it is a bad request
     * @exception CorbaServerException if a server side error occurs
     */
    public void processSync(String header, String request, org.omg.CORBA.StringHolder response)
        throws InvalidDataException, CorbaServerException 
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Starting Sync Processing...");                                     
        
        try
        {
            response.value = processSyncRequest(header, request);
        }
        catch (ProcessingException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": Synchronous processing error: " + e.getMessage());
            throw new CorbaServerException(CorbaServerExceptionType.UnknownError, e.toString());
        }
        catch (MessageException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": Synchronous processing invalid data error: " + e.getMessage());
            throw new InvalidDataException(InvalidDataExceptionType.UnknownDataError, " ", e.getMessage());
        }
        
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Sync Processing completed.");                                     
    }


     /**
     * Handles Synchronous Processing with response header support
     *
     * @param requestHeader Request Header in XML format
     *
     * @param request  The message which needs to be processed
     *
     * @param responseHeader  Holds the response header that gets returned.
     * @param response       Holds the response message that gets returned.
     *
     * @exception InvalidDataException if it is a bad request
     * @exception CorbaServerException if a server side error occurs
     *
     */
    public void processSynchronous (String requestHeader,
                                    String request,
                                    org.omg.CORBA.StringHolder responseHeader,
                                    org.omg.CORBA.StringHolder response) 
        throws InvalidDataException, CorbaServerException
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Starting Synchronous Processing...");
        
        try
        {

            MessageData data  =  processSynchronousRequest(requestHeader, request);
            responseHeader.value = data.header;
            response.value       = data.body;
        }
        catch (ProcessingException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": Synchronous processing error: " + e.getMessage());
            throw new CorbaServerException(CorbaServerExceptionType.UnknownError, e.toString());
        }
        catch (MessageException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + ": Synchronous processing invalid data error: " + e.getMessage());
            throw new InvalidDataException(InvalidDataExceptionType.UnknownDataError, " ", e.getMessage());
        }
        
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Synchronous Processing completed.");
    }


    
    /**
     * Waits for Clients to call methods,
     * 
     */
    public void waitForClients() 
    {
        try
        {
            cpl.activatePOAManager();
        
            cpl.run();
        }
        catch (Exception e)
        {
            Debug.error("ERROR: CorbaServer.waitForClients(): Failed to start the Corba server:\n" + e.getMessage());
        }
    }
    
    
    /**
     * Gets Corba Portability Layer from Supervisor
     *
     * @return CorbaPortabilityLayer object of CorbaPortabilityLayer
     *
     * @throws ProcessingException throw ProcessingException 
     *
     * This method can be overwritten by child classes to get 
     * CPL by other means than from Supervisor.
     */
    protected CorbaPortabilityLayer getCPL() throws ProcessingException
    {
        return Supervisor.getSupervisor().getCPL();
    }


    /**
     * Sends STARTUP/SHUTDOWN events to the event channel
     *
     * @param eventString event String
     *
     * @throws CorbaServerException throw CorbaServerException
     */
    private synchronized void postEvents(String eventString)
        throws CorbaServerException 
    {
        try 
        {
            if (supplier != null)
                supplier.pushEvent(eventString);
        }
        catch (Throwable e)
        {
            Debug.warning(StringUtils.getClassName(this) +
                    ": Failed while sending events to the channel. [" +eventString
                    + "]. Ignoring start/stop event posting exception :" + e.getMessage());
        }
    }
}
