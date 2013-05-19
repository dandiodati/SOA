package com.nightfire.order.pojo;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.sql.Types;
import java.sql.Connection;
import java.lang.Long;

import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;

import biz.neustar.nsplatform.db.util.ColumnMetaData;
import biz.neustar.nsplatform.db.util.DBSessionManager;
import biz.neustar.nsplatform.db.util.DBUtils;
import biz.neustar.nsplatform.db.util.DatabaseAware;
import biz.neustar.nsplatform.db.util.LockAware;
import biz.neustar.nsplatform.db.util.TableMetaData;
import biz.neustar.nsplatform.db.pojo.POJOBase;

/**
 * 	Pojo class to insert/update/query CH_ORDER table.
 */
public class CHOrderPojo extends POJOBase implements LockAware
{
    public Long oid;
    public Long last_trans_oid;
    public String status;
    public String created_by;
    public Date created_dt = null;
    public String updated_by = null;
    public Date updated_dt = null;
    public String locked = DBUtils.LOCKED_NO;
    public String locked_user;
    public Date locked_ts;
    public Date completed;
    public String cust_name;
    public String supplier_name;
    public String order_type;
    public String order_type_version;
    public String product;


    public CHOrderPojo() throws Exception
    {
        super();
    }

    /**
     * 
     */
    public TableMetaData getTableMetaData() throws Exception
    {
        /*
	      CREATE TABLE CH_ORDER
		  (
			OID	NUMBER(38)	NOT NULL,
			LAST_TRANS_OID	NUMBER(38),	
			STATUS    	VARCHAR2(30)	NOT NULL,
			CREATED_BY    	NUMBER(28,0)	NOT NULL,
			CREATED_DT    	TIMESTAMP	NOT NULL,
			UPDATED_BY    	VARCHAR2(30),	
			UPDATED_DT    	TIMESTAMP(0),     	
			COMPLETED    	TIMESTAMP(0),     	
			LOCKED		CHAR(1)		DEFAULT 'N' NOT NULL, 	
			LOCKED_USER    	VARCHAR2(10),	
			LOCKED_TS    	TIMESTAMP(0),	
			CUST_NAME    	VARCHAR2(32)	NOT NULL,
			SUPPLIER_NAME	VARCHAR2(32)	NOT NULL,
			ORDER_TYPE	VARCHAR2(32)	NOT NULL,
			ORDER_TYPE_VERSION	VARCHAR2(10)	NOT NULL,
			PRODUCT	        VARCHAR2(16)	NOT NULL,
			CONSTRAINT CH_ORDER_PK PRIMARY KEY (OID)
	   );
         */

        if (tmd == null)
        {
            synchronized(CHOrderPojo.class)
            {
                if(tmd != null)
                    return tmd;

                tmd = new TableMetaData("CH_ORDER", CHOrderPojo.class ); 

                tmd.addColumn(new ColumnMetaData("OID", Types.NUMERIC)).setPrimaryKeyIndex(0);
                tmd.addColumn(new ColumnMetaData("LAST_TRANS_OID", Types.NUMERIC));
                tmd.addColumn(new ColumnMetaData("STATUS"));
                tmd.addColumn(new ColumnMetaData("CREATED_BY"));
                tmd.addColumn((new ColumnMetaData("CREATED_DT", Types.TIMESTAMP)));
                tmd.addColumn(new ColumnMetaData("UPDATED_BY"));
                tmd.addColumn(new ColumnMetaData("UPDATED_DT", Types.TIMESTAMP));
                tmd.addColumn(new ColumnMetaData("LOCKED", Types.CHAR));
                tmd.addColumn(new ColumnMetaData("LOCKED_USER"));
                tmd.addColumn(new ColumnMetaData("LOCKED_TS", Types.TIMESTAMP));
                tmd.addColumn(new ColumnMetaData("COMPLETED", Types.TIMESTAMP));
                tmd.addColumn(new ColumnMetaData("CUST_NAME"));
                tmd.addColumn(new ColumnMetaData("SUPPLIER_NAME"));
                tmd.addColumn(new ColumnMetaData("ORDER_TYPE"));
                tmd.addColumn(new ColumnMetaData("ORDER_TYPE_VERSION"));
                tmd.addColumn(new ColumnMetaData("PRODUCT"));
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

        oid = new Long(DBUtils.getInstance().getSequenceValue(CH_ORDER_SEQ));
        insertCols = addColumn( insertCols, "oid" );

        created_by  = CustomerContext.getInstance().getUserID();  
        insertCols = addColumn(insertCols,CREATED_BY_COL);

        created_dt = new java.util.Date();
        insertCols = addColumn(insertCols,CREATED_DT_COL);

        return insertCols;
    }


    public ColumnMetaData[] beforeUpdate(ColumnMetaData[] updateCols) throws Exception
    {
        // get the user from context
        updated_by  = CustomerContext.getInstance().getUserID();  
        updateCols = addColumn(updateCols,UPDATED_BY_COL);

        updated_dt = new java.util.Date();
        updateCols = addColumn(updateCols,UPDATED_DT_COL);

        return updateCols;
    }

    /**
     * 
     */
    public ColumnMetaData getLockedFlagColumn() throws Exception
    {
        return getTableMetaData().getColumn(LOCKED_COL);
    }

    /**
     * 
     */
    public ColumnMetaData getLockUserColumn() throws Exception
    {
        return getTableMetaData().getColumn(LOCKED_USER_COL);
    }

    public ColumnMetaData getLockTimestampColumn() throws Exception
    {
        return getTableMetaData().getColumn(LOCKED_TS_COL);
    }

    public void queryLatest(Connection dbConn) throws Exception
    {
        LinkedList list = new LinkedList(queryByExample(dbConn,"order by updated_dt desc, created_dt desc"));
        if(list == null)
        {
            throw new Exception("No rows returned by query latest");
        }
        else
        {
            copy((POJOBase)list.get(0));
        }

    }

    private static String CH_ORDER_SEQ = "CH_ORDER_SEQ";
    private static String UPDATED_BY_COL = "UPDATED_BY";
    private static String UPDATED_DT_COL = "UPDATED_DT";
    private static String CREATED_BY_COL = "CREATED_BY";
    private static String CREATED_DT_COL = "CREATED_DT";
    private static String LOCKED_COL = "LOCKED";
    private static String LOCKED_USER_COL = "LOCKED_USER";
    private static String LOCKED_TS_COL = "LOCKED_TS";

    // Should be static, as we only want one instance per POJO type.
    private static TableMetaData tmd = null;


    /* (non-Javadoc)
     * @see biz.neustar.nsplatform.db.pojo.POJOBase#update(java.sql.Connection)
     */
    public void update(Connection dbConn) throws Exception
    {
        // if validates successfully only then update the CH_ORDER Table.
        if(validate(dbConn))
            super.update(dbConn);

        // else do nothing.
    }

    /**
     * validates the new order type version of the order should always be greater
     * or equal to the old order type version.
     * @param dbConn
     * @return true if validates successfully otherwise return false.
     * @throws Exception
     */
    private boolean validate(Connection dbConn) throws Exception
    {
        // populate the pojo with the records as in CH_ORDER table.
        CHOrderPojo oldPojo = new CHOrderPojo();
        oldPojo.oid = this.oid;
        oldPojo.select(dbConn);

        // fetch the order type version for old record.
        int oldVersion = Integer.parseInt(oldPojo.order_type_version);

        // new order type version is same as one in this pojo.
        int newVersion = Integer.parseInt(this.order_type_version);

        // if oldVersion is greater than the new version set validate false.
        // remember to sync this pojo to old copy.
        if( oldVersion > newVersion)
        {
            this.copy(oldPojo);
            return false;
        }
        return true;
    }
    
    @Override
    public void copy(DatabaseAware source) throws Exception {

        TimeZone tz = ((POJOBase)this).getPreferredTimeZone();
        super.copy(source);
        if(tz!=null)
            this.useTimeZone(tz);
            
    }
    
    /**
     * Action variable that points to current action this is introduced to support save action.  
     */
    public String action;
    
    /**
     * Insert a new record if record record already exists in the CH_Order table, otherwise 
     * updates the existing record.
     * To decide the existence oid column is used.
     * 
     * @param dbConn
     * @throws Exception
     */
    public void upsert(Connection dbConn) throws Exception
    {
        if(oid == null)
        {   
            insert(dbConn);
        }
        else
        {
            update(dbConn);
        }
    }
    

    public void populate (Connection dbConn)
    {
        POJOBase tempPojo ;
        
        try
        {
            tempPojo = super.select(dbConn);
            this.copy(tempPojo);
        }
        catch (Exception e)
        {
            
        }
    }
    
    /**
     * Lock this Order POJO with the supplied database connection.
     * Before calling the super class method; user name is fetched 
     * from CustomerContext.
     * @Override 
     * @param Connection 
     * @throws Exception
     */
    public POJOBase lock(Connection dbConn) throws Exception
    {
        locked_user = CustomerContext.getInstance().getUserID();
        return super.lock(dbConn);
    }
    
    /**
     * Main method to unit test  
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception
    {
        
        DBSessionManager.initialize("jdbc:oracle:thin:@impetus-132:1521:ORCL132","hpirosha","hpirosha",false);
        CHOrderPojo pojo = new CHOrderPojo();
        pojo.oid = null;
        pojo.select();
//        pojo.useTimeZone(TimeZone.getTimeZone("UTC"));
//        pojo.status = "valid";
//        pojo.cust_name="sprint";
//        pojo.supplier_name="Neustar";
//        pojo.order_type="MultiportRequest";
//        pojo.order_type_version="1";
//        pojo.product="ICP";
//        pojo.last_trans_oid = new Long(5463);
//        pojo.created_dt = new java.util.Date();
//        pojo.insert();
//        
///*        List lst = pojo.queryByExample();
//        CHOrderPojo pojo1 = (CHOrderPojo)lst.get(0);
//        pojo.copy(pojo1);
//        pojo.update();
//*/
//        CHOrderPojo pojo1 = new CHOrderPojo();
//        pojo1.copy(pojo);
//        pojo1.useTimeZone(pojo.getPreferredTimeZone());
//        pojo1.update();
    }
}
