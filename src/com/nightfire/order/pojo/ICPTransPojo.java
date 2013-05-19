package com.nightfire.order.pojo;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.sql.Types;
import java.lang.Long;
import java.sql.Connection;

import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.order.utils.CHOrderException;

import biz.neustar.nsplatform.db.util.ColumnMetaData;
import biz.neustar.nsplatform.db.util.DBUtils;
import biz.neustar.nsplatform.db.util.DatabaseAware;
import biz.neustar.nsplatform.db.util.TableMetaData;
import biz.neustar.nsplatform.db.pojo.POJOBase;

/**
 * 	Pojo class to insert/update/query ICP_TRANS table.
 */
public class ICPTransPojo extends POJOBase
{
    public Long oid;
    public Long parent_oid;
    public String status;
    public String supplier_name;
    public String cust_name;
    public String requestnumber;
    public String requestversionid;
    public String grouprequestnumber;
    public String responsenumber;
    public String responseversionid;
    public String groupresponsenumber;
    public String messagetype;
    public String messagesubtype;
    public String suptype;
    public String responsetype;
    public String numportdirection;
    public String interfaceversion;
    public String direction;
    public String last_action;
    public String correlationid;
    public Date duedate;
    public String accountnumber;
    public String customeruse;
    public String firstname;
    public String lastname;
    public String businessname;
    public String timezone;
    public String created_by;
    public Date created_dt;
    public String updated_by;
    public Date updated_dt;
    public String submitted_by;
    public Date submitted_dt;
    public String last_status_by;
    public String last_status_dt;
    public String last_user_by;
    public String last_user_dt;
   // Variable isn't be mapped with any column of trans table , rather it's  for internal use.
    public String updateSubmittedDT;
    public String result_found= "true";
    
        
    public ICPTransPojo() throws Exception
    {
        super();
    }

