/**
 * Copyright (c) 2003 Neustar, Inc. All rights reserved.
 */
package com.nightfire.comms.corba;

import java.util.HashMap;
import java.util.Map;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.corba.CorbaPortabilityLayer;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;

import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * This is an AsyncCorbaClient that distributes events to multiple,
 * iterated event channels. This allows the events to be processed
 * in parallel by consumers. 
 */
public class MultiChannelAsyncClient extends AsyncCorbaClient  {

   /**
    * This maps an event channel name to a counter instance. The
    * counter is used in distibuting events evenly to the
    * parallel event channels. See getNextEventChannelIndex().
    */
   private static Map counterTable = new HashMap();

   /**
    * This is the number of event channels with the configured
    * EVENT_CHANNEL_NAME to which events will be distributed.
    */
   public static final String EVENT_CHANNEL_COUNT_PROP = "EVENT_CHANNEL_COUNT";

   /**
    * This configured number of iterated event channels to which events will be
    * distrubuted.
    *
    * For the sake of backwards compatibility, the default number of event
    * channels is 1, in which case, this processor behaves like the
    * AsyncCorbaClient.
    */
   private int channelCount = 1;

   /**
    * Constuctor calls corresponding super constructor.
    */
   public MultiChannelAsyncClient()
   {
      super();
   }

   /**
    * Constuctor calls corresponding super constructor.
    */
   public MultiChannelAsyncClient(CorbaPortabilityLayer cpl)
   {
      super(cpl);
   }

    /**
     * Initializes this object given the <code>Key</code> and <code>Type</code>
     * <br> Properties are loaded from the Database based on the <code>
     * <b> Key </b></code> and
     * <code><b> Type </code></b><br>
     *
     * @exception ProcessingException Throws when Initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG,
                  "Initializing the multi-channel asynchronous CORBA client.");

        super.initialize(key,type);

        String count = getPropertyValue(EVENT_CHANNEL_COUNT_PROP);

        if(count != null){

           try{

              channelCount = Integer.parseInt( count );

              if(channelCount <= 0){

                 throw new ProcessingException("The value ["+count+
                                               "] for property ["+
                                               EVENT_CHANNEL_COUNT_PROP+
                                               "] must be greater than zero.");

              }

           }
           catch(NumberFormatException nfex){

              Debug.error( nfex.toString() );

              throw new ProcessingException("The value ["+count+
                                            "] for property ["+
                                            EVENT_CHANNEL_COUNT_PROP+
                                            "] is not a valid numeric value.");

           }

        }

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
           Debug.log( Debug.SYSTEM_CONFIG,
                      "The multi-channel asynchronous CORBA client will "+
                      "distribute events to ["+channelCount+
                      "] event channel(s).");
        }

    }

    /**
     * This is called by the initializeEventService() method of
     * the AsyncCorbaClient class in order to
     * determine the numbered event channel to which an event should be
     * published.
     *
     * This method gets the event channel name from the
     * EVENT_CHANNEL_NAME property. It then gets the next event channel index
     * value. If the index is zero, then the event is published to the
     * event channel name as is. If the index is greater than zero,
     * then the index is appended to the end of the event channel name
     * separated by an underscore. (e.g. NightFire.SPI.EventChannel_2).
     *
     * @return the event channel name to use for the current event.
     */
    protected String getEventChannelName(MessageProcessorContext mpcontext,
                                         MessageObject msgObj)
                                         throws ProcessingException,
                                                MessageException {

        String eventChannelName =
           getRequiredPropertyValue( EVENT_CHANNEL_NAME_PROP );

        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) ){
                Debug.log(Debug.IO_STATUS,
                          "Configured event channel name is [" +
                          eventChannelName + "]." );
        }

        int index = getNextEventChannelIndex( eventChannelName,
                                              channelCount );

        if( index > 0 ){
           eventChannelName += "_"+index;
        }

        if ( Debug.isLevelEnabled( Debug.IO_STATUS ) ){
                Debug.log(Debug.IO_STATUS,
                          "Using indexed event channel name [" +
                          eventChannelName + "]." );
        }

        return eventChannelName;

    }

    /**
    * This gets the event channel index that should be appended to the
    * configured event channel name in order to distibute events
    * evenly. A counter is first retrieved for the give channel name.
    * If no counter exists for the given channel name, then
    * a new one is created. The next counter value is retrieved and
    * mod'ed against the max number of channels to return the
    * next index.
    *
    * @param channelName the base channel name as defined in this
    *                    processor's EVENT_CHANNEL_NAME property.
    * @param maxChannels the total number of iterated channels.
    */
    private static int getNextEventChannelIndex(String channelName,
                                                int maxChannels){

       Counter counter = (Counter) counterTable.get(channelName);

       if(counter == null){

          synchronized(counterTable){

             // double-check that another thread did not already create
             // a new counter while we were blocked
             counter = (Counter) counterTable.get(channelName);

             if(counter == null){

                // create a new counter for this event channel
                counter = new Counter();
                counterTable.put(channelName, counter);

             }

          }

       }  

       // Get the next channel index from the counter. Restrict the
       // range of the index by mod'ing by the number of possible channels. 
       int nextIndex = counter.next() % maxChannels;
             
       // The channel index must always be a positive number.
       // The counter may overflow and become negative at some point. 
       return Math.abs(nextIndex);

    }

    /**
    * An "int" can't be stored in a hash map and an "Integer" cannot
    * be modified, so this simple counter class is needed.  
    */
    private static class Counter {
       
       private int index = 0;

       /**
       * Return the current count value, then increment the counter by one. 
       */
       public int next(){

          return index++;

       }

    }


}