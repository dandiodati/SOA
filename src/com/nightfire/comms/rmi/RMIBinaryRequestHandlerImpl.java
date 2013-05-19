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
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.communications.*;


/**
 * RMI Server for use in async/sync gateways that process binary data.
 */
public class RMIBinaryRequestHandlerImpl extends PushComServerBase
    implements RMIBinaryRequestHandler
{ 
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
    public RMIBinaryRequestHandlerImpl(String key, String type)
        throws ProcessingException
    { 
        super(key, type);
        
        Debug.log(Debug.NORMAL_STATUS,"RMIBinaryRequestHandlerImpl: Initializing the Binary RMI Communications Server ...");
        
        // Get the RMI configuration properties.
        StringBuffer sb = new StringBuffer();

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

             Debug.error("Could not unexport RMI Server: "+
                      nsoex);

         }

        Debug.log(Debug.NORMAL_STATUS, StringUtils.getClassName(this) + 
                  ": Successfully shutdown communications RMI server.");
    }

    
    /**
     * Method providing asynchronous processing of binary data.
     *
     * @param  header  Message header.
     * @param  request Message body containing binary data.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     */
    public void processAsync ( String header, byte[] request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                      ": The header passed to binary processAsync is:\n" + header);

        try
        {
            if ( !isAsync() ) 
            {
                throw new ProcessingException ( "ERROR: " + StringUtils.getClassName(this) +
                                                ": Binary asynchronous process method does not support synchronous processing." );
            }

            String[] drvConfig = getDriverConfiguration(header);

            process(header, request, drvConfig[0], drvConfig[1]);

            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Binary async processing done.");                                     
        }
        catch (ProcessingException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                      ": processAsync: " + e.getMessage());

            throw new RMIServerException (RMIServerException.UnknownError, e.getMessage());
        } 
        catch (MessageException e) 
        {
            Debug.log(Debug.ALL_ERRORS, "ERROR: RMIBinaryRequestHandlerImpl.processAsync: " +
                      e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, 
                                              e.getMessage());
        }
    }

    
    /**
     * Method providing synchronous processing of binary data.
     *
     * @param  header  Message header.
     * @param  request Message body containing binary data.
     *
     * @return The synchronous response.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public Object processSync ( String header, byte[] request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                      ": The header passed to binary processSync is:\n" + header);

        try
        {
            if ( isAsync() ) 
            {
                throw new ProcessingException("ERROR: " + StringUtils.getClassName(this) + 
                                              ": Binary process sync method does not support asynchronous processing.");
            }

            String[] drvConfig = getDriverConfiguration(header);

            ResponseObject res = process(header, request, drvConfig[0], drvConfig[1]);
        
            if ( res.message == null )
                throw new ProcessingException("Null Response Obtained");

            if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            {
                if ( res.message instanceof byte[] )
                    Debug.log( Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                               ": The binary response data has length [" + ((byte[])(res.message)).length + "]." );
                else if ( res.message instanceof String )
                    Debug.log( Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                               ": The string response data has length [" + ((String)(res.message)).length() + "]." );
                else
                    Debug.log( Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                               ": The response data is of type [" + StringUtils.getClassName( res.message ) + "]." );
            }

            return( res.message );
        }
        catch (ProcessingException e) 
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                      ": processSync: processing error: " + 
                      e.getMessage());
            
            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        } 
        catch (ClassCastException e)
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) +
                      ": processSync: Invalid data type returned. Error: " +
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
    }
    

    /**
     * Method providing synchronous processing of binary data, with headers in and out.
     *
     * @param  header  Message header.
     * @param  request Message body containing binary data.
     *
     * @return A response-pair object containing the response header and body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public BinaryResponsePair processSynchronous ( String header, byte[] request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                      ": the header passed to binary processSynchronous is:\n" + header);

        try
        {
            if ( isAsync() )
            {
                throw new ProcessingException("ERROR: " + StringUtils.getClassName(this) + 
                                              ": Process synchronous method does not support asynchronous processing.");
            }

            String[] drvConfig = getDriverConfiguration(header);

            ResponseObject res =  process(header, request, drvConfig[0], drvConfig[1]);

            //This is not a sufficient check on the response, but we would like this method to be
            //used for various types of responses.
            if (res == null || res.header == null || res.message == null )
                throw new ProcessingException("Null Response Obtained");

            BinaryResponsePair data = new BinaryResponsePair( );

            data.header = Converter.getString(res.header);
            data.message = res.message;

            if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                Debug.log(Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                          ": The response header obtained from Process Synchronous method is:\n "
                          + data.header);

            if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            {
                if ( data.message instanceof byte[] )
                    Debug.log( Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                               ": The binary response data has length [" + ((byte[])(data.message)).length + "]." );
                else if ( data.message instanceof String )
                    Debug.log( Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                               ": The string response data has length [" + ((String)(data.message)).length() + "]." );
                else
                    Debug.log( Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) +
                               ": The response data is of type [" + StringUtils.getClassName( data.message ) + "]." );
            }

            return data;
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
    }


    private boolean asyncFlag = true;
    private String serviceName = null;
}
