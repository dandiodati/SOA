/*
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.corba;

import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;
import org.omg.CORBA.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.spi.common.supervisor.Supervisor;

import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * Asynchronous Corba Client posts events (Strings/DOM etc) into an Event Channel
 * which can be picked up by interested listeners (Engine, Server etc)
 */
public class AsyncCorbaClient extends MessageProcessorBase 
{
    /**
     * Property Key for EVENT CHANNEL NAME stored in Persistent Property Table
     */
    public static final String EVENT_CHANNEL_NAME_PROP = "EVENT_CHANNEL_NAME";

    /**
     * Property giving location where event channel name can be obtained dynamically
     * at runtime from the context or input (optional).  If not found, the default
     * static property value will be used
     */
    public static final String EVENT_CHANNEL_NAME_LOC_PROP = "EVENT_CHANNEL_NAME_LOC";


    /**
     * Push Supplier for publishing events into the channel
     */
    private EventPushSupplier supplier                 = null;

    //Initializes corba service
    private CorbaPortabilityLayer cpl                  = null;

    
    /**
     * Constructor used by communications factory
     */
    public AsyncCorbaClient() 
    {
    }


    /**
     * Constructor used by Unit Test Programs
     */
    public AsyncCorbaClient(CorbaPortabilityLayer cpl) 
    {
        this.cpl = cpl;
    }

    
    /**
     * Initializes this object given the <code>Key</code> and <code>Type</code>
     * <br> Properties are loaded from the Database based on the <code><b> Key </b></code> and
     * <code><b> Type </code></b><br>
     *
     * @exception ProcessingException Throws when Initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException 
    {
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
             Debug.log(Debug.SYSTEM_CONFIG, "Initializing the asynchronous CORBA client.");
        
        super.initialize(key,type);
    }
    

     /**
     * Initializes the ORB and BOA, binds to an Event Channel after loading
     * Properties from the Database
     */
    private void initializeEventService( MessageProcessorContext mpcontext,
                                         MessageObject msgObj )
        throws ProcessingException, MessageException
    {
        initializeEventService( mpcontext,msgObj, false);
    }

    /**
     * Initializes the ORB and BOA, binds to an Event Channel after loading
     * Properties from the Database
     */
    private void initializeEventService( MessageProcessorContext mpcontext,
                                         MessageObject msgObj, boolean retry )
        throws ProcessingException, MessageException 
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Initializing the Event Service");

        if (cpl == null)
        {
            cpl = Supervisor.getSupervisor().getCPL();
        }

        try
        {
            String eventChannelName = getEventChannelName( mpcontext,
                                                           msgObj );

            if(Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "Creating push supplier ...");
            supplier = new EventPushSupplier(cpl.getORB(), eventChannelName , false, retry);
            if(Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "Created push supplier.");
        }
        catch (CorbaException ce)
        {
            Debug.log(Debug.MSG_ERROR, ce.getMessage());
            throw new ProcessingException(ce.getMessage());
        }
    }

    /**
     * This call-out method is used by initializeEventService() to
     * determine to which event channel an event should be published.
     * This method first checks to see if the event channel name exists
     * in the given context of message, and if not found there, uses
     * the value configured in the EVENT_CHANNEL_NAME property.
     * This method allows subclasses to customize the manner in which
     * the event channel name is determined.
     *
     * @return the event channel name to use.
     */
    protected String getEventChannelName(MessageProcessorContext mpcontext,
                                         MessageObject msgObj)
                                         throws ProcessingException,
                                                MessageException {

       String eventChannelName = null;

       String eventChannelNameLoc = getPropertyValue( EVENT_CHANNEL_NAME_LOC_PROP );

       if ( StringUtils.hasValue( eventChannelNameLoc ) && exists( eventChannelNameLoc, mpcontext, msgObj ) )
       {
           eventChannelName = getString( eventChannelNameLoc, mpcontext, msgObj );

           if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
               Debug.log(Debug.IO_STATUS, "Event channel name obtained from location ["
                         + eventChannelNameLoc + "] is [" + eventChannelName + "]." );
       }
       else
       {
           eventChannelName = getRequiredPropertyValue( EVENT_CHANNEL_NAME_PROP );

           if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
               Debug.log(Debug.IO_STATUS, "Configured event channel name is [" + eventChannelName + "]." );
       }

       return eventChannelName;

    }


    /**
     * Publishes the given request into an event channel as Corba Events
     * which can be picked up by interested listeners in the channel
     *
     * @param String  Input String , message which needs to be published
     *
     * @exception Exception  thrown when publishing Fails
     */
    public void publishEvents(String request) throws Exception
    {
        if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log(Debug.MSG_LIFECYCLE, "Pushing event to the channel ...");
        supplier.pushEvent(request);
        if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log(Debug.MSG_LIFECYCLE, "Successfully pushed the event.");
    }


    /**
     * Implements the disconnect operation with the event consumer
     */
    public void disconnect() 
    {
        if ( supplier != null )
            supplier.disconnect();
    }


    /**
     * Processes the given <code>Input</code> String
     * <br> should be called by the Message Processing Driver Object </br>
     *
     * @param input - Object which needs to be processed (String or DOM)
     *
     * @param  mpcontext The context
     *
     * @return Original input.
     *
     * @exception MessageException if processing fails
     *
     * @exception ProcessingException if processing fails
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject msgObj ) 
        throws MessageException, ProcessingException
    {
        if (msgObj==null) 
            return null;

        if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "The asynchronous CORBA client is publishing an event.");

        String event = msgObj.getString( );

        try 
        {
            initializeEventService( mpcontext, msgObj );

            publishEvents( event );
        }
        catch (ProcessingException ce)
        {
            throw ce;
        }
        catch(org.omg.CORBA.TRANSIENT badError)
        {
           Debug.log(Debug.ALL_WARNINGS,"Oops! Looks like we just hit a stale corba object reference: "+badError.toString() + ", Reinitializing...");
           initializeEventService( mpcontext, msgObj,true);
           try
           {
               Debug.log(Debug.ALL_WARNINGS,"Retry pushing event after reinitialization...");
               publishEvents( event );
           }catch (Exception e)
           {
                throw new ProcessingException("ERROR: AsyncCorbaClient: Failed in publishing Event into the Event Channel, Reason: " +e.toString());
           }
        }
        catch (Exception e)
        {
            throw new ProcessingException("ERROR: AsyncCorbaClient: Failed in publishing Event into the Event Channel, Reason: " +e.toString());
        }
        finally 
        {
            //SP : The client was not unregistering itself from the Event Server
            // This might be the cause for the event server slow down]
            if(Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "Disconnecting from the Event Channel ...");
            disconnect();
            if(Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "Disconnected from the Event Channel.");
        }

        return formatNVPair( msgObj );
    }

}


