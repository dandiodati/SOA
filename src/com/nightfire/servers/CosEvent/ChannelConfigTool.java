/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.servers.CosEvent;

import java.util.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;


public class ChannelConfigTool
{
    /**
     * Tool used to convert persistent property-based event channel configurations to
     * repository-based XML equivalents.
     *
     * @param  args  Command-line arguments:
     *               arg[0] = db-jdbc-url.
     *               arg[1] = db-user.
     *               arg[2] = db-password.
     *               arg[3] = Property key.
     *               arg[4] = Property type.
     *               arg[5] = Name of target file to write XML configuration to.
     */
    public static void main ( String[] args )
    {
        if ( args.length < 6 )
        {
            System.err.println( "\n\nUSAGE: ChannelConfigTool <db-jdbc-url> <db-user> <db-password> "
                                + "<property-key> <property-type> <target-file-name>.\n\n" );

            System.exit( -1 );
        }

        Debug.enableAll( );
        Debug.showLevels( );
        Debug.configureFromProperties( );

        // If thread logging is enabled, turn on thread-id logging in messages.
        if (Debug.isLevelEnabled(Debug.THREAD_BASE))
            Debug.enableThreadLogging();

        // Display stack trace when exceptions are created.
        if (Debug.isLevelEnabled( Debug.EXCEPTION_STACK_TRACE))
            FrameworkException.showStackTrace();

        String dbName          = args[0];
        String dbUser          = args[1];
        String dbPassword      = args[2];
        String propKey         = args[3];
        String propType        = args[4];
        String targetFileName  = args[5];

        try
        {
            // Initialize the database connections.
            Debug.log( Debug.UNIT_TEST, "Initializing database ..." );

            DBInterface.initialize( dbName, dbUser, dbPassword );

            // Generate the XML equivalent of the event channel associated with the
            // key and type identifying the application's configuration.
            String result = generateChannelConfig( propKey, propType );

            // Write out to the indicated file.
            FileUtils.writeFile( targetFileName, result );

            DBInterface.closeConnection( );
        }
        catch ( Exception e )
        {
            System.err.println( "\n\nERROR: " + e.toString() );

            e.printStackTrace( );
        }
    }


    /**
     * Generate the XML equivalent of the event channel configuration for the application
     * whose configuration is indicated by the given key and type values.
     *
     * @param  propKey  Property key identifying application's configuration.
     * @param  propType  Property type identifying application's configuration.
     *
     * @return  XML configuration of any found event channel configuration.
     *
     * @exception  Exception  Thrown on any errors.
     */
    private static String generateChannelConfig ( String propKey, String propType ) throws Exception
    {
        // Get all of the application's configuration.
        PropertyChainUtil pcu = new PropertyChainUtil( );

        Hashtable serverProperties = pcu.buildPropertyChains( propKey, propType );

        // Create the XML generator to write the channel configuration to.
        XMLMessageGenerator gen = new XMLMessageGenerator( "EventChannelConfiguration" );

        // Get the names of all of the event channel relevant configuration items.
        String[] propPrefixes = EventChannelImpl.getConfigPropertyNames( );

        // Get all of the iterations associated with named channels.
        NVPair[] channelNames = PersistentProperty.getPropertiesLike( serverProperties,
                                                                      EventChannelImpl.CHANNEL_NAME_PROP );

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

            String iterNamePrefix = EventChannelUtil.EVENT_CHANNEL_CONTAINER_NODE + ".EventChannel(" + Ix + ").";

            gen.setValue( iterNamePrefix + EventChannelImpl.CHANNEL_NAME_PROP,
                          (String)serverProperties.get( channelNameProp ) );

            // Extract all of the other configuration items for the specific channel configuration iteration.
            for ( int Jx = 0;  Jx < propPrefixes.length;  Jx ++ )
            {
                String propValue
                    = (String)serverProperties.get( PersistentProperty.getPropNameIteration( propPrefixes[Jx],
                                                                                             groupSuffix ) );

                // If available, write the configuration item out to the correct XML location.
                if ( StringUtils.hasValue( propValue ) )
                    gen.setValue( iterNamePrefix + propPrefixes[Jx], propValue );
            }
        }

        // Return resulting XML in string form.
        return( gen.generate() );
    }
}
