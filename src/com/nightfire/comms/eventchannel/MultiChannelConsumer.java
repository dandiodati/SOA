/**
 * Copyright (c) 2003 Neustar, Inc. All rights reserved.
 */
package com.nightfire.comms.eventchannel;

import org.omg.CORBA.ORB;

import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.*;
import com.nightfire.servers.CosEvent.*;

/**
 * This is an EventConsumer used by MultiCustomerEventConsumerServer
 * that allows for the comsumption of
 * events in parallel by consuming from multiple, iterative event
 * channels.
 */
public class MultiChannelConsumer implements PushConsumerCallBack {

    /**
      * The ORB.
      */
     private ORB orb;

     /**
       * The customer identifier.
       */
      private String cid;

  /**
    * The event channel name.
    */
  private String channelName;

  /**
    * The com server.
    */
  private PushConsumerCallBack comServerCallBack;

  private String orbAddr;
  private String orbPort;

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
    * @param   orb   The ORB.
    * @param   cid   The customer identifier.
    * @param   channelName   The event channel name.
    * @param   channelCount   Number of iterative event channels to register.
    * @param   comServerCallBack   The server that creates the consumer.
    *
    * @exception  FrameworkException  Thrown if initialization fails.
    */
    public MultiChannelConsumer(ORB orb, String cid, String channelName, int channelCount, PushConsumerCallBack comServerCallBack,
                                String orbAgentAddr, String orbAgentPort) throws FrameworkException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE,
                  "MultiChannelConsumer: Initializing the Multi-Channel "+
                  "Event Consumer");

        if(channelCount <= 0)
        {
            throw new FrameworkException("The name of event channel cannot be null.");
        }

        this.orb = orb;
        this.cid = cid;
        this.channelName = channelName;
        this.channelCount = channelCount;
        this.comServerCallBack = comServerCallBack;
        this.orbAddr = orbAgentAddr;
        this.orbPort = orbAgentPort;

        if( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
            Debug.log(Debug.OBJECT_LIFECYCLE,
                       "The Multi-Channel Event Consumer will consume "+
                       "events from ["+channelCount+"] event channel(s).");
        }

    }

    /**
     * Registers the call back to be ready to receive events.
     *
     * @exception  CorbaException  Thrown if cannot connect to the event channel.
     */
   public void register() throws CorbaException
    {
        register(false);
    }

    /**
     * Registers the call back to be ready to receive events.
     *
     * @exception  CorbaException  Thrown if cannot connect to the event channel.
     */
   public void register(boolean flush) throws CorbaException
   {

        try
        {
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
                                                  false, orbAddr, orbPort,flush);
               helpers[i].register(this);

            }

        }
        catch (CorbaException ce)
        {
            Debug.logStackTrace(ce);
        }

   }

    /**
     * Disconnects from the event channel.
     */
   public void disconnect () {

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                ": Received a request to disconnect.");

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
     * Thsi method is called by the EventChannel when events are available.
     *
     * @param message - the event
     */
    public void processEvent(String message) {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
        {
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                      ": Processing : " + message);
        }

        try
        {
            // set the new customer ID
            CustomerContext context = CustomerContext.getInstance();
            context.setCustomerID(cid);

            //set the event channel name
            context.set( MultiCustomerEventConsumerServer.EVENT_CHANNEL_NAME_PROP, channelName );

            //set the event channel base name
            context.set( CHANNEL_NAME_BASE, EventChannelUtil.removeCIDTrailer( channelName, cid ) );

            Debug.log( Debug.MSG_STATUS, context.describe() );
        }
        catch(FrameworkException fex)
        {
            // at the time of this writing, while the setCustomerID() says
            // that it throws a FrameworkException, the actual code never
            // throws that exception, so this should never happen, but ...
            Debug.error("Could not set the customer ID to ["+
                                       cid+"]: "+fex.getMessage());

        }

        comServerCallBack.processEvent(message);

    }

    /**
     * Get a human-readable description of the channel.
     *
     * @return  A description of the channel.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "Customer Identifier [" );
        sb.append( cid );
        sb.append( "]\n" );

        sb.append( "Event Channel [" );
        sb.append( channelName );
        sb.append( "]\n" );

        sb.append( "Event Channel count[" );
        sb.append( channelCount );
        sb.append( "]\n" );

        return( sb.toString() );
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
         indexedEventChannelName = channelName;
      }
      else
      {
         indexedEventChannelName = channelName+"_"+index;
      }

      return indexedEventChannelName;

   }

   public static final String CHANNEL_NAME_BASE = "CHANNEL_NAME_BASE";

}
