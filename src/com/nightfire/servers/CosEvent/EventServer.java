/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/CosEvent/EventServer.java#1 $
 */

package com.nightfire.servers.CosEvent;

import java.util.*;

import org.omg.CORBA.*;
import org.w3c.dom.*;
import org.omg.CosEventComm.*;
import org.omg.CosEventChannelAdmin.*;

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
 * The event service manages a set of event channels according to its configuration.
 * It implements the ServerAdmin interface so that it can be managed by the console.
 */
public class EventServer extends ServerBase implements ServerAdminOperations
{
    /**
     * Iterated property naming event channel repository configuration (Optional - if not
     * given and repository configuration exists, all such configuration will be used).
     */
    public static final String REPOSITORY_EVENT_CHANNEL_CONFIG_PROP = "REPOSITORY_EVENT_CHANNEL_CONFIG";

    /**
     * Repository category giving event channel configurations.
     */
    public static final String EVENT_CHANNEL_CONFIG = "eventChannel";

    /**
     * Entry point for CORBA COS Event Service application execution.
     *
     * @param  args  Command-line arguments.
     */
    public static void main ( String[] args )
    {
        Debug.log( Debug.NORMAL_STATUS, "Creating Event Service ..." );
        
        try 
        {
            EventServer server = new EventServer( args );
            
            Debug.log( Debug.NORMAL_STATUS, LINE + "\nEvent Service is now configured." + LINE );
            
            server.waitForRequests( );
        }
        catch ( Exception e )
        {
            Debug.error( "Event Service exiting due to the following error:\n" 
                         + e.toString() );
            
            Debug.error( Debug.getStackTrace( e ) );
        }
    }
    

    /**
     * Implementation for command-line syntax checking for event service.
     *
     * @param  args  Array of strings passed in to main() as command-line arguments.
     *
     * @exception  Exception  Thrown if command-line arguments are invalid.
     */
    protected void checkCommandLineSyntax ( String[] args ) throws Exception
    {
        if ( args.length != 5 ) 
        {
            throw new Exception( "\n\nERROR: Command-line arguments are incorrect!\n\n"
                                 + "Usage: java EventServer <DBNAME> <DBUSER> <DBPASSWORD> <KEY> <TYPE>\n"
                                 + "    DBNAME:     Name of database\n"
                                 + "    DBUSER:     Name of database account user\n"
                                 + "    DBPASSWORD: The password of database account user\n"
                                 + "    KEY       : Key for event service configuration properties\n"
                                 + "    TYPE      : Type for event service configuration properties\n"
                                 + "Ex: java EventServer jdbc:oracle:thin:@127.0.0.1:1521:ORCL scott tiger EVENT_SERVER EVENT_SERVER\n\n"
                                 );
        }
    }


    /**
     * Constructs the event service.
     *
     * @param  args  Array of strings containing command-line arguments.
     *
     * @exception  Exception  Thrown on errors.
     */
    protected EventServer ( String[] args ) throws Exception
    {
        super( args );
    }


    /**
     * Parses the command line and initializes the event service.
     *
     * @param  args  Array of strings containing command-line arguments.
     *
     * @exception Exception  Thrown if initialization fails.
     */
    protected void initializeServer ( String args[] ) throws Exception 
    {
        // Remember the config property key and type values for use in cache-flushing.
        configKey  = args[3];
        configType = args[4];

        try
        {
            // Get the name from Properties 
            adminName = getProperty( NAME_TAG );
            
            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Event Service admin name is [" + adminName + "]." );
            
            // Create the admin server CORBA object.
            adminServer =  new ServerAdminPOATie( this );
            
            corbaObjectIsReady( adminName, adminServer );
            
            // Exporting to naming service.
            Debug.log( Debug.OBJECT_LIFECYCLE, "Adding Event Service admin object to COS Naming Service ..." );
                
            exportToNS( adminName, adminServer );

            // Create the event channels.
            createEventChannels( );

            // Register for cache flushing.
            CacheManager.getRegistrar().register( new CacheFlushAdapter( this ) );

            RemoteOperationsAdminImpl.initialize( serverProperties );
        }
        catch ( Exception e )
        {
            Debug.error( "Event Service initialization failed:\n" + e.toString() );
            
            Debug.error( Debug.getStackTrace( e ) );
            
            throw e;
        }
    }


