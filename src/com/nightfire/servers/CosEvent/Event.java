/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.servers.CosEvent;

import java.sql.*;
import java.text.*;


/**
 * An object containing a single event.
 */
public class Event
{
    // These data members map to columns in the PersistentEvent database table.
    public String channelName;
    public int id;
    public String message;
    public java.sql.Timestamp arrivalTime;
    public String errorStatus;
    public int errorCount;
    public String lastErrorMessage;
    public java.sql.Timestamp lastErrorTime;


    /**
     * Create an event object.
     */
    public Event ( )
    {

    }


    /**
     * Create an event object.
     *
     * @param  channelName  The name of the channel the event is passing through.
     * @param  event  The event value.
     */
    public Event ( String channelName, String event )
    {
        this.channelName = channelName;

        message = event;
    }


    /**
     * Describe the event in human-readable form.
     *
     * @return  A string describing the event.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "Event: " );
        sb.append( "Channel-name [" );
        sb.append( channelName );
        sb.append( "], id [" );
        sb.append( id );
        sb.append( "], message-length [" );
        if ( message == null )
            sb.append( "null" );
        else
            sb.append( message.length() );
        sb.append( "], arrival-time [" );
        sb.append( formatDate( arrivalTime ) );
        sb.append( "], error-status [" );
        sb.append( errorStatus );
        sb.append( "], error-count [" );
        sb.append( errorCount );
        sb.append( "], last-error-message [" );
        sb.append( lastErrorMessage );
        sb.append( "], last-error-time [" );
        sb.append( formatDate( lastErrorTime ) );
        sb.append( "]" );

        return( sb.toString() );
    }


    private String formatDate ( Timestamp value )
    {
        if ( dateTimeFormatter == null ) 
            dateTimeFormatter = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss.SSS" );

        if ( value == null )
            return null;
        else
            return( dateTimeFormatter.format( value ) );
    }


    private SimpleDateFormat dateTimeFormatter;
}
