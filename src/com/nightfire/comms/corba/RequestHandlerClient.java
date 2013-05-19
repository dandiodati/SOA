/*
 * Copyright(c) 2002 Nightfire Software, Inc. All rights reserved.
  */

package com.nightfire.comms.corba;

import com.nightfire.framework.util.FrameworkException;
import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.corba.*;
import com.nightfire.spi.common.supervisor.Supervisor;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.servers.ServerBase;
import com.nightfire.framework.monitor.*;

import org.w3c.dom.*;
import org.omg.CORBA.ORB;
import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;

import java.util.*;

/**
 *
 *
 * This processor forwards requests to a Corba Server that implements the RequestHandler interface,
 * i.e. Nightfire Corba Server.
 *
 * The processor assumes that there exists 2 values in the context
 *      1. NF_HEADER_LOCATION_PROP - contain the NF style header 
 *      2. IS_ASYNCHRONOUS_PROP  - indicates if call should be async or sync
 */
public class RequestHandlerClient extends MessageProcessorBase  {
       
    /**
     *  The property name that specifies the Server to send request to
     */
    public static final String SERVER_NAME_PROP = "SERVER_NAME";

    /**
     *  The property name that specifies the ORBAgentAddr
     */
    public static final String ORB_AGENT_ADDR_PROP = ServerBase.ORB_AGENT_ADDR_PROP;

    /**
     *  The property name that specifies the ORBAgentPort
     */
    public static final String ORB_AGENT_PORT_PROP = ServerBase.ORB_AGENT_PORT_PROP;

    /**
     *  The property name that specifies the context location of alternate ORBAgentAddr
     */
    public static final String ORB_AGENT_ADDR_PROP_LOCATION = ServerBase.ORB_AGENT_ADDR_PROP + "_LOC";

    /**
     *  The property name that specifies the context location of alternate ORBAgentPort
     */
    public static final String ORB_AGENT_PORT_PROP_LOCATION = ServerBase.ORB_AGENT_PORT_PROP + "_LOC";

    /**
     *  The key to use on the context to look up the NF style header
     */
    public static final String NF_HEADER_LOCATION_PROP = "HEADER_LOC";

    /**
     *  The key to use on the context to find out if this is a synchronous or asynchronous call.
     */
    public static final String IS_ASYNCHRONOUS_LOCATION_PROP = "IS_ASYNCHRONOUS_LOC";

    /**
     *  Properties for Default values that may be used if no values set in context
     */
    public static final String DEFAULT_HEADER_PROP = "DEFAULT_HEADER";
    
    public static final String DEFAULT_IS_ASYNCHRONOUS_PROP  = "DEFAULT_IS_ASYNCHRONOUS";

    // The header to be sent to the Server 
    private String header = null;
    
    // The name of the Corba server that implements RequestHandler
    private String serverName = null;

    // Corba Location Properties
    private String orbAgentAddr = null;
    private String orbAgentPort = null;

    // Corba alternate Orb Location Properties
    private String orbAgentAddrLocation = null;
    private String orbAgentPortLocation = null;

    // Flag for process Sync/Async
    private boolean isAsync = false;

    private String headerLocation = null;
    private String isAsyncLocation = null;
    
    //ORB container for use across all instances of this class inside a single VM.
    private static Map orbStore = new HashMap( );


    /**
     * Initializes this object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException
    {
        super.initialize(key, type);

        StringBuffer errorBuf = new StringBuffer();

        // serverName = getRequiredProperty(SERVER_NAME_PROP, errorBuf);

        headerLocation = getPropertyValue(NF_HEADER_LOCATION_PROP);

        isAsyncLocation = getPropertyValue(IS_ASYNCHRONOUS_LOCATION_PROP);

        orbAgentAddr = getPropertyValue (ORB_AGENT_ADDR_PROP);

        orbAgentPort = getPropertyValue (ORB_AGENT_PORT_PROP);

        orbAgentAddrLocation = getPropertyValue(ORB_AGENT_ADDR_PROP_LOCATION);

        orbAgentPortLocation = getPropertyValue(ORB_AGENT_PORT_PROP_LOCATION);

        if(!StringUtils.hasValue(isAsyncLocation) ){
            try{
                isAsync = StringUtils.getBoolean( (String) getRequiredPropertyValue(DEFAULT_IS_ASYNCHRONOUS_PROP, errorBuf) );
            }
            catch (FrameworkException fe){
                errorBuf.append("No value is specified for either "+ IS_ASYNCHRONOUS_LOCATION_PROP+ "or"+ DEFAULT_IS_ASYNCHRONOUS_PROP
                                + ". One of the values should be present"+ fe.getMessage());
            }
        }


        if(!StringUtils.hasValue(headerLocation) ){
            try{
                header = getRequiredPropertyValue(DEFAULT_HEADER_PROP, errorBuf) ;
            }
            catch (Exception e){
                errorBuf.append("No value is specified for "+ NF_HEADER_LOCATION_PROP+ "or"+ DEFAULT_HEADER_PROP
                                + ". One of the values should be present"+ e.getMessage());
            }
        }
        
        
        if ( errorBuf.length() > 0 ) 
            throw new ProcessingException(errorBuf.toString());
    }


    /**
     * If NF_HEADER_LOCATION_PROP exists in context use as the header to forward to Gateway.
     * If IS_ASYNCHRONOUS_PROP  exists in context then use that value to call either
     * processAsync or processSync
     *
     *
     * @param  input  MessageObject containing the value to be processed     *
     * @param  mpcontext The context
     *
     * @return  Optional NVPair containing a Destination name and a MessageObject,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if bad message.
     */
    public NVPair[] process(MessageProcessorContext ctx, MessageObject input ) throws MessageException, ProcessingException
    {
        if (input == null ) 
            return null;

        try {
               serverName = getRequiredProperty(ctx, input, SERVER_NAME_PROP);
            }
        catch ( MessageException me ) {
                throw new ProcessingException(me.getMessage());
            }

        if( StringUtils.hasValue(headerLocation) )  
        {
            try {
                header = getString (headerLocation, ctx, input);
            }
            catch ( MessageException me ) {
                throw new ProcessingException(me.getMessage());
            }
        }

        if(StringUtils.hasValue(isAsyncLocation))  
        {
            try {
                isAsync = StringUtils.getBoolean( getString(isAsyncLocation, ctx, input) );
            }
            catch (FrameworkException fe) {
                throw new ProcessingException("Value of " + IS_ASYNCHRONOUS_LOCATION_PROP + " is not TRUE/FALSE. " + fe.getMessage() );
            }
        }

        // Fetch the alternate Orb Address, if one exists at the specified context location
        if( StringUtils.hasValue(orbAgentAddrLocation) )
        {
            try {
                if (exists(orbAgentAddrLocation,ctx,input,true))
                {
                    orbAgentAddr = getString (orbAgentAddrLocation, ctx, input);
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "RequestHandlerClient:: alternate orb exists with orb agent address [" + orbAgentAddr + "]");
                }
            }
            catch ( MessageException me ) {
                throw new ProcessingException(me.getMessage());
            }
        }

