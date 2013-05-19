/**
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/CosEvent/EventChannelUtil.java#1 $
 */

package com.nightfire.servers.CosEvent;

import java.util.*;

import org.omg.CORBA.*;
import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.repository.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.cache.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.servers.*;
import com.nightfire.idl.*;

/**
 * Helper class for reading event channel configuration from the repository. The format of the
 * configuration is:
 *
 * <?xml version="1.0"?>
 * <EventChannelConfiguration>
 * 	<EventChannelContainer type="container">
 * 		<EventChannel>
 *			<EVENT_CHANNEL_NAME value="NeuStar.lsr_order.BS.LSOG6.EventChannel_<CID>"/>
 *    	<PERSISTENCE_FLAG value="FALSE"/>
 *      <EVENT_CHANNEL_COUNT value="1"/>
 *		</EventChannel>
 *		<EventChannel>
 *			<EVENT_CHANNEL_NAME value="NeuStar.Clearinghouse.EventChannel_<CID>"/>
 *			<PERSISTENCE_FLAG value="FALSE"/>
 *		</EventChannel>
 *	</EventChannelContainer>
 * </EventChannelConfiguration>
 *
 * Other than the EVENT_CHANNEL_NAME property, all other properties are optional. Any property
 * at the same level as the EVENT_CHANNEL_NAME will be accessible via this class.
 */
public class EventChannelUtil
{
    /**
     * Name of container node in repository XML under which individual channel configurations live.
     */
    public static final String EVENT_CHANNEL_CONTAINER_NODE = "EventChannelContainer";

    /**
     * Name of node in repository XML that contains the channel count value.
     */
    public static final String EVENT_CHANNEL_COUNT_PROP = "EVENT_CHANNEL_COUNT";

    /**
     * Name of node in repository XML that contains the soap response handler url.
     */
    public static final String SOAP_RESPONSE_HANDLER_URL_PROP = "SOAP_RESPONSE_HANDLER_URL";


    /**
     * Delimeter to identify the  customer name from the EventChannel name.
     */
    public static final String EVENT_CHANNEL_DELIMETER = "EventChannel_";

    /**
     * Delimeter to identify the  customer name from the EventChannel name for Provider Notification channels.
     */
    public static final String EVENT_CHANNEL_DELIMETER_PN = ".PN_";

    /**
     * Gets all the repository configured event channels. The configuration is expected to live in xml
     * files with the format as specified in this class description.
     *
     * @param properties Typically persistent properties that contain properties with names like
     *        repositoryEventChannelConfig.
     * @param repositoryEventChannelConfig The property names following this pattern in the properties
     *        are assumed to contain the respository file name explicitly.
     *        For example, if repositoryEventChannelConfig is "REPOSITORY_EVENT_CHANNEL_CONFIG", then
     *        the properties "REPOSITORY_EVENT_CHANNEL_CONFIG_0", "REPOSITORY_EVENT_CHANNEL_CONFIG_1", etc
     *        are assumed to contain the complete file names to lookup.
     * @param repositoryCategory The category in the repository to look up in case repositoryEventChannelConfig
     *        yielded no configuration. For example: "eventChannel.apiConsumer".
     *
     * @exception  Exception  Thrown if processing fails.
     */
    public static EventChannelUtil.EventChannelConfig[] getRepositoryConfiguredEventChannels
                  ( Map properties, String repositoryEventChannelConfig, String repositoryCategory )
                  throws Exception
    {
        Debug.log( Debug.SYSTEM_CONFIG, "Configuring event channels from the repository ..." );

        String[] repositoryChannelConfigNames = null;
        NVPair[] channelNamePairs = null;

        if ( StringUtils.hasValue ( repositoryEventChannelConfig ) )
        {
            // Get all of the configured channel names.  The value in the NVPair will contain the repository file name.
            channelNamePairs = PersistentProperty.getPropertiesLike( new Hashtable(properties),
                                                                          repositoryEventChannelConfig );
        }
        else
        {
            Debug.log( Debug.SYSTEM_CONFIG, "Event channel configuration files are not specified in persistent properties." );
        }

        if ( (channelNamePairs != null) && (channelNamePairs.length > 0) )
        {
            repositoryChannelConfigNames = new String[ channelNamePairs.length ];

            for ( int Ix = 0;  Ix < channelNamePairs.length;  Ix ++ )
                repositoryChannelConfigNames[Ix] = (String)(channelNamePairs[Ix].value);
        }
        else
        {
            // No property configuration exists designating particular channel configuration items to use from
            // the repository, so attempt to see if any repository configuration exists.
            try
            {
                if ( StringUtils.hasValue( repositoryCategory ) )
                {

                    // The name in the NVPair will contain the repository file name.
                    channelNamePairs = RepositoryManager.getInstance().listMetaData( repositoryCategory, false,
                                                                                     RepositoryManager.XML_FILTER );

                    if ( (channelNamePairs != null) && (channelNamePairs.length > 0) )
                    {
                        repositoryChannelConfigNames = new String[ channelNamePairs.length ];

                        for ( int Ix = 0;  Ix < channelNamePairs.length;  Ix ++ )
                            repositoryChannelConfigNames[Ix] = channelNamePairs[Ix].name;
                    }
                }//If repository configuration is being used
                else
                {
                    Debug.log( Debug.SYSTEM_CONFIG, "Event channel configuration is not specified in any repository category." );
                }
            }
            catch ( Exception e )
            {
                Debug.warning( "Couldn't list repository contents for ["
                               + repositoryCategory + "]\n" + e.getMessage() );
            }
        }

        if ( (repositoryChannelConfigNames == null) || (repositoryChannelConfigNames.length < 1) )
        {
            Debug.log( Debug.SYSTEM_CONFIG, "No event channel configuration information was present in the repository." );

            return null;
        }

        //Container for EventChannelConfig objects.
        ArrayList allChannels = new ArrayList();

        // Loop until all iterated properties describing channels have been accessed.
        // A master list of all the channels described across all configuration is created
        // and put into allChannels.
        for ( int Ix = 0;  Ix < repositoryChannelConfigNames.length;  Ix ++ )
        {
            XMLMessageParser configParser = null;

            try
            {
                configParser = new XMLMessageParser( RepositoryManager.getInstance().getMetaData(
                                                     repositoryCategory, repositoryChannelConfigNames[Ix] ) );
            }
            catch ( Exception e )
            {
                Debug.error( "Couldn't read repository contents for category ["
                             + repositoryEventChannelConfig + "] criteria ["
                             + repositoryChannelConfigNames[Ix] + "]\n" + e.getMessage() );

                // Skip this one and go on to next repository configuration.
                continue;
            }

            Node[] channelConfigs = configParser.getChildNodes( EVENT_CHANNEL_CONTAINER_NODE );

            for ( int Jx = 0;  Jx < channelConfigs.length;  Jx ++ )
            {
                // Create a containner for the additional properties, and load
                // any that are found into it.
                EventChannelConfig channelConfig = new EventChannelConfig( );

                Node[] channelNode = configParser.getChildNodes( channelConfigs[Jx] );

                for ( int Kx = 0;  Kx < channelNode.length;  Kx ++ )
                {
                    channelConfig.put( channelNode[Kx].getNodeName(),
                                     configParser.getNodeValue(channelNode[Kx]) );
                }
                allChannels.add( channelConfig );

            }
        }

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log (Debug.SYSTEM_CONFIG, "Returning [" + allChannels.size() + "] channel descriptions." );

        return (EventChannelConfig[])allChannels.toArray(new EventChannelConfig[allChannels.size()]);
    }

