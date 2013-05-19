/*
 * Copyright (c) 2000-2002 Nightfire Software, Inc. All rights reserved.
 * $Header: //comms/R4.4/com/nightfire/comms/eventchannel/EventChannelClient.java#1 $
 */


package com.nightfire.comms.eventchannel;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.message.*;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;

import com.nightfire.comms.soap.SOAPConstants;
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.supervisor.*;
import java.util.*;
import java.io.*;


/**
 * This is a Client Message Processor to the Nightfire EventChannel.
 */
public class EventChannelClient extends MessageProcessorBase implements PushConsumerCallBack {


    /**
     * Property that specifies the event channel to register with for events.
     */
    private static final String EVENT_CHANNEL_PROP = "EVENT_CHANNEL_NAME";

    /**
     * Property that specifies time in seconds to wait for events.
     */
    private static final String TIME_OUT_PROP      = "TIME_OUT";

    private EventPushConsumer helper                = null;

    //Initializes corba service
    protected CorbaPortabilityLayer cpl             = null;

    //Gets Persistent Property value EVENT_CHANNEL_NAME
    protected String eventChannelName              = null;

    //either will get an event or will TIME_OUT
    protected String message                       = SOAPConstants.TIMED_OUT;

    //time is seconds this client should wait for an event
    protected int timeOut;



    /** Creates new EventChannelClient */
    public EventChannelClient() {
    }

    /**
     * Called to initialize a message processor object.
     *
     * @param  key   Property-key used to locate initialization properties.
     * @param  type  Property-type used to locate initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public void initialize(String key, String type) throws ProcessingException {

        super.initialize(key, type);

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, 
                    StringUtils.getClassName(this) + " initializing...");

        // The Properties are obtained from a protected Hashtable Object
        StringBuffer sb = new StringBuffer();
        try {

        eventChannelName = getRequiredPropertyValue(EVENT_CHANNEL_PROP, sb);

        timeOut = Integer.parseInt(getRequiredPropertyValue(TIME_OUT_PROP, sb) );


         if (sb.length() > 0)
            throw new ProcessingException(StringUtils.getClassName(this) +
                ": EVENT_CHANNEL_NAME not specified in persistent properties.\n" +
                sb.toString() );
        } catch ( NumberFormatException nfex ) {
            throw new ProcessingException (StringUtils.getClassName(this) +
                "value [" + timeOut + "] for property [" + TIME_OUT_PROP + "] not a valid integer." +
                sb.toString() );
        }


    }

    /**
     * This is a client to the Nightfire EventChannel.
     * It will register on the specified event channel and wait the specified time in seconds
     * for an event.  Upon receiving an event the client will disconnect from
     * the channel ensuring we receive a single event.
     *
     * @param  context  The  message context.
     *
     * @param  input  Input message object to process.
     *
     * @return  An array of name-value pair objectss, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    public NVPair[] process(MessageProcessorContext context, MessageObject input) throws MessageException, ProcessingException {

	 if (input == null) return null;
     ThreadMonitor.ThreadInfo tmti = ThreadMonitor.start("Registering on eventChannel :"+System.currentTimeMillis());

         try {
            //Initialize Corba Service
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Getting the CorbaPortabilityLayer from Supervisor... ");


            //get instance of EventPushConsumer and register this class so it will be called back when events are
            //posted to the event channel
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Registering for events on " + eventChannelName);

            cpl =  Supervisor.getSupervisor().getCPL();
            helper = new EventPushConsumer(cpl.getORB(), eventChannelName, false );
            helper.register(this);

	    synchronized (this) {
		  wait(timeOut * 1000 );
	    }

            //make sure we disconnect
            helper.disconnect();

        }
        catch (CorbaException ce) {
            throw new ProcessingException(ce.getMessage());
        } catch (InterruptedException iex ) {
	    throw new ProcessingException(iex.getMessage());
    	}
        finally {
            ThreadMonitor.stop(tmti);   
        }
        return formatNVPair( message );
    }


    public void processEvent(String message) {

        ThreadMonitor.ThreadInfo tmti = ThreadMonitor.start("Processing Event:"+message);
	try {
            synchronized (this) {
                //we could be waiting or we have timed out.

                if (Debug.isLevelEnabled(Debug.IO_STATUS))
                    Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Processing : " + message);
                helper.disconnect();

                this.message = message;
                notify();
            }
        }
        catch (Exception me) {
            throw new RuntimeException(me.getMessage());
        }
        finally {
            ThreadMonitor.stop(tmti);
        }
    }

}

