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
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.supervisor.Supervisor;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.servers.ServerBase;
import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.spi.common.supervisor.*;

import org.w3c.dom.*;
import org.omg.CORBA.ORB;
import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;

/**
 * RMI Server that accepts a async/sync/synchronous method invocation and sends it
 * on to the CORBA server specified in the properties. This RMI-CORBA bridge server
 * essentially acts as an RMI-CORBA adapter.
 * <P>
 * The properties to be configured on this server object are:
 * <P>
 * The properties for registering this RMI server
 * SERVER_NAME, HOST_NAME, PORT_NUMBER
 *
 * <P>
 * Information of this RMI server (not really used)
 * INTERFACE_VERSION_#, SERVICE_PROVIDER_#, OPERATION_#, ASYNCHRONOUS_#
 *
 * <P>
 * The CORBA server to send the message to
 * CORBA_SERVER_NAME, ORBagentAddr, ORBagentPort
 */
public class RMICORBABridge extends RMIRequestHandlerImpl
{
    /**
     *  Constant that indicates processSync method.
     */
    public static final String PROCESS_SYNC_METHOD = "processSync";

    /**
     *  Constant that indicates processAsync method.
     */
    public static final String PROCESS_ASYNC_METHOD = "processAsync";

    /**
     *  Constant that indicates processSynchronous method.
     */
    public static final String PROCESS_SYNCHRONOUS_METHOD = "processSynchronous";

    /**
     *  The property name that specifies the Server to send request to
     */
    public static final String CORBA_SERVER_NAME_PROP = "CORBA_SERVER_NAME";

    /**
     *  The property name that specifies the ORBAgentAddr
     */
    public static final String ORB_AGENT_ADDR_PROP = ServerBase.ORB_AGENT_ADDR_PROP;

    /**
     *  The property name that specifies the ORBAgentPort
     */
    public static final String ORB_AGENT_PORT_PROP = ServerBase.ORB_AGENT_PORT_PROP;
    
    // The name of the Corba server that implements RequestHandler
    private String corbaServerName = null;

    // Corba Location Properties
    private String orbAgentAddr = null;
    private String orbAgentPort = null;

    //ORB container for use across all instances of this class inside a single VM.
    private static Map orbStore = new HashMap( );


    /**
     * Creates an RMI server object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public RMICORBABridge(String key, String type)
        throws ProcessingException
    {
        super(key, type);

        Debug.log(Debug.NORMAL_STATUS,"RMICORBABridge: Initializing the RMI-CORBA Bridge Communications Server ...");

        // Get the RMI configuration properties.
        StringBuffer sb = new StringBuffer();

        corbaServerName = getRequiredPropertyValue( CORBA_SERVER_NAME_PROP, sb );        

        orbAgentAddr = getPropertyValue (ORB_AGENT_ADDR_PROP);

        orbAgentPort = getPropertyValue (ORB_AGENT_PORT_PROP);

        if (sb.length() > 0)
            throw new ProcessingException(StringUtils.getClassName(this) +
                                          ": Invalid properties specified.\n" +
                                          sb.toString());

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
            makeClientCall( PROCESS_ASYNC_METHOD, corbaServerName, header, request );
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
            String response = (String) makeClientCall( PROCESS_SYNC_METHOD, corbaServerName, header, request );

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
            ResponsePair rp = (ResponsePair) makeClientCall( PROCESS_SYNCHRONOUS_METHOD, corbaServerName, header, request );

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
     * Make the actual CORBA call. If the CORBA call fails because we have a stale object reference,
     * then try again with a fresh object reference.
     *
     * @param  methodName Name of the method to be invoked on the CORBA object (one of processAsync,
     * processSync or process Asynchronous).
     * @param  corbaServerName  Name of the server as known in the COS Naming Service.
     * @param  reload  Flag indicating whether object reference should be refreshed (true)
     *                 or any previously-cached version should be used (false).
     * @param  header  Header message to send.
     * @param  message Body message to send.
     *
     * @return  Response that caller should return.
     *
     * @exception  Exception  Thrown on any errors.
     */
    private Object makeClientCall ( String methodName, String corbaServerName, String header, String message )
    throws MessageException, ProcessingException
    {
        try
        {
            try
            {
                return ( makeCORBACall( methodName, corbaServerName, false, header, message ) );
            }
            catch ( Exception e )
            {
                // Any of the following exceptions indicate that the failure might be due to
                // a CORBA communications issue (stale object reference) that should be retried.
                if ( (e instanceof org.omg.CORBA.OBJECT_NOT_EXIST) || 
                     (e instanceof org.omg.CORBA.TRANSIENT) || 
                     (e instanceof org.omg.CORBA.COMM_FAILURE) || 
                     (e instanceof org.omg.CORBA.INV_OBJREF) ||
                     (e instanceof org.omg.CORBA.UNKNOWN) )
                {
                    Debug.warning( "Caught the following CORBA communication error, so retrying:\n" 
                                   + e.toString() + "\n" + Debug.getStackTrace(e) );

                    return ( makeCORBACall( methodName, corbaServerName, true, header, message ) );
                }
                else 
                {
                    // It's not a communication exception indicating that retry is recommended, 
                    // so just re-throw it.
                    throw e;
                }
            }
        } 
        catch( Exception e ) 
        {
            if ( e instanceof InvalidDataException ) 
            {
                Debug.error( e.toString() + "\n" + Debug.getStackTrace( e ) );

                throw new MessageException( ((InvalidDataException)e).errorMessage );
            }
            else
            {
                Debug.error( e.toString() + "\n" + Debug.getStackTrace( e ) );

                throw new ProcessingException( e.toString() );
            }
        }

    }

