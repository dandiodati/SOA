package com.nightfire.order.pojo;

import java.sql.Connection;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import com.nightfire.framework.util.CustomerContext;
import com.nightfire.order.utils.CHOrderConstants;

import biz.neustar.nsplatform.db.pojo.POJOBase;
import biz.neustar.nsplatform.db.util.ColumnMetaData;
import biz.neustar.nsplatform.db.util.DBUtils;
import biz.neustar.nsplatform.db.util.DatabaseAware;
import biz.neustar.nsplatform.db.util.TableMetaData;

public class CHTxEventHistory extends POJOBase
{
    public Long oid;
    public Long parent_oid;
    public Long trans_parent_oid;
    public String event_code;
    public String input_source;
    public String created_by;
    public java.util.Date created_dt;
    public java.util.Date billed;
    public Long instance_id;
    public String xml_data;
    public Integer applybusinessrules;
    public String action	;

    public CHTxEventHistory () throws Exception
    {
        super( );
    }


    /**
     * Get the list of history records that match this one.
     *
     * @return  List of SeaTxEventHistory objects, sorted in
     *          ascending created-date order.
     *
     * @exception  Exception  Thrown on any errors.
     */
    public List queryByExample ( Connection dbConn ) throws Exception
    {
        return( getDBUtils().queryByExample( dbConn, this, getTableMetaData().getNonCLOBColumns(), 
                null, "ORDER BY created_dt ASC" ) );
    }


    /**
     * Describe the history for the indicated order.
     *
     * @return  History in string form.
     */
    public static String describeHistory ( List history )
    {
        StringBuffer sb = new StringBuffer( );
        sb.append( "CH ORDER HISTORY:\n" );

        if(history==null || history.isEmpty())
            sb.append("NONE");
        else
        {
            Iterator iter = history.iterator();
            for(int Ix = 0; iter.hasNext(); Ix++)
            {
                if(Ix>0)
                    sb.append("\n");

                sb.append("["+Ix+"] " + ((CHTxEventHistory)iter.next()).toString());
            }
        }

        return sb.toString() ;
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
        CREATE TABLE CH_TXEVENT_HISTORY(
        OID                 NUMBER(38)    NOT NULL,
        PARENT_OID          NUMBER(38)    NOT NULL,
        TRANS_PARENT_OID    NUMBER(38)    NOT NULL,
        EVENT_CODE          VARCHAR2(50)     NOT NULL,
        INPUT_SOURCE        VARCHAR2(3),
        CREATED_BY          VARCHAR2(32)     NOT NULL,
        CREATED_DT          TIMESTAMP(0)     NOT NULL,
        BILLED              TIMESTAMP(0),
        INSTANCE_ID         NUMBER(28),
        XML_DATA            CLOB,   
       	ACTION	VARCHAR2(50),	
 	    APPLYBUSINESSRULES	NUMBER(1),
        CONSTRAINT CH_TXEVENT_HISTORY_PK PRIMARY KEY (OID)
	);	

         */
        if(tmd == null)
        {
            synchronized(CHTxEventHistory.class)
            {
                if(tmd != null)
                    return tmd;

                tmd = new TableMetaData( "CH_TXEVENT_HISTORY", CHTxEventHistory.class ); 

                tmd.addColumn( new ColumnMetaData("OID", Types.NUMERIC)).setPrimaryKeyIndex(0);
                tmd.addColumn( new ColumnMetaData("PARENT_OID", Types.NUMERIC));
                tmd.addColumn( new ColumnMetaData("TRANS_PARENT_OID", Types.NUMERIC));
                tmd.addColumn( new ColumnMetaData("EVENT_CODE"));
                tmd.addColumn( new ColumnMetaData("INPUT_SOURCE"));
                tmd.addColumn( new ColumnMetaData("CREATED_BY"));
                tmd.addColumn( new ColumnMetaData("CREATED_DT",Types.TIMESTAMP));
                tmd.addColumn( new ColumnMetaData("BILLED", Types.TIMESTAMP));
                tmd.addColumn( new ColumnMetaData("INSTANCE_ID",Types.NUMERIC));
                tmd.addColumn( new ColumnMetaData("XML_DATA",Types.CLOB));
                tmd.addColumn( new ColumnMetaData("APPLYBUSINESSRULES",Types.NUMERIC));
                tmd.addColumn( new ColumnMetaData("ACTION"));
            }
        }

        return tmd;
    }

    /**
     * Perform any processing required before a record can be inserted
     * into the database.  Typically, this involves setting up things
     * like OIDs using SEQUENCE values.
     *
     * @exception  Exception  Thrown on any errors.
     */
    public ColumnMetaData[] beforeInsert(ColumnMetaData[] insertCols) throws Exception
    {
        oid = new Long(DBUtils.getInstance().getSequenceValue(CH_TXEVENTHISTORY_SEQ));
        insertCols = addColumn( insertCols, "oid" );

        created_by  = CustomerContext.getInstance().getUserID();  
        insertCols = addColumn(insertCols,CREATED_BY_COL);

        created_dt = new java.util.Date();
        insertCols = addColumn(insertCols,CREATED_DT_COL);

        return insertCols;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("OID : "+oid);
        sb.append("\nPARENT_OID : "+parent_oid);
        sb.append("\nTRANS_PARENT_OID : "+trans_parent_oid);
        sb.append("\nXML_DATA : "+xml_data);

        return sb.toString();
    }

    @Override
    public void copy(DatabaseAware source) throws Exception {

        TimeZone tz = ((POJOBase)this).getPreferredTimeZone();
        super.copy(source);
        if(tz!=null)
            this.useTimeZone(tz);
            
    }

    private static String CREATED_BY_COL = "CREATED_BY";
    private static String CREATED_DT_COL = "CREATED_DT";    
    private static String CH_TXEVENTHISTORY_SEQ = "CH_TXEVENTHISTORY_SEQ";
    // Should be static, as we only want one instance per POJO type.
    private static TableMetaData tmd = null;
}
