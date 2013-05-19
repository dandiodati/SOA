package com.nightfire.order.pojo;

import java.sql.Types;

import biz.neustar.nsplatform.db.pojo.POJOBase;
import biz.neustar.nsplatform.db.util.ColumnMetaData;
import biz.neustar.nsplatform.db.util.TableMetaData;

public class CHOrderStatus extends POJOBase
{
    public String product;
    public String status;
    public String status_name;
    public Long sort_order;


    public CHOrderStatus() throws Exception
    {
        super();
    }


    /**
     * Get the meta-data describing the POJO/database table mapping.
     *
     * @return  Table meta-data object describing the mapping.
     *
     * @exception  Exception  Thrown on any errors.
     */
    public TableMetaData getTableMetaData() throws Exception
    {
        /*
          CREATE TABLE CH_ORDER_STATUS(
          PRODUCT        VARCHAR2(16)    NOT NULL,
          STATUS         VARCHAR2(20)    NOT NULL,
          STATUS_NAME    VARCHAR2(24)    NOT NULL,
          SORT_ORDER     NUMBER(2, 0),
          CONSTRAINT CH_ORDER_STATUS_PK PRIMARY KEY (STATUS)
          );
         */
        if(tmd == null)
        {
            synchronized(CHOrderStatus.class)
            {
                if ( tmd != null )
                    return tmd;

                tmd = new TableMetaData("CH_ORDER_STATUS", CHOrderStatus.class ); 

                tmd.addColumn( new ColumnMetaData("STATUS") ).setPrimaryKeyIndex(0);
                tmd.addColumn( new ColumnMetaData("PRODUCT") );
                tmd.addColumn( new ColumnMetaData( "STATUS_NAME") );
                tmd.addColumn( new ColumnMetaData("SORT_ORDER", Types.NUMERIC) );
            }
        }

        return tmd;
    }


    // Should be static, as we only want one instance per POJO type.
    private static TableMetaData tmd = null;
}
