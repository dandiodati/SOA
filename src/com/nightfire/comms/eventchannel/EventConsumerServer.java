/*
 * Copyright (c) 2000-2002 Nightfire Software, Inc. All rights reserved.
 * $Header: //comms/R4.4/com/nightfire/comms/eventchannel/EventConsumerServer.java#1 $
 */

package com.nightfire.comms.eventchannel;

import com.nightfire.spi.common.communications.*;
import com.nightfire.spi.common.supervisor.*;

import com.nightfire.framework.message.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.common.*;

/**
 *
 *  This component consumes events from an NF Reliable Event Service and serves as an entry point for
 *  response processing.
 */
public class EventConsumerServer extends ComServerBase implements PushConsumerCallBack {

  /**
   * Property which specifies the Event Channel to register with for events.
   */
  protected static final String EVENT_CHANNEL_PROP  = "EVENT_CHANNEL_NAME";

  private EventPushConsumer helper                  = null;

  //Gets Persistent Property value EVENT_CHANNEL_NAME
  protected String eventChannelName                 = null;


   /**
     * Constructor used by Object FactoryS
     *
     * @param   key   Property-key to use for locating initialization properties.
     * @param   type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */

    public EventConsumerServer (String key, String type) throws ProcessingException
    {
        super(key, type);

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "EventConsumerServer: Initializing the Communications EventConsumer Server");

        eventChannelName = getRequiredPropertyValue(EVENT_CHANNEL_PROP);
    }

   /**
    * Shuts-down the server object.
    */
   public void shutdown () {

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                ": Received a request to shutdown.  Notifying ShutdownHandler...");

            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Disconnecting from " + eventChannelName);
            helper.disconnect();
    }


    /**
     * Here we register for the on the specified event channel and wait
     * for the EventChannel to call back with events.
     */
    public void run() {
        try {
            //Initialize Corba Service
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Getting the CorbaPortabilityLayer from Supervisor... ");

            CorbaPortabilityLayer cpl =  Supervisor.getSupervisor().getCPL();

            //get instance of EventPushConsumer and register this class so it will be called back when events are
            //posted to the event channel
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Registering for events on " + eventChannelName);

            helper = new EventPushConsumer(cpl.getORB(), eventChannelName, false );
            helper.register(this);

        }
        catch (CorbaException ce) {
            Debug.logStackTrace(ce);
        }

    }


    /**
     * Thsi method is called by the EventChannel when events are available.
     *
     * @param message - the event
     */
    public void processEvent(String message) {
        try {
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Processing : " + message);
            process(null, message);

        //we need a way to propogate an exception back to the ReliableChannel so that the event is not
        //wrongly removed from the event channel.  the PushCallBackConsumer Interface does not define
        //a method signature that throws an Exception so we catch what we can and wrap it up as a Runtime Exception
        //which will result in the desired behavior, i.e. the event remains in the channel.
        }
        catch (MessageException me) {
            throw new RuntimeException(me.getMessage());
        }
        catch (ProcessingException pe) {
            throw new RuntimeException(pe.getMessage());
        }

    }

}