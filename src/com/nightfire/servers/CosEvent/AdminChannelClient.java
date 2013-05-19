/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/main/com/nightfire/servers/CosEvent/ReliableChannel.java#11 $
 */

package com.nightfire.servers.CosEvent;

import java.util.*;
import java.text.*;

import org.omg.CORBA.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.message.generator.xml.*;


/**
 * Utility used to send administrative events to the Admin Event Channel
 * for resetting failed persistent events that should be re-processed.
 */
public class AdminChannelClient
{
    /**
     * Flag indicating command line argument for channel name.
     */
    public static final String CHANNEL_NAME_ARG_FLAG = "-c";
    
    /**
     * Flag indicating command line argument for retry date.
     */
    public static final String RETRY_DATE_ARG_FLAG = "-d";
    
    /**
     * Flag indicating command line argument for retry count
     */
    public static final String RETRY_COUNT_ARG_FLAG = "-r";

    /**
     * Flag indicating command line argument for unique event identifier.
     */
    public static final String EVENT_ID_ARG_FLAG = "-i";


    /**
     * Application execution entry point.
     *
     * @param  args  Command-line arguments.
     */
    public static void main ( String[] args )
    {
        System.out.println( "Executing admin channel client utility ..." );

        // Allow users to turn on low-level debugging via a -DDEBUG_LOG_LEVELS="#" 
        // command-line argument.
        Debug.showLevels( );
        Debug.configureFromProperties( );
        
        // At a minimum the name of the target channel to administer must be given.
        if ( args.length == 0 )
        {
            System.err.println( getUsage( args ) );
            
            System.exit( -1 );
        }
            
        try
        {
            // Construct the XML event from the command-line arguments.
            XMLMessageGenerator gen = new XMLMessageGenerator( "AdminChannelEvent" );
            

            // Get the channel-name.
            String value = getOption( args, CHANNEL_NAME_ARG_FLAG, true );
            
            gen.setValue( AdminChannelImpl.CHANNEL_NAME_NODE, value );
            

            // Get the retry-date.
            value = getOption( args, RETRY_DATE_ARG_FLAG, false );
            
            if ( StringUtils.hasValue( value ) )
            {
                // Check that date format is valid by attempting to parse it.
                try
                {
                    SimpleDateFormat dateTest = new SimpleDateFormat( AdminChannelImpl.DATE_FORMAT );
                    
                    dateTest.parse( value );
                }
                catch(Exception e)
                {
                    throw new Exception( "ERROR: Invalid date format for configuration item: " 
                                         + RETRY_DATE_ARG_FLAG + "\n" + e.toString() );
                }

                gen.setValue( AdminChannelImpl.DATE_FLOOR_NODE, value );
            }
            

            // Get the retry-count.
            value = getOption( args, RETRY_COUNT_ARG_FLAG, false );
            
            if ( StringUtils.hasValue( value ) )
                gen.setValue( AdminChannelImpl.RETRY_CEILING_NODE, value );
            

            // Get the event-id.
            value = getOption( args, EVENT_ID_ARG_FLAG, false );
            
            if ( StringUtils.hasValue( value ) )
                gen.setValue( AdminChannelImpl.EVENT_ID_NODE, value );


            String event = gen.generate( );
            
            System.out.println( "Administrative event is:\n" + event );
            
            
            // Construct push supplier and deliver event to Admin Channel.
            CorbaPortabilityLayer cpl = new CorbaPortabilityLayer( args, null, null );
			
            EventPushSupplier pushSupplier = new EventPushSupplier( cpl.getORB(), AdminChannelImpl.ADMIN_CHANNEL_NAME, false );
            
            try
            {
                System.out.println( "Pushing admin event to Event Service ..." );
                
                pushSupplier.pushEvent( event );
                
                System.out.println( "Successfully pushed admin event to Event Service." );
            }
			finally
			{
				if ( pushSupplier != null ) 
				{
                    try
                    {
                        pushSupplier.disconnect( );
                    }
                    catch ( Exception e )
                    {
                        System.err.println( e );
                    }
				}
			}
        }
        catch ( Exception e )
        {
            System.err.println( getUsage( args ) );
            
            System.err.println( e );
            
            e.printStackTrace( );
        }
    }


    /**
     * Extract a configuration value from the command-line arguments.
     * 
     * @param  args  Array of command-line arguments.
     * @param  name  Name of the command-line argument to get.
     * @param  required  Flag indicating whether the item is required or not.
     * 
     * @return  The value of the requested item, or null if not available.
     * 
     * @exception  Thrown if required item is missing or malformed.
     */
    private static String getOption ( String[] args, String name, boolean required ) throws Exception
    {
        String value = null;
        
        for ( int Ix = 0;  Ix < args.length;  Ix ++ )
        {
            if ( args[Ix].startsWith( name ) )
            {
                value = args[Ix].substring(name.length()).trim( );
                
                break;
            }
        }
        
        if ( required && !StringUtils.hasValue( value ) )
        {
            throw new Exception( "ERROR: Missing required command-line switch [" + name + "]." );
        }
        
        return value;
    }


    /**
     * Get the utility's command-line usage description.
     *
     * @param  args  Command-line arguments.
     */
    private static String getUsage( String[] args )
    {
        StringBuffer sb = new StringBuffer( );
        
        for ( int Ix = 0;  Ix < args.length;  Ix ++ )
        {
            sb.append( args[Ix] );
            sb.append( "\n" );
        }
        
        String usage = 
            "\n\n\nUtility used to administer Event Channels with failed persistent\n" +
            "events that should be reset for reprocessing by the Event Service.\n\n" +
            "USAGE: " +  AdminChannelClient.class.getName() + " " + CHANNEL_NAME_ARG_FLAG + "<channel-name> " 
            + RETRY_DATE_ARG_FLAG + "<date-floor> " + RETRY_COUNT_ARG_FLAG + "<retry-count-ceiling> " + EVENT_ID_ARG_FLAG + "<id>\n\n"
            + "WHERE:\n" + 
            "   " + CHANNEL_NAME_ARG_FLAG + " indicates <name-of-event-channel-to-administer> (required)\n" + 
            "   " + RETRY_DATE_ARG_FLAG + " indicates <date-of-oldest-event-to-consider> (optional)\n" + 
            "   " + RETRY_COUNT_ARG_FLAG + " indicates <largest-error-count-to-consider> (optional)\n" + 
            "   " + EVENT_ID_ARG_FLAG + " indicates <unique-event-identifier> (optional)\n\n" + 
            "NOTE: The date must be in \"" + AdminChannelImpl.DATE_FORMAT + "\" format (as accepted by java.text.SimpleDateFormat API) WHERE:\n" + 
            "   'M'=numeric month in year (Ex: 07), 'd'=day in month (Ex: 10), 'yy'=2-digit year (Ex: 99), and 'H'=hour in day (0~23)\n\n" + 
            "Current command-line arguments:\n" + sb.toString() + "\n\n\n";

        
        return usage;
    }
}
