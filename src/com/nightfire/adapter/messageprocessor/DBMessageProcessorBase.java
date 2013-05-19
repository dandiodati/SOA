/**
 * Copyright (c) 2003 Neustar, Inc. All rights reserved.
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/DBMessageProcessorBase.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.sql.*;
import java.text.*;
import java.io.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;


/**
 * Base class containing common database-related utilities for use in 
 * message-processors that access the database.
 */
public class DBMessageProcessorBase extends MessageProcessorBase
{
    /**
     * Property indicating whether an exception should be thrown when the SQL operation 
     * doesn't access any database rows (default is false for backwards-compatibility).
     */
    public static final String THROW_EXCEPTION_WHEN_NOT_FOUND_FLAG_PROP = "THROW_EXCEPTION_WHEN_NOT_FOUND_FLAG";

    // Token to be replaced by current customer id
    public static final String CID_TOKEN = "${CID}";

    // Token to be replaced by current DBMessageProcessorBase id
    public static final String SDID_TOKEN = "${SDID}";

    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        try
        {
            if ( propertyExists( THROW_EXCEPTION_WHEN_NOT_FOUND_FLAG_PROP ) )
            {
                throwExceptionWhenNotFoundFlag 
                    = StringUtils.getBoolean( getPropertyValue( THROW_EXCEPTION_WHEN_NOT_FOUND_FLAG_PROP ) );
            }
        }
        catch ( ProcessingException pe )
        {
            throw pe;
        }
        catch ( Exception e )
        {
            throw new ProcessingException( e.getMessage() );
        }
    }


    /**
     * Utility that checks outcome of SQL operation based on configuration.
     *
     * @param  resultCount  The number of database rows accessed as a result of the
     *                      SQL operation.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    protected void checkSQLOperationResultCount ( int resultCount ) throws ProcessingException, MessageException
    {
        if ( Debug.isLevelEnabled( Debug.DB_STATUS ) ) 
            Debug.log( Debug.DB_STATUS, "SQL operation accessed [" + resultCount + "] database row(s)." );

        // Do nothing if not configured to check result count.
        if ( throwExceptionWhenNotFoundFlag == false )
        {
            Debug.log( Debug.DB_STATUS, "Message-processor is configured to skip throwing exceptions when no database rows are accessed." );

            return;
        }
        else
        {
            if ( resultCount == 0 )
            {
                throw new ProcessingException( "SQL operation is required to access at least one database row." );
            }
        }
    }

    protected boolean throwExceptionWhenNotFoundFlag = false;
}
