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
 * Class for storing information about a database table. Currently this only stores
 * the column names for a table. This can be used for existence checks on columns.
 * Accessing this class through the DBMetaData API is recommended.
 */
public class TableMetaData
{

    /**
     * Constructor that accesses the table meta-data associated with the table tableName.
     *
     * @param dbConn The connection to the database that contains the table tableName.
     * @param tableName The tableName whose information is required.
     *
     * @exception DatabaseException on processing errors.
     */
    public TableMetaData ( Connection dbConn, String tableName ) throws DatabaseException
    {
        this.tableName = tableName;
        columnContainer = new ArrayList();
        columnMetaDataMap = new HashMap();
        try
        {
            DatabaseMetaData dmd = dbConn.getMetaData();
            ResultSet rs = dmd.getColumns( null, null, tableName, null );
            for ( int Ix = 0;  rs.next();  Ix ++ )
            {
                String schemaName = rs.getString("TABLE_SCHEM");
                String colName = rs.getString( "COLUMN_NAME" );
                int type = rs.getInt("DATA_TYPE");
                String typeName = rs.getString("TYPE_NAME");
                int size = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                String defaultValue = rs.getString("COLUMN_DEF");
                int ordPos = rs.getInt("ORDINAL_POSITION");
                String isnull = rs.getString("IS_NULLABLE");

                ColumnMetaData columnMetaData = new ColumnMetaData(schemaName,
                                                       colName,
                                                       type,typeName,size,
                                                       decimalDigits,
                                                       defaultValue,ordPos,isnull);

                if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                    Debug.log( Debug.DB_DATA, "Adding column[" + colName + "] to table["
                               + tableName + "] of type:[" + typeName + "]");

                add( colName );
                columnMetaDataMap.put(colName,columnMetaData);
            }
        }
        catch ( SQLException e )
        {
            throw new DatabaseException( "ERROR: Could not get table meta-data for [" + tableName
                                           + "]:\n" + DBInterface.getSQLErrorMessage(e) );
        }
    }

    /**
     * Add the columnName to the list of columns belonging to this table.
     * columnName is in upper-case as returned by the DatabaseMetaData.
     */
    private void add ( String columnName )
    {
        columnContainer.add( columnName );
    }

    /**
     * Whether UserId is a column in this table. This is a case-insensitive check.
     *
     * @return boolean true if UserId is a column in this table, false otherwise.
     */
    public boolean existsUserId ()
    {
        return exists( CustomerContext.USER_ID_COL_NAME );
    }


    /**
        * Whether InputSource is a column in this table. This is a case-insensitive check.
        *
        * @return boolean true if InputSource is a column in this table, false otherwise.
        */
    public boolean existsInputSource()
    {
        return exists(DBMetaDataConstants.INPUT_SOURCE_HEADER_PROP);

    }

    /**
        * Whether OrderOId is a column in this table. This is a case-insensitive check.
        *
        * @return boolean true if OrderOId is a column in this table, false otherwise.
        */
    public boolean existsOrderOId()
    {
        return exists(DBMetaDataConstants.ORDER_OID_HEADER_PROP);

    }

    /**
        * Whether TransOId is a column in this table. This is a case-insensitive check.
        *
        * @return boolean true if TransOId is a column in this table, false otherwise.
        */
    public boolean existsTransOId()
    {
        return exists(DBMetaDataConstants.TRANS_OID_HEADER_PROP);

    }

    /**
     * Whether InterfaceVersion is a column in this table. This is a case-insensitive check.
     *
     * @return boolean true if InterfaceVersion is a column in this table, false otherwise.
     */
    public boolean existsInterfaceVersion ()
    {
        return exists( CustomerContext.INTERFACE_VERSION_COL_NAME );
    }

    /**
     * Whether CustomerId is a column in this table. This is a case-insensitive check.
     *
     * @return boolean true if CustomerId is a column in this table, false otherwise.
     */
    public boolean existsCustomerId ()
    {
        return exists( CustomerContext.CUSTOMER_ID_COL_NAME );
    }

    /**
     * Whether the column colName is a column in this table. This is a case-insensitive check.
     *
     * @return boolean true if colName is a column in this table, false otherwise.
     */
    public boolean exists ( String colName )
    {
        return columnContainer.contains( colName.toUpperCase() );
    }

    /**
     * Returns a human-readable description of the table meta data.
     *
     * @return Table meta-data description in text form.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "Table meta-data: table [" );
        sb.append( tableName );
        sb.append( "], columns [" );
        sb.append( columnContainer.toString() );
        sb.append( "]" );

        return( sb.toString() );
    }

    /**
     * This method returns a map containing a column meta data information
     * for the column.  A map of column name to column meta data object.
     * @return Map
     */
    public Map getColumnMetaData()
    {
        return columnMetaDataMap;
    }


    //Columns in this table.
    private List columnContainer = null;

    private Map columnMetaDataMap = null;

    //Name of the table associated with this class.
    private String tableName = null;

}//end of class TableMetaData