    /**
     * Extract the customer id from the channelName. It is the string following EventChannel_.
     * It is assumed that the channelName will have the customerid.
     * @param channelName String to extract the customerid from.
     *
     * @return String the customerid.
     */
    public static String getCustomerID( String channelName )
    {
        //Get the customerid from the channel name.
        //The channel name is of the pattern <channelName>.EventChannel_<CID>
        int indx = channelName.indexOf( EVENT_CHANNEL_DELIMETER );
        String customerID = null;
        if (indx != -1)
        {
            //Extract CustomerId using the EventChannel Delimeter
            customerID = channelName.substring( indx + EVENT_CHANNEL_DELIMETER.length() );
        }
        else
        {
            //Extract CustomerId using the PN EventChannel Delimeter
            indx = channelName.indexOf( EVENT_CHANNEL_DELIMETER_PN );
            customerID = channelName.substring( indx + EVENT_CHANNEL_DELIMETER_PN.length() );
        }

        if (indx == -1)
        {
            Debug.error("Unable to extract CustomerId from the EventChannel Name."+
              "EventChannelName should have '.EventChannel_' or '.PN_' to separate the customerId.");
        }
        return customerID;
    }

    /**
     * Remove the trailing _cid from the name.
     * @param name The string containing the cid.
     * @param cid The customer id that name might be ending in.
     *
     * @return String - the name without the trailing _cid.
     */
    public static String removeCIDTrailer( String name, String cid )
    {
        StringBuffer sb = new StringBuffer( name );

        if( name.endsWith(cid) )
            name = name.substring(0, name.length() -  ( cid.length() + 1 ) );
        return name;

    }

    /**
     * Inner helper class for containing a single event channel properties.
     */
    public static class EventChannelConfig
    {
        public EventChannelConfig()
        {
            props = new HashMap();
        }

        public void put ( String name, String value )
        {
            props.put( name, value );
        }

        public Map get ()
        {
            return props;
        }

        public String getChannelName ()
        {
            return (String)props.get(EventChannelImpl.CHANNEL_NAME_PROP);
        }

        public String getChannelCount()
        {
            return (String)props.get( EVENT_CHANNEL_COUNT_PROP );
        }

        public String getPersistenceFlag()
        {
            return (String)props.get(EventChannelImpl.PERSISTENCE_PROP);
        }

		public String getSoapResponseHandlerURL()
        {
            return (String)props.get(SOAP_RESPONSE_HANDLER_URL_PROP);
        }

        /**
         * Get a human-readable description of the channel.
         *
         * @return  A description of the channel.
         */
        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "EventChannel description: \n" );
            sb.append( props.toString() );
            sb.append( "\n" );
            
            return( sb.toString() );
        }

        private Map props = null;
    }

}//end of class EventChannelUtil.
