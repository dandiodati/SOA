/**
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //comms/R4.4/com/nightfire/comms/ejb/GatewayBeanImpl.java#1 $
 */
package com.nightfire.comms.ejb;


import java.io.*;
import java.rmi.*;
import java.util.*;
import javax.ejb.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.rmi.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.repository.*;
import com.nightfire.framework.environment.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.communications.*;


/**
 * Class implementing a gateway as a session EJB.
 */
public class GatewayBeanImpl implements RMIRequestHandler, javax.ejb.SessionBean
{
    /**
     * Name of the deployment environment entry giving the gateway bean configuration key (optional).
     */
    public static final String CONFIG_KEY_ENV_PROP = "ConfigurationKey";

    /**
     * Name of the deployment environment entry giving the gateway bean configuration type (optional).
     */
    public static final String CONFIG_TYPE_ENV_PROP = "ConfigurationType";

    /**
     * Name of the deployment environment entry giving the gateway bean's async-flag value (required).
     */
    public static final String ASYNC_FLAG_ENV_PROP = "AsyncFlag";


    /**
     * Constructor called by container to create new bean instances.
     */
    public GatewayBeanImpl ( )
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "Gateway bean created." );
        
        beanClassName = StringUtils.getClassName( this );
        
        enterMessagePrefix   = "ENTERING: " + beanClassName + ".";
        leavingMessagePrefix = "LEAVING: " + beanClassName + ".";
    }


    /**
     * EJB specification-mandated method used to create a session bean instance.
     *
     * @exception  CreateException  Thrown on EJB-related creation errors.
     * @exception  RemoteException  Thrown on communication-related errors.
     */
    public void ejbCreate ( ) throws CreateException, RemoteException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "EJB-CREATE called." );
        
        try
        {
            String key  = J2EEUtils.getEnvProperty( CONFIG_KEY_ENV_PROP );
            String type = J2EEUtils.getEnvProperty( CONFIG_TYPE_ENV_PROP );
            
            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Gateway bean will use key [" + key 
                           + "], type [" + type + "] to locate configuration ..." );
            
            impl = new CommServerAdapter( key, type );
        }
        catch ( Exception e )
        {
            throw new CreateException( e.getMessage() );
        }
    }


    /**
     * EJB specification-mandated method used to activate a session bean instance from its
     * passive state. In the current context, this method will never be called.
     *
     * @exception  EJBException  Thrown on system-level errors.
     * @exception  RemoteException  Thrown on communication-related errors.
     */
    public void ejbActivate ( ) throws EJBException, RemoteException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "EJB-ACTIVATE called." );
    }


    /**
     * EJB specification-mandated method called before a session bean instance enters
     * its passive state. In the current context, this method will never be called.
     *
     * @exception  EJBException  Thrown on system-level errors.
     * @exception  RemoteException  Thrown on communication-related errors.
     */
    public void ejbPassivate ( ) throws EJBException, RemoteException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "EJB-PASSIVATE called." );
    }


    /**
     * EJB specification-mandated method called before a session bean instance's life
     * is about to end.
     *
     * @exception  EJBException  Thrown on system-level errors.
     * @exception  RemoteException  Thrown on communication-related errors.
     */
    public void ejbRemove ( ) throws EJBException, RemoteException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "EJB-REMOVE called." );
    }


    /**
     * EJB specification-mandated method called to set the associated session context,
     * after a session bean is instantiated.
     *
     * @param ctx The current session context for this bean.
     *
     * @exception  EJBException  Thrown on system-level errors.
     * @exception  RemoteException  Thrown on communication-related errors.
     */
    public void setSessionContext ( SessionContext ctx ) throws EJBException, RemoteException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "EJB-SET-SESSION-CONTEXT called." );

        context = ctx;
    }


    /**
     * Method used to get all interfaces supported by a given gateway.
     *
     * @return An array of UsageDescription objects.
     *
     * @exception 
     */
    public RMIRequestHandler.UsageDescription[] getUsageDescriptions ( ) throws java.rmi.RemoteException
    {
        logEntry( "getUsageDescriptions()" );
        
        try
        {
            return( impl.getUsageDescriptions() );
        }
        finally
        {
            logExit( "getUsageDescriptions()" );
        }
    }
    
    
    /**
     * Test to see if a particular usage is supported.
     *
     * @param  usage  UsageDescription object containing usage in question.
     *
     * @return 'true' if usage is supported, otherwise false.
     *
     * @exception RemoteException  Thrown on communications errors.
     */
    public boolean supportsUsage ( RMIRequestHandler.UsageDescription usage ) throws java.rmi.RemoteException
    {
        logEntry( "supportsUsage()" );
        
        try
        {
            return( impl.supportsUsage( usage ) );
        }
        finally
        {
            logExit( "supportsUsage()" );
        }
    }
    
    
    /**
     * Method providing asynchronous processing.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     */
    public void processAsync ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
    {
        logEntry( "processAsync()" );
        
        try
        {
            impl.processAsync( header, request );
        }
        finally
        {
            logExit( "processAsync()" );
        }
    }
    

    /**
     * Method providing synchronous processing.
     *
     * @param  header  Message header.
     * @param  request Message body.
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
        logEntry( "processSync()" );
        
        try
        {
            return( impl.processSync( header, request ) );
        }
        finally
        {
            logExit( "processSync()" );
        }
    }


    /**
     * Method providing synchronous processing, with headers in and out.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @return A response-pair object containing the response header and body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public RMIRequestHandler.ResponsePair processSynchronous ( String header, String request )
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {
        logEntry( "processSynchronous()" );
        
        try
        {
            return( impl.processSynchronous( header, request ) );
        }
        finally
        {
            logExit( "processSynchronous()" );
        }
    }


    /**
     * Method used to log method-entry messages.
     *
     * @param  method  Text describing method signature.
     */
    private final void logEntry ( String method )
    {
        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
            Debug.log( Debug.IO_STATUS, enterMessagePrefix + method );
    }
    
    
    /**
     * Method used to log method-exit messages.
     *
     * @param  method  Text describing method signature.
     */
    private final void logExit ( String method )
    {
        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
            Debug.log( Debug.IO_STATUS, leavingMessagePrefix + method );
    }


    // Class adapting the push com server base implementation
    // to the gateway EJB environment.
    private class CommServerAdapter extends PushComServerBase implements RMIRequestHandler
    {
        /**
         * Constructor
         * 
         * @param  key   Property-key to use for locating initialization properties.
         * @param  type  Property-type to use for locating initialization properties.
         *
         * @exception  ProcessingException  Thrown if initialization fails.
         */
        public CommServerAdapter ( String key, String type )
            throws ProcessingException
        { 
            super( key, type );
        }
        
        
        /**
         * Get the value of the flag indicating whether the gateway is async or sync.
         *
         * @return 'true' if async, 'false' if sync.
         */
        public boolean isAsync ( )
        {
            try
            {
                boolean asyncFlag = StringUtils.getBoolean( J2EEUtils.getEnvProperty( ASYNC_FLAG_ENV_PROP ) );
                
                if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                    Debug.log( Debug.SYSTEM_CONFIG, "Gateway is asynchronous? [" + asyncFlag + "]." );

                return asyncFlag;
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );
                
                return true;
            }
        }


        /**
         * Method used to get all interfaces supported by a given gateway.
         *
         * @return An array of UsageDescription objects.
         *
         * @exception 
         */
        public RMIRequestHandler.UsageDescription[] getUsageDescriptions ( ) throws java.rmi.RemoteException
        {
            ArrayList usageList = getUsageList( );
            
            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "The total number of usages is ["
                           + usageList.size() + "]." );
            
            RMIRequestHandler.UsageDescription[] rmiUsage = new RMIRequestHandler.UsageDescription[ usageList.size() ];
            
            for( int i=0;  i < usageList.size();  i++ )
            {
                CommonUsageDescription common = (CommonUsageDescription)usageList.get( i );
                
                rmiUsage[i] = new RMIRequestHandler.UsageDescription( common.getServiceProvider(),
                                                                      common.getInterfaceVersion(),
                                                                      common.getOperationType(),
                                                                      common.isAsynchronous() );
            }
            
            return rmiUsage;
        }
        
        
        /**
         * Test to see if a particular usage is supported.
         *
         * @param  usage  UsageDescription object containing usage in question.
         *
         * @return 'true' if usage is supported, otherwise false.
         *
         * @exception RemoteException  Thrown on communications errors.
         */
        public boolean supportsUsage ( RMIRequestHandler.UsageDescription usage ) throws java.rmi.RemoteException
        {
            //Get all the descriptions for the particular server
            RMIRequestHandler.UsageDescription[] rmiUsage = getUsageDescriptions( );
            
            // Loop over all usages, checking for a match by comparing ServiceProviderName, 
            // OperationType, Version and Asynchronous.
            for ( int i=0;  i < rmiUsage.length;  i++ ) 
            {
                if ( rmiUsage[i].operationType.equals(usage.operationType) &&
                     rmiUsage[i].serviceProvider.equals(usage.serviceProvider) &&
                     rmiUsage[i].interfaceVersion.equals(usage.interfaceVersion) &&
                     rmiUsage[i].asynchronous == usage.asynchronous )
                {
                    if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                        Debug.log( Debug.SYSTEM_CONFIG, "Supported usage: operation-type [" + usage.operationType 
                                   + "], service-provider [" + usage.serviceProvider + "], interface-version [" 
                                   + usage.interfaceVersion + "], asynchronous [" + usage.asynchronous + "]." );
                    
                    return true;
                }
            }
            
            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Non-supported usage: operation-type [" + usage.operationType 
                           + "], service-provider [" + usage.serviceProvider + "], interface-version [" 
                           + usage.interfaceVersion + "], asynchronous [" + usage.asynchronous + "]." );

            return false;
        }
        
        
        /**
         * Method providing asynchronous processing.
         *
         * @param  header  Message header.
         * @param  request Message body.
         *
         * @exception RemoteException  Thrown on communications errors.
         * @exception RMIInvalidDataException  Thrown if request data is bad.
         * @exception RMIServerException  Thrown if server can't process request due to system errors.
         */
        public void processAsync ( String header, String request )
            throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
        {
            try
            {
                processAsyncRequest( header, request );
            }
            catch ( ProcessingException e ) 
            {
                Debug.error( e.getMessage() );
                
                throw new RMIServerException( RMIServerException.UnknownError, e.getMessage() );
            } 
            catch ( MessageException e ) 
            {
                Debug.error( e.getMessage() );
                
                throw new RMIInvalidDataException( RMIInvalidDataException.UnknownDataError, 
                                                   e.getMessage() );
            }
        }
        
        
        /**
         * Method providing synchronous processing.
         *
         * @param  header  Message header.
         * @param  request Message body.
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
            try
            {
                return( processSyncRequest( header, request ) );
            }
            catch ( ProcessingException e ) 
            {
                Debug.error( e.getMessage() );
                
                throw new RMIServerException( RMIServerException.UnknownError, e.toString() );
            } 
            catch ( MessageException e ) 
            {
                Debug.error( e.getMessage() );
                
                throw new RMIInvalidDataException( RMIInvalidDataException.UnknownDataError, e.getMessage() );
            }
        }
        
        
        /**
         * Method providing synchronous processing, with headers in and out.
         *
         * @param  header  Message header.
         * @param  request Message body.
         *
         * @return A response-pair object containing the response header and body.
         *
         * @exception RemoteException  Thrown on communications errors.
         * @exception RMIInvalidDataException  Thrown if request data is bad.
         * @exception RMIServerException  Thrown if server can't process request due to system errors.
         * @exception RMINullResultException  Thrown if server can't process request due to system errors.
         */
        public RMIRequestHandler.ResponsePair processSynchronous ( String header, String request )
            throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
        {
            try
            {
                MessageData response = processSynchronousRequest( header, request );

                if ( !StringUtils.hasValue( response.header ) || !StringUtils.hasValue( response.body ) )
                {
                    String errMsg = "Invalid response header or response body: Response header = [" 
                                    + response.header + "], Response body = [" + response.body + "].";
                    
                    Debug.error( errMsg );
                    
                    throw new RMIServerException( RMIServerException.UnknownError, errMsg );
                }
                
                RMIRequestHandler.ResponsePair result = new  RMIRequestHandler.ResponsePair( );

                result.header  = response.header;
                result.message = response.body;
                
                return result;
            }
            catch ( ProcessingException e )
            {
                Debug.error( e.getMessage() );
                
                throw new RMIServerException( RMIServerException.UnknownError, e.toString() );
            }
            catch ( MessageException e )
            {
                Debug.error( e.getMessage() );
                
                throw new RMIInvalidDataException( RMIInvalidDataException.UnknownDataError, e.getMessage() );
            }
        }
        
        
        /**
         * Required by PushComServerBase, but not used here.
         */
        public void run ( ) 
        {
            Debug.warning( "run() method was called." );
        }
        
        
        /**
         * Implements shutdown operation
         */
        public void shutdown() 
        {
            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log( Debug.OBJECT_LIFECYCLE, "shutdown() called." );
        }
    }


    // Implementation object supporting push comm server methods in remote interface.
    private CommServerAdapter impl;

    // Reference to the current session context for this session bean instance.
    private SessionContext context;

    // Name of this class without package prefix.
    private final String beanClassName;

    // Prefixes for log messages bracketing method calls.
    private final String enterMessagePrefix;
    private final String leavingMessagePrefix;
}
