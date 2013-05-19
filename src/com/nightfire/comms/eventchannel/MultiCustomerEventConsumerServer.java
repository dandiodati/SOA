/**
 * Copyright (c) 2003 Neustar, Inc. All rights reserved.
 */
package com.nightfire.comms.eventchannel;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.omg.CORBA.ORB;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.cache.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.servers.CosEvent.*;

import com.nightfire.spi.common.communications.*;
import com.nightfire.spi.common.supervisor.*;
import com.nightfire.spi.common.driver.*;

/**
 * This is an EventConsumerServer that allows, for the list of configured customers
 * and event channels, for the comsumption of
 * events in parallel by consuming from multiple, iterative event
 * channels.
 * With the change in this component, repository configuration can be used to list
 * the channels associated with each customer for which consumers are to be created.
 */
public class MultiCustomerEventConsumerServer extends ComServerBase
    implements PushConsumerCallBack
{

   /**
    * This iterative property contains the name of event channel from which events will be consumed.
    */
   public static final String EVENT_CHANNEL_NAME_PROP = "EVENT_CHANNEL_NAME";

   /**
    * This iterative property contains the list of customer id from which events will be consumed.
    */
   public static final String CUSTOMER_CID_PROP = "CUSTOMER_ID";

   /**
    * This iterative property contains the number of event channels with the configured
    * EVENT_CHANNEL_NAME from which events will be consumed.
    */
   public static final String EVENT_CHANNEL_COUNT_PROP = "EVENT_CHANNEL_COUNT";

   /**
    * Repository category giving event channel configurations for the consumers to be created on.
    */
   public static final String REPOSITORY_CATEGORY_PROP = "REPOSITORY_EVENT_CHANNEL_CATEGORY";

   public static final String ALT_ORBS_POA_NAME   = "ALTERNATE_ORBS";

   public static final String ORB_AGENT_PORT_PROP  = "ORB_AGENT_PORT";
   public static final String ORB_AGENT_ADDR_PROP  = "ORB_AGENT_ADDR";

   public static final String CUSTOMER_CONTEXT = "customer-context";

   private EventChannelUtil.EventChannelConfig[] channelConfigs = null;

   private MultiChannelConsumer[] helpers;

   private String addr = null;
   private String port = null;

   /**
    * Constructor used by the Object Factory to create instances
    * of this server object.
    *
    * @param   key   Property-key to use for locating initialization properties.
    * @param   type  Property-type to use for locating initialization properties.
    *
    * @exception  ProcessingException  Thrown if initialization fails.
    */
   public MultiCustomerEventConsumerServer(String key, String type) throws ProcessingException
   {
        super(key, type);

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE,"MultiCustomerEventConsumerServer: Initializing the Multi-Customer "+
                                            "Event Consumer Server");

        try
        {
            createConsumers();
        }
        catch (Exception e)
        {
            throw new ProcessingException(e.getMessage());
        }

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
           Debug.log(Debug.OBJECT_LIFECYCLE,
                      "The Multi-Customer Event Consumer Server will consume "+
                      "events from ["+ helpers.length +"] customer(s).");

        // Allow refreshing of cache.
        CacheManager.getRegistrar().register(new CacheFlushAdapter());

   }


   /**
    * Here we register on the configured number of event channels and
    * wait for the EventChannel to call back with events.
    */
   public void run()
  {
      registerAll();
  }


   private void registerAll()
     {
        try {
            for(int i = 0; i < helpers.length; i++)
               helpers[i].register();
        }
        catch (CorbaException ce)
        {
            Debug.logStackTrace(ce);
        }
   }

  /**
   * First removes the corba object from Object locator cache, 
   * to avoid stale object references. 
   */
   private void reRegisterAll()
   {
        try 
        {
            for(int i = 0; i < helpers.length; i++)
                 helpers[i].register(true);
        }
        catch(CorbaException ce)
        {
            Debug.logStackTrace(ce);
        }
   }

   /**
    * Shuts-down the server object.
    */
   public void shutdown () {

     if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, getClass().getSimpleName()+
                                     ": Received a request to shutdown.");

       disconnectAll();
   }

   public void disconnectAll () {

        for (int i = 0; i < helpers.length; i++)
        {
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
        try {
            if(Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                       ": Processing : " + message);
            process(null, message);

        //we need a way to propogate an exception back to the ReliableChannel so that the event is not
        //wrongly removed from the event channel.  the PushCallBackConsumer Interface does not define
        //a method signature that throws an Exception so we catch what we can and wrap it up as a Runtime Exception
        //which will result in the desired behavior, i.e. the event remains in the channel.
        }
        catch (MessageException me) {
            Debug.logStackTrace(me);
            throw new RuntimeException(me.getMessage());
        }
        catch (ProcessingException pe) {
            Debug.logStackTrace(pe);
            throw new RuntimeException(pe.getMessage());
        }
        catch (Exception e) {
            Debug.logStackTrace(e);
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Callback allowing sub-classes to provide additional driver configuration.
     * Sets the customer context content in the driver chain context.
     *
     * @param  driver  Message-processing driver to configure.
     *
     * @exception  ProcessingException  Thrown if processing can't be performed.
     *
     * @exception  MessageException  Thrown if message is bad.
     */
    protected void configureDriver (MessageProcessingDriver driver) throws ProcessingException, MessageException
    {
        try
        {
            CustomerContext cusContext = CustomerContext.getInstance();
            if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, cusContext.describe());

			// Get the event channel name from the customer context
			String eventChannelName = (String)cusContext.get( MultiCustomerEventConsumerServer.EVENT_CHANNEL_NAME_PROP);
			if(Debug.isLevelEnabled( Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS ,"Got Channel [" + eventChannelName + "]");

			// Iterate over the channel config objects to check if their is a url specified for the event channel.
			// If present set the event channel name-url mapping into customer context
			for(int Ix = 0;  (channelConfigs!=null) && (Ix < channelConfigs.length);  Ix ++)
			 {
					EventChannelUtil.EventChannelConfig channelConfig = channelConfigs[Ix];
				    String channelName = channelConfig.getChannelName();

					if(channelName.equals(eventChannelName))
					{
					   String soapSerURL =  channelConfig.getSoapResponseHandlerURL();

					   if (StringUtils.hasValue(soapSerURL))
					   {
                           String channelNameWithoutCustomerTrail = EventChannelUtil.removeCIDTrailer( channelName, EventChannelUtil.getCustomerID(channelName) );
                           //replacing "." in channel name with "_" as customer context will be set as a document
                           // in message context later in the chain.
                            
	   					   cusContext.set( StringUtils.replaceSubstrings(channelNameWithoutCustomerTrail,".", "_") , soapSerURL );

                           if(Debug.isLevelEnabled( Debug.MSG_STATUS))
								Debug.log(Debug.MSG_STATUS,
                                        "Added Channel [" + channelName  
                                            +"] and URL [" + soapSerURL + "] Pair to Customer Context");
					   }
					}
			 }
		    

            //The customercontext contains the base event channel name and the event
            //channel name with customer id.
            //We want this event channel name to be used later on to perform certain actions.
            //Since the message processors have built-in logic for accessing message processor
            //contexts, we would like to use that. So we copy over all the contents from the
            //CustomerContext to the MessageProcessorContext as a Document located at
            //customer-context.
            MessageProcessorContext mpContext = driver.getContext();
            mpContext.set(CUSTOMER_CONTEXT,  cusContext.getAll());
            
            if(Debug.isLevelEnabled( Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, mpContext.describe());

        }
        catch(FrameworkException ex)
        {
            throw new ProcessingException(ex.getMessage());
        }
    }

    /**
     * Creates consumers for all of the configured customers.
     *
     * @exception  FrameworkException  Thrown if initialization fails.
     */
    private void createConsumers() throws FrameworkException, ProcessingException
    {
        ORB orb = null;
        List customerConsumers = new ArrayList();
        //Maintain a list of channels for which consumers are already created. This list
        //contains the list of channel names without the count appended at the end.
        List channelsWithConsumer = new ArrayList();

        addr = getPropertyValue(ORB_AGENT_ADDR_PROP);
        port = getPropertyValue(ORB_AGENT_PORT_PROP);

        if(StringUtils.hasValue(addr) && StringUtils.hasValue(port))
        {
            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, 
                        "MultiCustomerEventConsumerServer.createConsumers(): " +
                        "Intializing an alternative ORB with address [" + addr + "] and port [" + port + "] ");

            try
            {
                orb = createORB(addr, port);
            }
            catch (CorbaException e)
            {
                Debug.error("ERROR: MultiCustomerEventConsumerServer.createConsumers():" +
                        " Encountered an error while creating orb with address and port [" + addr + ", " + port + "]:\n" + e.toString());
                throw new ProcessingException(e);
            }
        }
        else
        {
            if(Supervisor.getSupervisor()!=null && 
                    Supervisor.getSupervisor().getCPL()!=null)
            {
                orb = Supervisor.getSupervisor().getCPL().getORB();
            }
            
            if(orb==null)
            {
                /* get ORB addr and ORB port from CommonConfig */
                try
                {
                    orb = createORB(CommonConfigUtils.getCORBAServerAddr(), 
                            CommonConfigUtils.getCORBAServerPort());
                    
                    if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                        Debug.log(Debug.OBJECT_LIFECYCLE,"Created ORB from addr and port defined in CommonConfig");
                
                }
                catch(CorbaException ce)
                {
                    Debug.error("ERROR: MultiCustomerEventConsumerServer.createConsumers(): " +
                            "Encountered an error while creating ORB from addr and port defined in CommonConfig :\n" + ce.toString());
                    throw new ProcessingException(ce);
                }

            }
        }

        //Create consumers for channels listed in the repository.
        createRepositoryConfiguredEventConsumers(customerConsumers, channelsWithConsumer, orb);

        //////////////////////////////////////////////////////////////////////////////////////////
        //Create consumers for the channels listed in the traditional way in the PersistentProperty.
        // Get configured CIDs.
        NVPair[] channelCids = PersistentProperty.getPropertiesLike(new Hashtable(properties), CUSTOMER_CID_PROP);

        // Loop until all iterated properties describing channels have been accessed.
        for(int Ix = 0;  Ix < channelCids.length;  Ix ++)
        {
            String channelCidProp = channelCids[Ix].name;

            // Extract the numeric suffix value to get the associated channel configuration properties.
            int suffixLoc = channelCidProp.lastIndexOf( '_' );

            if(suffixLoc == -1)
            {
                throw new FrameworkException( "Event channel cid [" + channelCidProp
                                              + "] is missing required '_#' grouping suffix." );
            }

            int groupSuffix = StringUtils.getInteger(channelCidProp.substring( suffixLoc + 1));

            String channelCid = getPropertyValue(channelCidProp);

            // Find the additional properties for the CID
            String channelName = getPropertyWithDefault(EVENT_CHANNEL_NAME_PROP, groupSuffix);

            // Append CID to channel name if not appended already
            channelName = PropUtils.appendCustomerId(channelName, channelCid);

            String strTemp = getPropertyWithDefault(EVENT_CHANNEL_COUNT_PROP, groupSuffix);


            int channelCount = 1;
            if(StringUtils.hasValue(strTemp))
            {
                channelCount = Integer.parseInt(strTemp);
            }

            //Only create a consumer if the channel was not listed in the repository.
            if(!channelsWithConsumer.contains(channelName) && (StringUtils.hasValue(channelName)))
            {
                // Create the consumer for the customer.
                MultiChannelConsumer customerConsumer = new MultiChannelConsumer(orb, channelCid, channelName, channelCount, this, addr, port);

                if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                    Debug.log(Debug.OBJECT_LIFECYCLE, 
                            "MultiCustomerEventConsumerServer: Successfully created event consumer for customer ["
                               + channelCid + "]:\n" + customerConsumer.describe());

                customerConsumers.add(customerConsumer);
                //Since the MultiChannelConsumer takes care of creating consumers for
                //iterative channels, we just need to track the real channel name.
                channelsWithConsumer.add(channelName);
            }
            else
            {
                if(Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE))
                    Debug.log(Debug.OBJECT_LIFECYCLE, 
                            "Channel [" + channelName + "] already has a registered consumer." );
            }

        }//for

        //////////////////////////////////////////////////////////////////////////////////////////

        helpers = (MultiChannelConsumer[])customerConsumers.toArray(
                                                                     new MultiChannelConsumer[customerConsumers.size()]
                                                                     );

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "MultiCustomerEventConsumerServer: Created [" + helpers.length
                   + "] event consumers." );
        

    }


    protected String getPropertyWithDefault(String propName, int groupSuffix)
    {
        String propValue = getProperty( propName, groupSuffix );

        if(!StringUtils.hasValue(propValue))
        {
            // Get the value from the default, which is without suffix.
            propValue = getPropertyValue(propName);
        }

        return propValue;
    }

    /**
     * Get an iterated property value.
     *
     * @param  name  The property name prefix.
     * @param  count  The iteration count.
     *
     * @return  The property value, or null if absent.
     */
    private String getProperty ( String name, int counter )
    {
        return( getPropertyValue( PersistentProperty.getPropNameIteration( name, counter ) ) );
    }

    /**
     * Creates consumers for all of the repository configured event channels.
     *
     * @param customerConsumers List of consumers created.
     * @param channelsWithConsumer List of channels that have consumers. This list
     *        contains the channel name without the count appended at the end of the name.
     * @param orb The ORB to use to create the consumer.
     *
     * @exception  FrameworkException  Thrown if processing fails.
     */
    private void createRepositoryConfiguredEventConsumers( List customerConsumers,
    List channelsWithConsumer, ORB orb ) throws FrameworkException
    {
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Configuring event consumers from the repository ..." );

        //Value used for api case is apiConsumer.
        String repositoryCategoryName = (String)properties.get(REPOSITORY_CATEGORY_PROP);


       // EventChannelUtil.EventChannelConfig[] channelConfigs = null;
        try
        {
            channelConfigs = EventChannelUtil.getRepositoryConfiguredEventChannels(
                                              properties,
                                              EventServer.REPOSITORY_EVENT_CHANNEL_CONFIG_PROP,
                                              repositoryCategoryName );
        }
        catch ( Exception e )
        {
            throw new FrameworkException( "Could not get repository configuration for event channels: " + e );
        }

        for ( int Ix = 0;  (channelConfigs!=null) && (Ix < channelConfigs.length);  Ix ++ )
        {
            EventChannelUtil.EventChannelConfig channelConfig = channelConfigs[Ix];
            String channelName = channelConfig.getChannelName();

            // Don't continue with channel creation if the named channel already has a consumer.
            if ( channelsWithConsumer.contains( channelName ) )
            {
                String message = "Event Channel named [" + channelName + "] has a consumer, so skipping channel.";

                continue;
            }

            int channelCount = getChannelCount( channelConfig.getChannelCount() );
            String channelCid = EventChannelUtil.getCustomerID ( channelName );

            // Create the consumer for the customer.
            MultiChannelConsumer customerConsumer = new MultiChannelConsumer( orb, channelCid, channelName, channelCount, this, addr, port);

            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, 
                        "MultiCustomerEventConsumerServer: Successfully created event consumer for customer ["
                           + channelCid + "]:\n" + customerConsumer.describe() );

            customerConsumers.add( customerConsumer );
            channelsWithConsumer.add( channelName );
        }

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Event Service: Created [" +  customerConsumers.size()
                       + "] consumers on channels with names:\n" + channelsWithConsumer.toString() );

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Done configuring event consumers from the repository." );
    }

    /**
     * If count is invalid, then the default value 1 is returned, else it is converted into
     * an integer value and returned.
     */
    private int getChannelCount( String count )
    {
        int channelCount = 1;
        if (StringUtils.hasValue(count))
        {
            channelCount = Integer.parseInt(count);
        }
        return channelCount;
    }

    private ORB createORB(String addr, String port) throws CorbaException
    {
        Properties props = new Properties();

        if(StringUtils.hasValue(addr))
        {
            props.put(CorbaPortabilityLayer.ORB_AGENT_ADDR_PROP, addr);
        }

        if(StringUtils.hasValue(port))
        {
            props.put(CorbaPortabilityLayer.ORB_AGENT_PORT_PROP, port);
        }

        CorbaPortabilityLayer cpl = new CorbaPortabilityLayer(null, props, ALT_ORBS_POA_NAME);
        ORB orb = cpl.getORB();
        cpl.activatePOAManager();

        return orb;
    }

    private class CacheFlushAdapter implements CachingObject
    {
        /**
         * Create a cache-flushing adapter for the event consumer server.
         *
         */
        public CacheFlushAdapter( )
        {
        }

        /**
         * Method invoked by the cache-flushing infrastructure
         * to indicate that the cache should be emptied.
         *
         * @exception FrameworkException if cache cannot be cleared.
         */
        public void flushCache ( ) throws FrameworkException
        {
            if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG,
                       "Flushing configuration cache, " +
                       "re-registering all event consumers..." );

            try
            {

                // TODO: The following line need to invoked first in by SPIServer or the Supervisor
                //       to flush the persistent properties. It can't be done in this
                //       comm server since PersistentProperties are global.
                //PersistentProperty.flushCache( );

                // The event consumers can be safely disconnected.
                if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                    Debug.log(Debug.SYSTEM_CONFIG, "Disconnecting all event consumers...");
                
                disconnectAll();


                // Refresh the in-memory persistent properties to pick up any changes.
                if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                    Debug.log(Debug.SYSTEM_CONFIG, "Reloading properties...");
                
                initialize(key, type);

                if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                    Debug.log(Debug.SYSTEM_CONFIG, "Creating all event consumers...");
                
                createConsumers();

                if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                    Debug.log(Debug.SYSTEM_CONFIG, "Re-registering all event consumers...");
                
                reRegisterAll();
            }
            catch(Exception e)
            {
                Debug.error(e.toString() + "\n" + Debug.getStackTrace(e));

                throw new FrameworkException(e);
            }

            if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, "Done flushing configuration cache to re-register event consumers.");
        }
    }
}