    public TableMetaData getTableMetaData() throws Exception
    {

        /*
	  CREATE TABLE CH_ICP_TRANS
     ( 
       OID	NUMBER(38)	NOT NULL,
       PARENT_OID	NUMBER(38)	NOT NULL,
       STATUS	VARCHAR2(30) NOT NULL,
       SUPPLIER_NAME VARCHAR2(30) NOT NULL, 
       CUST_NAME VARCHAR2(32) NOT NULL, 
       REQUESTNUMBER VARCHAR2(16) NOT NULL, 
       REQUESTVERSIONID VARCHAR2(2) NOT NULL, 
       GROUPREQUESTNUMBER VARCHAR2(16), 
       RESPONSENUMBER VARCHAR2(18), 
       RESPONSEVERSIONID CHAR(10), 
       GROUPRESPONSENUMBER VARCHAR2(20), 
       MESSAGETYPE VARCHAR2(40) NOT NULL, 
       MESSAGESUBTYPE VARCHAR2(35) NOT NULL, 
       SUPTYPE VARCHAR2(1), 
       RESPONSETYPE VARCHAR2(1), 
       NUMPORTDIRECTION VARCHAR2(20), 
       INTERFACEVERSION VARCHAR2(10), 
       DIRECTION VARCHAR2(3) NOT NULL, 
       LAST_ACTION VARCHAR2(50) DEFAULT NULL, 
       CORRELATIONID VARCHAR2(64), 
       DUEDATE TIMESTAMP(0),
       ACCOUNTNUMBER VARCHAR2(20), 
       CUSTOMERUSE VARCHAR2(100), 
       FIRSTNAME VARCHAR2(25), 
       LASTNAME VARCHAR2(25), 
       BUSINESSNAME VARCHAR2(60), 
       TIMEZONE VARCHAR2(5) 
       CREATED_BY	VARCHAR2(32)	NOT NULL,
       CREATED_DT	TIMESTAMP(0)	NOT NULL,
       UPDATED_BY	VARCHAR2(32),	
       UPDATED_DT	TIMESTAMP(0),	
       SUBMITTED_BY	VARCHAR2(32),	
       SUBMITTED_DT	TIMESTAMP(0),	
       LAST_STATUS_BY	VARCHAR2(32),	
       LAST_STATUS_DT	TIMESTAMP(0),	
       LAST_USER_BY	VARCHAR2(32),	
       LAST_USER_DT	TIMESTAMP(0),
       CONSTRAINT CH_ICP_TRANS_PK PRIMARY KEY (OID)
      ); 
         */
        if (tmd == null)
        {
            synchronized(ICPTransPojo.class)
            {
                if(tmd != null)
                    return tmd;

                tmd = new TableMetaData("CH_ICP_TRANS", ICPTransPojo.class ); 

                tmd.addColumn(new ColumnMetaData("OID", Types.NUMERIC)).setPrimaryKeyIndex(0);
                tmd.addColumn(new ColumnMetaData("PARENT_OID", Types.NUMERIC));
                tmd.addColumn(new ColumnMetaData("STATUS"));
                tmd.addColumn(new ColumnMetaData("REQUESTNUMBER"));
                tmd.addColumn(new ColumnMetaData("REQUESTVERSIONID"));
                tmd.addColumn(new ColumnMetaData("GROUPREQUESTNUMBER"));
                tmd.addColumn(new ColumnMetaData("RESPONSENUMBER"));
                tmd.addColumn(new ColumnMetaData("RESPONSEVERSIONID"));
                tmd.addColumn(new ColumnMetaData("GROUPRESPONSENUMBER"));
                tmd.addColumn(new ColumnMetaData("MESSAGETYPE"));
                tmd.addColumn(new ColumnMetaData("MESSAGESUBTYPE"));
                tmd.addColumn(new ColumnMetaData("SUPTYPE"));
                tmd.addColumn(new ColumnMetaData("RESPONSETYPE"));
                tmd.addColumn(new ColumnMetaData("NUMPORTDIRECTION"));
                tmd.addColumn(new ColumnMetaData("INTERFACEVERSION"));
                tmd.addColumn(new ColumnMetaData("DIRECTION"));
                tmd.addColumn(new ColumnMetaData("LAST_ACTION"));
                tmd.addColumn(new ColumnMetaData("CORRELATIONID"));
                tmd.addColumn(new ColumnMetaData("DUEDATE",Types.TIMESTAMP)).disablePreferredTimeZone(); 
                tmd.addColumn(new ColumnMetaData("ACCOUNTNUMBER"));
                tmd.addColumn(new ColumnMetaData("CUSTOMERUSE"));
                tmd.addColumn(new ColumnMetaData("FIRSTNAME"));
                tmd.addColumn(new ColumnMetaData("LASTNAME"));
                tmd.addColumn(new ColumnMetaData("BUSINESSNAME"));
                tmd.addColumn(new ColumnMetaData("TIMEZONE"));
                tmd.addColumn(new ColumnMetaData("CREATED_BY"));
                tmd.addColumn(new ColumnMetaData("CREATED_DT",Types.TIMESTAMP));
                tmd.addColumn(new ColumnMetaData("UPDATED_BY"));
                tmd.addColumn(new ColumnMetaData("UPDATED_DT",Types.TIMESTAMP));
                tmd.addColumn(new ColumnMetaData("SUBMITTED_BY"));
                tmd.addColumn(new ColumnMetaData("SUBMITTED_DT",Types.TIMESTAMP));
                tmd.addColumn(new ColumnMetaData("LAST_STATUS_BY"));
                tmd.addColumn(new ColumnMetaData("LAST_STATUS_DT",Types.TIMESTAMP));
                tmd.addColumn(new ColumnMetaData("LAST_USER_BY"));
                tmd.addColumn(new ColumnMetaData("LAST_USER_DT",Types.TIMESTAMP));
                tmd.addColumn(new ColumnMetaData("CUST_NAME"));
                tmd.addColumn(new ColumnMetaData("SUPPLIER_NAME"));
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

        // While inserting transaction at 'save' action submitted_dt, submitted_by,  updated_dt , updated_by must be null.
        if( (MULTIPORT_REQUEST_MSG_TYPE.equals(messagetype) || MULTIPORT_RESPONSE_MSG_TYPE.equals(messagetype)
               ||MODIFYPORT_REQUEST_MSG_TYPE.equals(messagetype)) && SAVE_EVENT_CODE.equals(last_action) )
        {

                   submitted_by  = null;
                   insertCols = addColumn(insertCols,SUBMITTED_BY_COL);

                   submitted_dt = null;
                   insertCols = addColumn(insertCols,SUBMITTED_DT_COL);

                   updated_by  = null;
                   insertCols = addColumn(insertCols,UPDATED_BY_COL);

                   updated_dt = null;
                   insertCols = addColumn(insertCols,UPDATED_DT_COL);

        }


        oid = new Long(DBUtils.getInstance().getSequenceValue(ICP_TRANS_SEQ));
        insertCols = addColumn( insertCols, "oid" );

        created_by  = CustomerContext.getInstance().getUserID();  
        insertCols = addColumn(insertCols,CREATED_BY_COL);

        created_dt = new java.util.Date();
        insertCols = addColumn(insertCols,CREATED_DT_COL);

        return insertCols;
    }

    public ColumnMetaData[] beforeUpdate ( ColumnMetaData[] updateCols ) throws Exception
    {
        // While updateding record on save action submitted_dt, submitted_by must not be populated or must be null.
        
        if( (MULTIPORT_REQUEST_MSG_TYPE.equals(messagetype) || MULTIPORT_RESPONSE_MSG_TYPE.equals(messagetype)
               || MODIFYPORT_REQUEST_MSG_TYPE.equals(messagetype)) && SAVE_EVENT_CODE.equals(last_action) )
          {
                   submitted_by  = null;
                   updateCols = addColumn(updateCols,SUBMITTED_BY_COL);

                   submitted_dt = null;
                   updateCols = addColumn(updateCols,SUBMITTED_DT_COL);
          }


        if( (STORE_STATUS_TYPE.equals(messagetype) && IN_DIRECTION.equals(direction)) || StringUtils.hasValue(updateSubmittedDT))
        {
            submitted_by  = CustomerContext.getInstance().getUserID();
            updateCols = addColumn(updateCols,SUBMITTED_BY_COL);

            submitted_dt = new java.util.Date();
            updateCols = addColumn(updateCols,SUBMITTED_DT_COL);
          }

        
        // get the user from context
        updated_by  = CustomerContext.getInstance().getUserID(); 
        updateCols = addColumn(updateCols,UPDATED_BY_COL);

        updated_dt = new java.util.Date();
        updateCols = addColumn(updateCols,UPDATED_DT_COL);

        return updateCols;
    }

    /**
     * gets the latest record ordered by (updated date, created date) of ICP trans 
     * pojo. 
     * @param dbConn
     * @throws Exception
     */
    public void queryLatest(Connection dbConn) throws Exception
    {
        LinkedList list = new LinkedList(queryByExample(dbConn, " order by nvl(updated_dt, created_dt) desc"));

        if(list == null || list.size() <= 0)
        {
            ICPTransPojo transpojo =  new ICPTransPojo();
            copy(transpojo);
            result_found ="false";

        }
        else{
            copy((POJOBase)list.get(0));
        }

    }

    /**
     * On upsert the trans record will be either updated or a new record will be inserted. This will be based on
     * uniqueness of <CUSTOMERID,CORRELATIOID and REQUESTNUMBER>
     * @param dbConn
     * @throws Exception
     */

    public void fetchAndUpdate(Connection dbConn) throws Exception
    {
        // check for a trans pojo if exists with same CUSTOMERID,CORRELATIOID and REQUESTNUMBER
        // to check this follow the following steps:

        // 1) create a temporary POJO with all 3 values set.
        ICPTransPojo tempTransPojo = new ICPTransPojo();
        tempTransPojo.cust_name = this.cust_name;
        tempTransPojo.correlationid = this.correlationid;
        tempTransPojo.requestnumber = this.requestnumber;

        // 2) select any such record
        List transList = tempTransPojo.queryByExample(dbConn);

        // 3.1) if no such record which is an invalid condition.
        if(transList == null || transList.size() <= 0 )
        {
            throw new CHOrderException("No TRANS Record found to update with cust_name["+cust_name+"], correlationid["+correlationid+"] and requestNumber["+requestnumber+"]");
        }
        // 3.2) else update such a record.
        else
        {
            // 3.2.1) again if multiple such records are found this is not again a valid condition
            //        throw an CHOrderException stating uniqueness constraint violated for valid tranaction.

            if( transList.size() > 1)
                throw new CHOrderException("Multiple Trans records found while updating for customerid["+cust_name+"], correlationid["+correlationid+"] and requestNumber["+requestnumber+"]");
            // 3.2.2) for a single record update the trans record.
            else if( transList.size() == 1)
            {
                // get the records OID
                Long oid = ((ICPTransPojo)transList.get(0)).oid;
                this.oid = oid;
                // assuming all other properties are correctly set using the configuration file.
                update(dbConn);
            }
            else
                throw new CHOrderException("Unreachable condition in TRANS UPDATE");
        }
    }
    
    @Override
    public void copy(DatabaseAware source) throws Exception {

        TimeZone tz = ((POJOBase)this).getPreferredTimeZone();
        super.copy(source);
        if(tz!=null)
            this.useTimeZone(tz);
        
        if(!source.getTableMetaData().getColumn("DUEDATE").isPreferredTimeZoneEnabled())
            this.getTableMetaData().getColumn("DUEDATE").disablePreferredTimeZone();
            
    }

    /**
     * Insert a new record if record record already exists in the Trans table, otherwise 
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
      
    public static final String UPDATED_BY_COL = "UPDATED_BY";
    public static final String UPDATED_DT_COL = "UPDATED_DT";
    public static final String CREATED_BY_COL = "CREATED_BY";
    public static final String CREATED_DT_COL = "CREATED_DT";    
    public static final String SUBMITTED_BY_COL = "SUBMITTED_BY";
    public static final String SUBMITTED_DT_COL = "SUBMITTED_DT";
    public static final String STORE_STATUS_TYPE = "StoreStatus";
    public static final String MODIFYPORT_REQUEST_MSG_TYPE = "ModifyPortRequest";
    public static final String MULTIPORT_RESPONSE_MSG_TYPE = "MultiPortResponse";
    public static final String MULTIPORT_REQUEST_MSG_TYPE = "MultiPortRequest";
    public static final String SAVE_EVENT_CODE = "save";
    public static final String IN_DIRECTION = "In";

    public static final String ICP_TRANS_SEQ = "ICP_TRANS_SEQ";


    // Should be static, as we only want one instance per POJO type.
    private static TableMetaData tmd = null;
}
