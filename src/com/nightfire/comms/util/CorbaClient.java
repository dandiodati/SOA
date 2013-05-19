/*
 * Copyright (c) 2000-2002 Nightfire Software, Inc. All rights reserved.
 * $Header: //comms/R4.4/com/nightfire/comms/util/CorbaClient.java#1 $
 */
package com.nightfire.comms.util;

import org.omg.CORBA.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;

import com.nightfire.framework.corba.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.comms.soap.SOAPConstants;

/**
 * A generic Corba Client to send messages to a Corba Server
 */
public class CorbaClient implements PushConsumerCallBack
{

    private static CorbaPortabilityLayer cpl    = null;
    private static org.omg.CORBA.ORB orb        = null;

    //Name of Server or EventChannel as known in NamingService
    private String corbaServerName              = null;
    
    //Whether specified server is asynchronous or not
    private boolean async                       = true;
    
    //The contents of an event or 'TIMED_OUT' 
    private String eventMessage                 = SOAPConstants.TIMED_OUT;
    
    //Helps register/disconnect with specified EventChannel
    private EventPushConsumer helper            = null;



    /**
     * Constructor
     *
     * @param async - flag indicating whether server we connecting to is
     *                synchronous (false) or asynchronous (true).
     *
     * @param corbaServerName - the name of the Nightfire SPI, as known in
     *                          the Naming Service.
     */
    public CorbaClient( boolean async, String corbaServerName )
    {
        this.async = async;
        this.corbaServerName = corbaServerName;
    }


    /**
     * Sends the header and request to a specified corba server
     *
     * @param header - the Nightfire style header
     *
     * @param request - the request message being sent to the server
     *
     * @exception CorbaServerException - thrown if unable to there is a communication
     *                                   failure or a processing failure.
     *
     * @exception InvalidDataException - thrown if the request is malformed.
     *
     */
    public String send ( String header, String request ) throws CorbaException, CorbaServerException, InvalidDataException, NullResultException
    {

        String response = null;


        org.omg.CORBA.Object server= null;
	
	initialize();

        ObjectLocator objLocator = new ObjectLocator( orb );
        objLocator.removeFromCache(corbaServerName);
        server = objLocator.find( corbaServerName, null );

        RequestHandler handler = RequestHandlerHelper.narrow( server );

        if (handler == null )
          throw new CorbaException("Narrow to server [" + server + "] failed.");

        if (async)
        {
            handler.processAsync( header, request );
        }
        else
        {
            org.omg.CORBA.StringHolder responseHolder = new org.omg.CORBA.StringHolder( " " );
            handler.processSync( header,  request, responseHolder );
            response = responseHolder.value;
        }

        return response;
    }
    
    

    /**
     * Retrieves an event from the specified event channel.
     *
     * @param timeOut - the time in seconds to wait for an event.
     *
     * @exception CorbaException - thrown if unable to there is a communication
     *                                   failure or a processing failure.
     *
     */
     public String getEvent(int timeOut) throws CorbaException {
        try {
                        
            initialize();
            //get instance of EventPushConsumer and register this class so it will be called back when events are
            //posted to the event channel
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Registering for events on " + corbaServerName);
            
            
            ObjectLocator objLocator = new ObjectLocator( orb );
            objLocator.removeFromCache(corbaServerName);
            
            helper = new EventPushConsumer(orb, corbaServerName, false );
            helper.register(this);

	    synchronized (this) {
		  wait(timeOut * 1000 );
	    }

            //make sure we disconnect
            helper.disconnect();

        } catch (InterruptedException iex ) {
	    throw new CorbaException(iex.getMessage());
    	}

        return ( getEventMessage() );
    }

    /**
     * Processes the event pushed from event channel.
     *
     * @param   event The String pushed from event channel.
     */
    public void processEvent(String event) {
        
        try {
            synchronized (this) {
                //we could be waiting or we have timed out.
                if (Debug.isLevelEnabled(Debug.IO_STATUS))
                    Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Processing : " + event);
                helper.disconnect();

                setEventMessage(event);
                notify();
            }
        }
        catch (Exception me) {
            throw new RuntimeException(me.getMessage());
        }
    }
    
    /**
     * Sets the message received from EventChannel.
     *
     * @param msg - the event message
     */
    private synchronized void setEventMessage(String msg) 
    {
        this.eventMessage = msg;
    }
    
    /**
     * Gets the event message 
     *
     * @return String - the event message of 'TIMED_OUT' if no event was
     *                  received before timeout expired.
     */
    private synchronized String getEventMessage()
    {
        return eventMessage;
    }
    
    

    /**
     * Utility method to initialize the CorbaPortability and orb in a thread safe manner.
     */
    private void initialize() throws CorbaException {
	
        if ( cpl == null) {
	  synchronized (this) {
          	cpl = new CorbaPortabilityLayer(null, System.getProperties()) ;
	  }
	}

        if ( orb == null) {
	  synchronized (this) {
           	orb = cpl.getORB();
	  }
	}
    }

	

    //Test
    public static void main (String argv[]) throws Exception
    {
        if (argv.length != 4)
        {
            System.err.println("\n Usage: CorbaClient <sync|async> <CORBA Name of server> <input header> <input message>");
            System.exit(-1);
        }

        String asyncFlag = argv[0];
        String serverName = argv[1];
        String headerFile = argv[2];
        String inputFile = argv[3];

        boolean async = true;

        if (asyncFlag.equals("async"))
        {
            async = true;
        }
        else if (asyncFlag.equals("sync") )
        {
            async = false;
        }
        else
        {
            quit ("The first argument to the corba client should either be  \"sync\" or \"async\"");
        }

        String header = null;
        String message = null;

        try
        {
            header = FileUtils.readFile(headerFile);
            message = FileUtils.readFile(inputFile);
        }
        catch(FrameworkException fe)
        {
            quit ("CorbaClient couldn't load the file with name =[" + inputFile +"]");
        }

        CorbaClient corbaClient = new CorbaClient( async, serverName );

        String response = null;

        try
        {
            response = corbaClient.send( header, message );
        }
        catch(FrameworkException fe)
        {
            quit ("Processing error on the server, Reason = " + fe.getMessage() );
        }

    }

    private static void quit ( String errorMessage )
    {
        System.err.println( errorMessage );
        System.exit(-1);
    }
    


    
}