    /**
     * Make the actual CORBA call.
     *
     * @param  methodName Name of the method to be invoked on the CORBA object (one of processAsync,
     * processSync or process Asynchronous).
     * @param  corbaServerName  Name of the server as known in the COS Naming Service.
     * @param  reload  Flag indicating whether object reference should be refreshed (true)
     *                 or any previously-cached version should be used (false).
     * @param  header  Header message to send.
     * @param  message Body message to send.
     *
     * @return  Response that caller should return.
     *
     * @exception  Exception  Thrown on any errors.
     */
    private Object makeCORBACall ( String methodName, String corbaServerName, boolean reload, String header, String message ) throws Exception
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS) )
        {
            Debug.log ( Debug.MSG_STATUS, "RMICORBABridge: Invoking method [" + methodName +
            "] on server [" + corbaServerName + "]." );
        }

        ORB orb = null;

        //If the alternate ip and port are provided
        if ( StringUtils.hasValue ( orbAgentAddr ) && StringUtils.hasValue ( orbAgentPort ) )
        {
            //Key to store the orb in a static map for access the next time around
            String key = orbAgentAddr + ":" + orbAgentPort ;
            
            synchronized ( orbStore )
            {
                orb = (ORB) orbStore.get( key );

                if ( orb == null )
                {
                    Properties props = new Properties();
                    props.put ( ORB_AGENT_ADDR_PROP, orbAgentAddr );
                    props.put ( ORB_AGENT_PORT_PROP, orbAgentPort );
                    orb = new CorbaPortabilityLayer( new String[0], props, null, corbaServerName ).getORB();
                    
                    orbStore.put ( key , orb );
                }
                else
                {
                    if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
                        Debug.log( Debug.IO_STATUS, "Using cached orb with properties " +
                                   "ORBagentAddr = [" + orbAgentAddr +"] ORBagentPort = [" + orbAgentPort +"]");
                }
            }
        }
        else
        {
            if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "Using the default orb ..");
            
            orb = Supervisor.getSupervisor().getCPL().getORB();
        }        
        
        ObjectLocator ob_loc = new ObjectLocator( orb );

        if ( reload )
            ob_loc.removeFromCache( corbaServerName );

        RequestHandler rh = RequestHandlerHelper.narrow( ob_loc.find( corbaServerName ) );

        if ( rh == null )
        {
            throw new Exception("Object named [" + corbaServerName + "] is not of IDL type RequestHandler." );
        }

        // Make sure that the header contains any available customer context information.
        header = CustomerContext.getInstance().propagate( header );

        if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
            Debug.log( Debug.IO_STATUS, "Header value:\n" + header );

        if (methodName.equals(PROCESS_SYNC_METHOD))
        {
            org.omg.CORBA.StringHolder response = new org.omg.CORBA.StringHolder("");

            rh.processSync( header, message, response );

            return( response.value );
        }
        else
        if (methodName.equals(PROCESS_ASYNC_METHOD))
        {
            rh.processAsync( header, message );

            return( null );
        }
        else
        {
            org.omg.CORBA.StringHolder responseHeader = new org.omg.CORBA.StringHolder("");
            org.omg.CORBA.StringHolder response = new org.omg.CORBA.StringHolder("");
            rh.processSynchronous( header, message, responseHeader, response );

            ResponsePair rp = new ResponsePair( );
            rp.header  = responseHeader.value;
            rp.message = response.value;
            return( rp );
        }
    }

}