    /**
     * Shuts-down the event service.
     */
    public void shutdown ( ) 
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, LINE + "\nShutting-down Event Service..." + LINE );

        RemoteOperationsAdminImpl.shutdown( );

        // Destroy the event channels.
        destroyEventChannels( );

        // Shut down the admin object.
        if ( StringUtils.hasValue( adminName ) ) 
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Removing Event Service admin name from COS Naming Service ..." );
            
            try 
            {
                getObjectLocator().remove( adminName );
            }
            catch ( Exception e ) 
            {
                Debug.warning( "Failed to remove the Event Service name from COS Naming Service:\n" 
                               + e.toString() );

                Debug.warning( Debug.getStackTrace( e ) );
            }
        }
        
        Debug.log( Debug.OBJECT_LIFECYCLE, "Deactivating Event Service admin object ..." );
        
        try 
        {
            corbaDeactivateObj( adminServer );
        }
        catch ( Exception e ) 
        {
            Debug.warning( "Failed to deactivate Event Service:\n" 
                           + e.toString() );
            
            Debug.warning( Debug.getStackTrace( e ) );
        }

        try 
        {
            // Clean up DB connection, ORB,  and other resources.
            super.shutdown( );
        }
        catch ( Exception e ) 
        {
            Debug.warning( "An error occurred during Event Service shutdown:\n" + e.toString() );

            Debug.warning( Debug.getStackTrace( e ) );
        }
    }


    /**
     * Creates all of the event channels.
     *
     * @exception  Exception  Thrown if initialization fails.
     */
    private void createEventChannels ( ) throws Exception 
    {
        eventChannels = new LinkedList( );

        // Remember channels already created so that we don't create one with the same name twice.
        createdChannels = new HashSet( );

        // First, create the administration channel.
        AdminChannelImpl adminChannel = new AdminChannelImpl( getORB(), new HashMap() );
        
        eventChannels.add( adminChannel );
        
        // Now, create the configured channels.
        createConfiguredEventChannels( true );
    }

    
    /**
     * Creates all of the configured event channels.
     *
     * @param  firstTime  Flag indicating whether the next call of this method is the
     *                    first time that it has been called (during initialization) or
     *                    a subsequent call (during cache-flushing).
     *
     * @exception  Exception  Thrown if initialization fails.
     */
    private void createConfiguredEventChannels ( boolean firstTime ) throws Exception 
    {
        // Attempt to configure from repository first - before properties.
        createRepositoryConfiguredEventChannels( firstTime );

        Debug.log( Debug.SYSTEM_CONFIG, "Configuring event channels from the PersistentProperty database table ..." );

        // Get the list of iterated property name prefix that event channels
        // might need to initialize themselves.
        String[] propPrefixes = EventChannelImpl.getConfigPropertyNames( );

        // Get all of the configured channel names.
        NVPair[] channelNames = PersistentProperty.getPropertiesLike( serverProperties, EventChannelImpl.CHANNEL_NAME_PROP );

        int newChannelCounter = 0;

        // Loop until all iterated properties describing channels have been accessed.
        for ( int Ix = 0;  Ix < channelNames.length;  Ix ++ )
        {
            String channelNameProp = channelNames[Ix].name;
            
            // Extract the numeric suffix value to get the associated channel configuration properties.
            int suffixLoc = channelNameProp.lastIndexOf( '_' );

            if ( suffixLoc == -1 )
            {
                throw new FrameworkException( "Event channel name [" + channelNameProp 
                                              + "] is missing required '_#' grouping suffix." );
            }
            
            int groupSuffix = StringUtils.getInteger( channelNameProp.substring( suffixLoc + 1 ) );

            String channelName = getProperty( channelNameProp );

            // Don't continue with channel creation if the named channel has already been created.
            if ( createdChannels.contains( channelName ) )
            {
                String message = "Event Channel named [" + channelName + "] given by configuration property [" 
                    + channelNameProp + "] has already been created, so skipping new channel creation.";

                if ( firstTime )
                    Debug.warning( message );
                else
                    Debug.log( Debug.SYSTEM_CONFIG, message );

                continue;
            }

            // Create a containner for the additional properties, and load
            // any that are found into it.
            Map configProps = new HashMap( );

            for ( int Jx = 0;  Jx < propPrefixes.length;  Jx ++ )
            {
                String propValue = getProperty( propPrefixes[Jx], groupSuffix );

                if ( StringUtils.hasValue( propValue ) )
                    configProps.put( propPrefixes[Jx], propValue );
            }

            // Create the event channel and add it to the container.
            EventChannelImpl channel = new EventChannelImpl( getORB(), channelName, configProps );
            
            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "Event Service: Successfully created event channel [" 
                           + channelName + "]:\n" + channel.describe() );
            
            eventChannels.add( channel );

            // Remember that we've created a channel with this name.
            createdChannels.add( channelName );

            newChannelCounter ++;
        }

        if ( firstTime )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event Service: Created [" + channelNames.length 
                       + "] event channels. Names of all event channels:\n" + createdChannels.toString() );
        }
        else
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event Service: Created [" 
                       + newChannelCounter + "] new event channels." );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "Done configuring event channels from the PersistentProperty database table." );
    }


    /**
     * Creates all of the repository configured event channels.
     *
     * @param  firstTime  Flag indicating whether the next call of this method is the
     *                    first time that it has been called (during initialization) or
     *                    a subsequent call (during cache-flushing).
     *
     * @exception  Exception  Thrown if initialization fails.
     */
    private void createRepositoryConfiguredEventChannels ( boolean firstTime ) throws Exception 
    {
        Debug.log( Debug.SYSTEM_CONFIG, "Configuring event channels from the the repository ..." );

        String[] repositoryChannelConfigNames = null;

        EventChannelUtil.EventChannelConfig[] channelConfigs = null;
        try
        {
            channelConfigs = EventChannelUtil.getRepositoryConfiguredEventChannels(
                                              serverProperties,
                                              REPOSITORY_EVENT_CHANNEL_CONFIG_PROP,
                                              EVENT_CHANNEL_CONFIG );
        }
        catch ( Exception e )
        {
            throw new FrameworkException( "Could not get repository configuration for event channels: " + e );
        }

        int newChannelCounter = 0;
                
        // Loop until all iterated properties describing channels have been accessed.
        for ( int Ix = 0;  (channelConfigs!=null) && (Ix < channelConfigs.length);  Ix ++ )
        {

            EventChannelUtil.EventChannelConfig channelConfig = channelConfigs[Ix];
            String channelName = channelConfig.getChannelName();

            // Don't continue with channel creation if the named channel has already been created.
            if ( createdChannels.contains( channelName ) )
            {
                String message = "Event Channel named [" + channelName + "] given by configuration property ["
                    + channelName + "] has already been created, so skipping new channel creation.";

                if ( firstTime )
                    Debug.warning( message );
                else
                    Debug.log( Debug.SYSTEM_CONFIG, message );

                continue;
            }

            // Create the event channel and add it to the container.
            EventChannelImpl channel = new EventChannelImpl( getORB(), channelName, channelConfig.get() );

            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "Event Service: Successfully created event channel ["
                           + channelName + "]:\n" + channel.describe() );

            eventChannels.add( channel );

            // Remember that we've created a channel with this name.
            createdChannels.add( channelName );

            newChannelCounter ++;
        }

        if ( firstTime )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event Service: Created [" +  createdChannels.size()
                       + "] event channels with names:\n" + createdChannels.toString() );
        }
        else
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Event Service: Created ["
                       + newChannelCounter + "] new event channels." );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "Done configuring event channels from the the repository." );
    }


    /**
     * Destroy all of the event channels.
     */
    private void destroyEventChannels ( ) 
    {
        try
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Destroying event channels ..." );
            
            if ( eventChannels != null )
            {
                Iterator iter = eventChannels.iterator( );
                
                while ( iter.hasNext() ) 
                {
                    try
                    {
                        EventChannelImpl channel = (EventChannelImpl)iter.next( );
                        
                        channel.destroy( );
                    }
                    catch ( Exception e )
                    {
                        Debug.warning( "Could not destroy event channel:\n" + e.toString() );
                        
                        Debug.warning( Debug.getStackTrace( e ) );
                    }
                }
            }

            Debug.log( Debug.OBJECT_LIFECYCLE, "Event channels destroyed." );
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );
            
            Debug.warning( Debug.getStackTrace( e ) );
        }
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
        return( getProperty( PersistentProperty.getPropNameIteration( name, counter ) ) );
    }

    
    /**
     * Utility class to link cache-flushing to event server.
     */
    private class CacheFlushAdapter implements CachingObject
    {
        /**
         * Create a cache-flushing adapter for the given event server.
         *
         * @param  es  Event server to call back when flush is received.
         */
        public CacheFlushAdapter( EventServer es )
        {
            eventServer = es;
        }

        /**
         * Method invoked by the cache-flushing infrastructure
         * to indicate that the cache should be emptied.
         *
         * @exception FrameworkException if cache cannot be cleared.
         */
        public void flushCache ( ) throws FrameworkException
        {
            Debug.log( Debug.SYSTEM_CONFIG, "Flushing configuration cache to pick up any new event channels that should be created ..." );

            try
            {
                // Refresh the in-memory persistent properties to pick up any changes.
                PersistentProperty.flushCache( );

                PropertyChainUtil pcu = new PropertyChainUtil( );

                serverProperties = pcu.buildPropertyChains( configKey, configType );

                eventServer.createConfiguredEventChannels( false );
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() + "\n" + Debug.getStackTrace( e ) );

                throw new FrameworkException( e );
            }

            Debug.log( Debug.SYSTEM_CONFIG, "Done flushing configuration cache to pick up any new event channels that should be created." );
        }

        private EventServer eventServer;
    }


    private String configKey;
    private String configType;

    private List eventChannels;

    // Remember channels already created so that we don't create one with the same name twice.
    private Set createdChannels;

    // ServerAdmin name used to publish to the ORB and Naming Service.
    private String adminName;

    // COS Naming Service Locator
    private ObjectLocator ol;

    private ServerAdminPOA adminServer;

    private static final String LINE 
        = "\n###############################################################################";
}