        // Fetch the alternate Orb Port, if one exists at the specified context location
        if( StringUtils.hasValue(orbAgentPortLocation) )
        {
            try {
                if (exists(orbAgentPortLocation,ctx,input,true))
                {
                    orbAgentPort = getString (orbAgentPortLocation, ctx, input);
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS,"RequestHandlerClient:: alternate orb exists with orb agent port [" + orbAgentPort + "]");
                }
            }
            catch ( MessageException me ) {
                throw new ProcessingException(me.getMessage());
            }
        }

        String msg = input.getString();

        try
        {
            try
            {
                return ( formatNVPair( makeClientCall( serverName, false, header, msg ) ) );
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

                    return ( formatNVPair( makeClientCall( serverName, true, header, msg ) ) );
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
     * @param  serverName  Name of the server as known in the COS Naming Service.
     * @param  reload  Flag indicating whether object reference should be refreshed (true)
     *                 or any previously-cached version should be used (false).
     * @param  header  Header message to send.
     * @param  message Body message to send.
     *
     * @return  Response that caller should return.
     *
     * @exception  Exception  Thrown on any errors.
     */
    private String makeClientCall ( String serverName, boolean reload, String header, String message ) throws Exception
    {
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
                    orb = new CorbaPortabilityLayer( new String[0], props, null, serverName ).getORB();
                    
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
        {
         ob_loc.removeFromCache( serverName );
         if ( StringUtils.hasValue ( orbAgentAddr ) && StringUtils.hasValue ( orbAgentPort ) )
             ob_loc.removeFromCache( serverName, orbAgentAddr, orbAgentPort );
        }

        RequestHandler rh = null;
        // The key corresponding to secondary install object references depends on host address and port too.
        if ( StringUtils.hasValue ( orbAgentAddr ) && StringUtils.hasValue ( orbAgentPort ) )
            rh = RequestHandlerHelper.narrow( ob_loc.find(serverName, orbAgentAddr, orbAgentPort));
        else
            rh = RequestHandlerHelper.narrow( ob_loc.find( serverName ) );

        if ( rh == null )
        {
            throw new Exception("Object named [" + serverName + "] is not of IDL type RequestHandler." );
        }

        // Make sure that the header contains any available customer context information.
        header = CustomerContext.getInstance().propagate( header );

        if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
            Debug.log( Debug.IO_STATUS, "Header value:\n" + header );

        ThreadMonitor.ThreadInfo tmti = ThreadMonitor.start( "Message-processor [" + getName() + 
                                                             "] making CORBA client call with header:\n" + header );

        try
        {
           if (isAsync)
           {
              rh.processAsync( header, message );

              return( message );
           }
           else
           {
              org.omg.CORBA.StringHolder response = new org.omg.CORBA.StringHolder("");

              rh.processSync( header, message, response );

              return( response.value );
           }
        }
        finally
        {
           ThreadMonitor.stop( tmti );
        }
    }


    public static void main(String[] args)
    {
        Debug.enableAll();
        String HEADER = "<HEADER>" +
            "<REQUEST value=\"LSR_ORDER\"/>" +
            "<SUB_REQUEST value=\"loop\"/>" +
            "<SUPPLIER value=\"VZE\"/>" +
            "</HEADER>";
        RequestHandlerClient sr = null;
		
        try{
            String xml = FileUtils.readFile(args[0]);
            MessageProcessorContext ctx = new MessageProcessorContext();
            ctx.set("NF_HEADER_LOCATION_PROP", HEADER);  
            sr = new RequestHandlerClient();
            sr.serverName = "Nightfire.Router";
            NVPair[] result = sr.process(ctx, new MessageObject( (Object) xml));
        } catch(Exception e){
		    e.printStackTrace();
        }	   
			
	}
}


