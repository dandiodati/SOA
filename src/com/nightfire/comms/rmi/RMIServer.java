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
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.communications.*;


/**
 * RMI Server for use in gateways.
 */
public abstract class RMIServer extends PushComServerBase
    implements RMIRequestHandler
{ 
    private String serviceName = null;
    
    /**
     * Creates an RMI server object.
     * 
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public RMIServer(String key, String type)
        throws ProcessingException
    { 
        super(key, type);
        
        Debug.log(Debug.NORMAL_STATUS,"RMIServer: Initializing the RMI Communications Server ...");
        
        // Initialize Usage Descriptions
        initUsageDescriptions();

        // Get the RMI configuration properties.
        StringBuffer sb = new StringBuffer();
        mServerName = getRequiredPropertyValue(SERVER_NAME_PROP, sb);
        mHostName	= getRequiredPropertyValue(HOST_NAME_PROP, sb);
        mPortNumber = getRequiredPropertyValue(PORT_NUMBER_PROP, sb);

        if (sb.length() > 0) 
            throw new ProcessingException(StringUtils.getClassName(this) + 
                ": Server Name, Host Name and Port Number are not specified in properties.\n" +
                sb.toString());
        
        serviceName = "//" + mHostName + ":" + mPortNumber + "/" + mServerName;
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
        } 
        catch (Exception e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                        ": Couldn't initialize RMI server:\n" + e.toString());
        }
    }
    

    /**
     * Implements shutdown operation
     */
    public void shutdown() 
    {
        Debug.log(Debug.NORMAL_STATUS, StringUtils.getClassName(this) + 
                                       ": Shutting down server " + serviceName);
        try 
        {
            //Remove all References from the Naming Service
            Naming.unbind(serviceName);
        } 
        catch (Exception e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                        ": Couldn't unbind RMI server:\n" + e.toString());
        }


        try{
            Debug.log( Debug.IO_STATUS, "Unexporting RMI object ..." );
            UnicastRemoteObject.unexportObject( this, false );
        }
        catch(NoSuchObjectException nsoex){

            Debug.error("Could not unexport RMIServer: "+
                        nsoex);

        }

        Debug.log(Debug.NORMAL_STATUS, StringUtils.getClassName(this) + 
                                       ": Successfully shutdown communications RMI server.");
    }

    
    /**
     * Tests whether a given particular usage (interface) is supported
     * by matching the Operation Type i.e, AddressValidation.
     *
     * @param UsageDescription UsageDescription sent by the client
     *
     * @return true when usage is available
     */
    public boolean supportsUsage(RMIRequestHandler.UsageDescription usage)
        throws java.rmi.RemoteException
    {
        //Gets all the descriptions for the particular server
        RMIRequestHandler.UsageDescription[] rmiUsage = getUsageDescriptions();
        
        // Loop over all usages, checking for a match by comparing ServiceProviderName, 
        // OperationType, Version and Asynchronous.
        for (int i=0; i < rmiUsage.length; i++) 
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
     * Gets all the usages (Interfaces) supported by the given SPI
     *
     * @return  An array of UsageDescription objects.
     */
    public RMIRequestHandler.UsageDescription[] getUsageDescriptions() 
        throws java.rmi.RemoteException
    {
        ArrayList usageList = getUsageList();
        
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                       ": The total number of usages is " +
                                       usageList.size());
        
        RMIRequestHandler.UsageDescription[] rmiUsage = new RMIRequestHandler.UsageDescription[usageList.size()];

        for(int i=0 ; i < usageList.size(); i++)
        {
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                           ": Creating the " + (i+1) + "th usage.");

            CommonUsageDescription common = (CommonUsageDescription) usageList.get(i);
            rmiUsage[i] = new RMIRequestHandler.UsageDescription(common.getServiceProvider(),
                                                                 common.getInterfaceVersion(),
                                                                 common.getOperationType(),
                                                                 common.isAsynchronous());
        }

        return rmiUsage;
    }


    /**
     * Handles Asynchronous Processing
     *
     * @param  header Header in XML format
     * @param  message The message which needs to be processed
     *
     * @exception RemoteException if there is no asyncronous response from server
     * @exception RMIServerException if request processing fails
     * @exception RMIInvalidDataException if it is a bad request
     */
    public void processAsync(String header, String message) 
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
    {
        try
        {
            processAsyncRequest(header, message);
        }
        catch (ProcessingException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                        ": processAsync: " + e.getMessage());

            throw new RMIServerException (RMIServerException.UnknownError, e.getMessage());
        } 
        catch (MessageException e) 
        {
            Debug.log(Debug.ALL_ERRORS, "ERROR: RMIServer.processAsync: " +
                                        e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, 
                e.getMessage());
        }
    }

    
    /**
     * Handles Synchronous Processing
     *
     * @param header Header in XML format
     * @param message  The message which needs to be processed
     *
     * @return String result of processing
     *
     * @exception RemoteException if there is no syncronous response from server
     * @exception RMIServerException if request processing fails
     * @exception RMIInvalidDataException if it is a bad request
     */
    public String processSync(String header, String request)
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException 
    {
        String result = null;
        
        try
        {
            result = processSyncRequest(header, request);
        }
        catch (ProcessingException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                        ": processSync: processing error: " + 
                                        e.getMessage());
            
            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        } 
        catch (MessageException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                        ": processSync: invalid data error: " + 
                                        e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Handles Synchronous Processing for requests that require a return value consisting
     * of a header and a body.
     *
     * @param header Header in XML format
     * @param message  The message which needs to be processed
     *
     * @return A RMIRequestHandler.ResponsePair object containing the result of processing
     *
     * @exception RemoteException if there is no syncronous response from server
     * @exception RMIServerException if request processing fails
     * @exception RMIInvalidDataException if it is a bad request
     */
    public RMIRequestHandler.ResponsePair processSynchronous ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        RMIRequestHandler.ResponsePair result = new  RMIRequestHandler.ResponsePair();
        
        try
        {
            MessageData data = processSynchronousRequest(header, request);

             result.header = data.header;
             result.message = data.body;

        }
        catch (ProcessingException e)
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) +
                                        ": processSynchronous: processing error: " +
                                        e.getMessage());

            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        }
        catch (ClassCastException e)
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) +
                                        ": processSynchronous: Invalid data type returned. Error: " +
                                        e.getMessage());

            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        }
        catch (MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) +
                                        ": processSynchronous: invalid data error: " +
                                        e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, e.getMessage());
        }

        if ( !StringUtils.hasValue ( result.header ) || !StringUtils.hasValue ( result.message ) )
        {
            String errMsg = "Invalid response header or response body: Response header = [" + result.header +
            "], Response body = [" + result.message + "].";
            throw new RMIServerException (RMIServerException.UnknownError, errMsg );
        }

        return result;

    }
}
