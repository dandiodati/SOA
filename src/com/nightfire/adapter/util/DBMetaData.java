/**
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 * $Header: $
 */

package com.nightfire.adapter.util;


import java.util.*;
import java.sql.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;


/**
 * This singleton class fetches and stores information about columns in any table
 * that can be accessed using the Connection object passed in.
 */
public class DBMetaData
{
    /**
     * Do not instantiate.
     */
    private DBMetaData ()
    {
    }

    /**
     * Return an instance of DBMetaData to perform operations.
     *
     * @return DBMetaData instance used for performing operations.
     */
    public static DBMetaData getInstance()
    {
        return instance;
    }

    /**
     * Returns the TableMetaData for the tableName accessible through dbConn.
     *
     * @param dbConn The connection to the database that contains the table tableName.
     * @param tableName The tableName whose information is required.
     *
     * @return TableMetaData instance associated with tableName.
     * @exception DatabaseException on processing errors.
     */
    public TableMetaData getTableMetaData( Connection dbConn, String tableName ) throws DatabaseException
    {
        //Accommodating applications passing in the tableName in any case.
        tableName = tableName.toUpperCase();
        if ( tableContainer.get( tableName ) == null )
        {
            synchronized( tableContainer )
            {
                if ( tableContainer.get( tableName ) == null )
                {
                    tableContainer.put( tableName, new TableMetaData( dbConn, tableName ) );
                }
            }
        }

        return (TableMetaData)tableContainer.get( tableName );
    }

    //Singleton instance.
    private static DBMetaData instance = new DBMetaData();

    //Container for tables(and their columns) accessed so far.
    private static Map tableContainer = new HashMap();
}//end of class DBMetaData
