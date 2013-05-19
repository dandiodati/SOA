/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.rmi;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

import com.nightfire.rmi.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.spi.common.supervisor.*;


/**
 * RMI Server for use in async/sync gateways that talk via text data.
 */
public class RMIRequestHandlerImpl extends PushComServerBase
    implements RMIRequestHandler
{ 
    /**
     * Property Key for EVENT_CHANNEL_NAME in PersistentProperty Table
     */
    protected static final String EVENT_CHANNEL_PROP = "EVENT_CHANNEL_NAME";

    /**
     * Name of property indicating whether server is asynchronous (default) or synchronous.
     */
    public static final String IS_ASYNCHRONOUS_PROP = "IS_ASYNCHRONOUS";


    /**
     * Creates an RMI server object.
     * 
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public RMIRequestHandlerImpl(String key, String type)
        throws ProcessingException
    { 
        super(key, type);
        
        Debug.log(Debug.NORMAL_STATUS,"RMIRequestHandlerImpl: Initializing the RMI Communications Server ...");
        
        // Initialize Usage Descriptions
        initUsageDescriptions();

        // Get the RMI configuration properties.
        StringBuffer sb = new StringBuffer();

        eventChannelName = getRequiredPropertyValue(EVENT_CHANNEL_PROP, sb);

        mServerName = getRequiredPropertyValue(SERVER_NAME_PROP, sb);
        mHostName	= getRequiredPropertyValue(HOST_NAME_PROP, sb);
        mPortNumber = getRequiredPropertyValue(PORT_NUMBER_PROP, sb);

        String temp = getPropertyValue( IS_ASYNCHRONOUS_PROP );

        if ( StringUtils.hasValue( temp ) )
        {
            try
            {
                asyncFlag = StringUtils.getBoolean( temp );
            }
            catch ( Exception e )
            {
                sb.append( e.getMessage() );
            }
        }

        if (sb.length() > 0) 
            throw new ProcessingException(StringUtils.getClassName(this) + 
                                          ": Server Name, Host Name and Port Number are not specified in properties.\n" +
                                          sb.toString());
        
        serviceName = "//" + mHostName + ":" + mPortNumber + "/" + mServerName;

        //Creates a Event Supplier
        try 
        {
            Debug.log(Debug.MSG_STATUS, this.getClass().getName() + ": Creating an EventPushSupplier object ...");
            
            supplier = new EventPushSupplier(Supervisor.getSupervisor().getCPL().getORB(), eventChannelName , false);
        }
        catch (CorbaException e) 
        {
            Debug.error( StringUtils.getClassName(this) + 
                         ": Could not create an event push supplier.\n" +
                         e.toString());

            throw new ProcessingException(e);
        }
    }

    
    /**
     * Implements the disconnect operation with the event supplier
     */
    public void disconnect_push_supplier() 
    {
        supplier.disconnect();
    }

    
    /**
     * Registers the Objects and waits for the client objects to call methods
     */
    public void run() 
    {
        Debug.log(Debug.NORMAL_STATUS, StringUtils.getClassName(this) + 
                  ": Exporting RMI object [" + serviceName + "].");
        try 
        {
            // May want to pass port in the future.
            UnicastRemoteObject.exportObject(this);

            Naming.rebind(serviceName, this);	

            Debug.log(Debug.NORMAL_STATUS, StringUtils.getClassName(this) + 
                      ": RMI server [" + serviceName + "] now available for requests ...");
        } 
        catch (Exception e) 
        {
            Debug.error( StringUtils.getClassName(this) + 
                         ": Couldn't initialize RMI server:\n" + e.toString());
        }

        postEvents(mServerName + "." + ComServerBase.STARTUP_TAG);
    }
    

    /**
     * Implements shutdown operation
     */
    public void shutdown() 
    {
        Debug.log(Debug.NORMAL_STATUS, StringUtils.getClassName(this) + 
                  ": Shutting down server " + serviceName);

        postEvents(mServerName + "." + ComServerBase.SHUTDOWN_TAG);

        try
        {
            supplier.disconnect();
        }
        catch (Exception e)
        {
            Debug.error( StringUtils.getClassName(this) + ": " + e.getMessage());
        }

        try 
        {
            //Remove all References from the Naming Service
            Debug.log( Debug.IO_STATUS, "Unbinding RMI object from registry ..." );
            Naming.unbind(serviceName);
        } 
        catch (Exception e) 
        {
            Debug.error( StringUtils.getClassName(this) + 
                         ": Couldn't unbind RMI server:\n" + e.toString());
        }


        try
        {
            Debug.log( Debug.IO_STATUS, "Unexporting RMI object ..." );
            UnicastRemoteObject.unexportObject( this, false );
        }
        catch(NoSuchObjectException nsoex)
        {
            Debug.error("Could not unexport RMI Server: " + nsoex );
        }

        Debug.log(Debug.NORMAL_STATUS, StringUtils.getClassName(this) + 
                  ": Successfully shutdown communications RMI server.");
    }

    
    /**
     * Get type of processing supported (synchronous/asynchronous).
     * 
     * @return True if asynchronous, otherwise false.
     */
    public boolean isAsync ( )
    {
        return asyncFlag;
    }
    
    
    /**
     * Test to see if a particular usage is supported.
     *
     * @param  usage  UsageDescription object containing usage in question.
     *
     * @return 'true' if usage is supported, otherwise false.
     */
    public boolean supportsUsage(UsageDescription usage) 
    {
        //Gets all the descriptions for the particular server
        UsageDescription[] rmiUsage = getUsageDescriptions();
        
        // Loop over all usages, checking for a match by comparing ServiceProviderName, 
        // OperationType, Version and Asynchronous.
        for (int i = 0; i < rmiUsage.length; i++) 
        {
            if (rmiUsage[i].operationType.equals(usage.operationType) &&
                rmiUsage[i].serviceProvider.equals(usage.serviceProvider) &&
                rmiUsage[i].interfaceVersion.equals(usage.interfaceVersion) &&
                rmiUsage[i].asynchronous == usage.asynchronous)
                return true;
        }
        
        return false;
    }


    /**
     * Method used to get all interfaces supported by a given gateway.
     *
     * @return An array of UsageDescription objects.
     */
    public UsageDescription[] getUsageDescriptions() 
    {
        ArrayList usageList = getUsageList();
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                      ": The total number of usages is " +
                      usageList.size());
        
        UsageDescription[] rmiUsage = new UsageDescription[usageList.size()];

        for(int i=0 ; i < usageList.size(); i++)
        {
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                          ": Creating the " + (i+1) +"th usage.");

            CommonUsageDescription common = (CommonUsageDescription) usageList.get(i);
            rmiUsage[i] = new UsageDescription(common.getServiceProvider(),
                                               common.getInterfaceVersion(),
                                               common.getOperationType(),
                                               common.isAsynchronous());
        }

        return rmiUsage;
    }


    /**
     * Method providing asynchronous processing of text data.
     *
     * @param  header  Message header.
     * @param  request Message body containing text data.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     */
    public void processAsync ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                      ": The header passed to processAsync is:\n" + header);

        try
        {
            processAsyncRequest( header, request );
        }
        catch (ProcessingException e) 
        {
            Debug.error( StringUtils.getClassName(this) + 
                         ": processAsync: " + e.getMessage());

            throw new RMIServerException (RMIServerException.UnknownError, e.getMessage());
        } 
        catch (MessageException e) 
        {
            Debug.error( "ERROR: RMIRequestHandlerImpl.processAsync: " +
                         e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, 
                                              e.getMessage());
        }
    }

    
    /**
     * Method providing synchronous processing of text data.
     *
     * @param  header  Message header.
     * @param  request Message body containing text data.
     *
     * @return The synchronous response.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public String processSync ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                      ": The header passed to processSync is:\n" + header);

        try
        {
            String response = processSyncRequest(header, request);

            return response;
        }
        catch (ProcessingException e) 
        {
            Debug.error( StringUtils.getClassName(this) + 
                         ": processSync: processing error: " + 
                         e.getMessage());
            
            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        } 
        catch (ClassCastException e)
        {
            Debug.error( StringUtils.getClassName(this) +
                         ": processSync: Invalid data type returned. Error: " +
                         e.getMessage());

            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        }
        catch (MessageException e) 
        {
            Debug.error( StringUtils.getClassName(this) + 
                         ": processSync: invalid data error: " + 
                         e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, e.getMessage());
        }
    }
    

    /**
     * Method providing synchronous processing of text data, with headers in and out.
     *
     * @param  header  Message header.
     * @param  request Message body containing text data.
     *
     * @return A response-pair object containing the response header and body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public ResponsePair processSynchronous ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                      ": the header passed to processSynchronous is:\n" + header);

        try
        {
            MessageData data = processSynchronousRequest(header, request);

            ResponsePair rp = new ResponsePair( );

            rp.header  = data.header;
            rp.message = data.body;

            return rp;
        }
        catch (ProcessingException e)
        {
            Debug.error( StringUtils.getClassName(this) +
                         ": processSynchronous: processing error: " +
                         e.getMessage());

            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        }
        catch (ClassCastException e)
        {
            Debug.error( StringUtils.getClassName(this) +
                         ": processSynchronous: Invalid data type returned. Error: " +
                         e.getMessage());

            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        }
        catch (MessageException e)
        {
            Debug.error( StringUtils.getClassName(this) +
                         ": processSynchronous: invalid data error: " +
                         e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, e.getMessage());
        }
    }


    /**
     * Sends STARTUP/SHUTDOWN events to the event channel
     */
    private synchronized void postEvents(String eventString)
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": " +
                      "Posting Event [" + eventString + "] to Event Channel ...");

        try 
        {
            supplier.pushEvent(eventString);
        }
        catch ( Exception e ) 
        {
            Debug.error( StringUtils.getClassName(this) + ": Failed posting event [" 
                         + eventString + "] to notifications channel.");
        }
    }


    //Push Supplier for publishing startup and shutdown Events
    private EventPushSupplier supplier = null;
    //Gets Persistent Property value EVENT_CHANNEL_NAME
    protected String eventChannelName = null;

    private boolean asyncFlag = true;
    private String serviceName = null;
}
