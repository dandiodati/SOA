/**
 * Copyright (c) 2003 Neustar, Inc. All rights reserved.
 */
package com.nightfire.comms.eventchannel;

import org.omg.CORBA.ORB;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.corba.CorbaException;
import com.nightfire.framework.corba.CorbaPortabilityLayer;
import com.nightfire.framework.corba.EventPushConsumer;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;

import com.nightfire.spi.common.supervisor.Supervisor;

/**
 * This is an EventConsumerServer that allows for the comsumption of
 * events in parallel by consuming from multiple, iterative event
 * channels. 
 */
public class MultiChannelConsumerServer extends EventConsumerServer {

   /**
    * This property contains the number of event channels with the configured
    * EVENT_CHANNEL_NAME from which events will be consumed. 
    */
   public static final String EVENT_CHANNEL_COUNT_PROP = "EVENT_CHANNEL_COUNT";

   /**
    * This configured number of event channels from which events will
    * be consumed.
    * For the sake of backwards compatibility, the default number of event
    * channels is 1, in which case, this server behaves just like the
    * EventConsumerServer.
    */
   private int channelCount = 1;

   /**
    * Instead of a single EventPushConsumer instance as in the parent class,
    * this class defines multiple consumers to receive events in parallel
    * from multiple channels. 
    */
   private EventPushConsumer[] helpers;

   /**
    * Constructor used by the Object Factory to create instances
    * of this server object.
    *
    * @param   key   Property-key to use for locating initialization properties.
    * @param   type  Property-type to use for locating initialization properties.
    *
    * @exception  ProcessingException  Thrown if initialization fails.
    */
   public MultiChannelConsumerServer(String key, String type)
                                     throws ProcessingException
   {
        super(key, type);

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE,
                  "MultiChannelConsumerServer: Initializing the Multi-Channel "+
                  "Event Consumer Server");

        String count = getPropertyValue(EVENT_CHANNEL_COUNT_PROP);

        if(count != null)
        {
           try
           {

              channelCount = Integer.parseInt( count );

              if(channelCount <= 0)
              {
                 throw new ProcessingException("The value ["+count+
                                               "] for property ["+
                                               EVENT_CHANNEL_COUNT_PROP+
                                               "] must be greater than zero.");
              }

           }
           catch(NumberFormatException nfex)
           {
              Debug.error( nfex.toString() );

              throw new ProcessingException("The value ["+count+
                                            "] for property ["+
                                            EVENT_CHANNEL_COUNT_PROP+
                                            "] is not a valid numeric value.");
           }

        }

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
           Debug.log(Debug.OBJECT_LIFECYCLE,
                      "The Multi-Channel Event Consumer Server will consume "+
                      "events from ["+channelCount+"] event channel(s).");
        

   }


   /**
    * Here we register on the configured number of event channels and
    * wait for the EventChannel to call back with events.
    */
   public void run() {

        try {
            //Initialize Corba Service
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                          ": Getting the CorbaPortabilityLayer from Supervisor... ");

            CorbaPortabilityLayer cpl =  Supervisor.getSupervisor().getCPL();
            ORB orb = cpl.getORB();

            // create the array to hold the EventPushConsumer instances
            helpers = new EventPushConsumer[ channelCount ];

            String currentEventChannelName;

            for(int i = 0; i < channelCount; i++)
            {

               currentEventChannelName = getEventChannelName(i);

               // create an instance of EventPushConsumer and register this
               // class so it will be called back when events are
               // posted to the current event channel               
               if (Debug.isLevelEnabled(Debug.IO_STATUS))
                   Debug.log(Debug.IO_STATUS,
                             StringUtils.getClassName(this) +
                             ": Registering for events on " +
                             currentEventChannelName );

               helpers[i] = new EventPushConsumer(orb,
                                                  currentEventChannelName,
                                                  false );
               helpers[i].register(this);

            }

        }
        catch (CorbaException ce)
        {
            Debug.logStackTrace(ce);
        }

   }

   /**
    * Shuts-down the server object.
    */
   public void shutdown () {

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                ": Received a request to shutdown.");

        for(int i = 0; i < channelCount; i++){

            if (Debug.isLevelEnabled(Debug.IO_STATUS))
            {
                String currentEventChannelName = getEventChannelName(i);

                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Disconnecting from " +
                                       currentEventChannelName );
            }

            try
            {
               helpers[i].disconnect();
            }
            catch(Exception ex)
            {
               Debug.error(StringUtils.getClassName(this)+
                           "Could not disconnect from event channel: "+
                           ex.toString());
            }

        }

   }

   /**
    * Constructs an indexed event channel name using the event channel
    * name read from a property by the parent class and the given index.
    */
   private String getEventChannelName(int index){

      String indexedEventChannelName; 

      // if the index is 0, then just use the event channel name
      // as is, otherwise, append an index to the end of the name
      if(index == 0)
      {
         indexedEventChannelName = super.eventChannelName;
      }
      else
      {
         indexedEventChannelName = super.eventChannelName+"_"+index;
      }

      return indexedEventChannelName;

   }

}