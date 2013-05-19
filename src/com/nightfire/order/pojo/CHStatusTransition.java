package com.nightfire.order.pojo;


import biz.neustar.nsplatform.db.util.*;
import biz.neustar.nsplatform.db.pojo.*;


public class CHStatusTransition extends POJOBase
{
    public String status_level;
    public String product;
    public String cust_name;
    public String supplier_name;
    public String old_status;
    public String event_code;
    public String new_status;


    public CHStatusTransition ( ) throws Exception
    {
        super( );
    }


    /**
     * Get the meta-data describing the POJO/database table mapping.
     *
     * @return  Table meta-data object describing the mapping.
     *
     * @exception  Exception  Thrown on any errors.
     */
    public TableMetaData getTableMetaData ( ) throws Exception
    {
        /*
        CREATE TABLE CH_STATUS_TRANSITION(
            PRODUCT          VARCHAR2(16)    NOT NULL,
            CUST_NAME        VARCHAR2(32)    NOT NULL,
            SUPPLIER_NAME    VARCHAR2(32)    NOT NULL,
            OLD_STATUS       VARCHAR2(30)    NOT NULL,
            EVENT_CODE       VARCHAR2(50)    NOT NULL,
            NEW_STATUS       VARCHAR2(30)    NOT NULL,
        CONSTRAINT ICP_STATUS_TRANSITION_PK PRIMARY KEY (PRODUCT, CUST_NAME, SUPPLIER_NAME, OLD_STATUS, EVENT_CODE)        
         */
        if ( tmd == null )
        {
            synchronized( CHStatusTransition.class )
            {
                if ( tmd != null )
                    return tmd;

                tmd = new TableMetaData("CH_STATUS_TRANSITION", CHStatusTransition.class ); 

                tmd.addColumn( new ColumnMetaData( "STATUS_LEVEL") ).setPrimaryKeyIndex(0);
                tmd.addColumn( new ColumnMetaData( "PRODUCT") ).setPrimaryKeyIndex(1);
                tmd.addColumn( new ColumnMetaData( "CUST_NAME") ).setPrimaryKeyIndex(2);
                tmd.addColumn( new ColumnMetaData( "SUPPLIER_NAME") ).setPrimaryKeyIndex(3);
                tmd.addColumn( new ColumnMetaData( "OLD_STATUS") ).setPrimaryKeyIndex(4);
                tmd.addColumn( new ColumnMetaData( "EVENT_CODE") ).setPrimaryKeyIndex(5);
                tmd.addColumn( new ColumnMetaData( "NEW_STATUS") );
            }
        }

        return tmd;
    }


    // Should be static, as we only want one instance per POJO type.
    private static TableMetaData tmd = null;
}
