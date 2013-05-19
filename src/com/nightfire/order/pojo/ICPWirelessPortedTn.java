package com.nightfire.order.pojo;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Connection;
import java.util.StringTokenizer;

import biz.neustar.nsplatform.db.pojo.POJOBase;
import biz.neustar.nsplatform.db.util.ColumnMetaData;
import biz.neustar.nsplatform.db.util.TableMetaData;

import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;

public class ICPWirelessPortedTn extends POJOBase {

    public Long oid;
    public String starttn;
    public String endtn;
    public long tncount;
    public String responsecode;
    public String cust_name;
    public String lnum;
    public String event_code;

    /**
     * Ordered list of <LNUM,START_TN,END_TN,TN_COUNT,RESPONSE_CODE>
     * formated as LNUM|START_TN|END_TN|TN_COUNT|RESPONSE_CODE#LNUM|START_TN|END_TN|TN_COUNT|RESPONSE_CODE#.....
     */
    public String portedlinerequestlist;

    public ICPWirelessPortedTn() throws Exception {
        super();
    }

    public TableMetaData getTableMetaData() throws Exception {
        /*
         * 	CREATE TABLE ICP_WIRELESS_PORTED_TN 
         *  (OID NUMBER(38) NOT NULL, 
         *  CUST_NAME VARCHAR2(32) NOT NULL, 
         *  STARTTN VARCHAR2(12) NOT NULL, 
         *  ENDTN VARCHAR2(12) NOT NULL, 
         *  TNCOUNT NUMBER(10) DEFAULT 1 NOT NULL, 
         *  RESPONSECODE VARCHAR2(2),
         *  LNUM VARCHAR2(10)
         *  CONSTRAINT ICP_WIRELESS_PORTED_TN_PK PRIMARY KEY (OID)); 
         */

        if (tmd == null)
        {
            synchronized(ICPWirelessPortedTn.class)
            {
                if(tmd != null)
                    return tmd;

                tmd = new TableMetaData("ICP_WIRELESS_PORTED_TN", ICPWirelessPortedTn.class );
                tmd.addColumn(new ColumnMetaData("OID", Types.NUMERIC)).setPrimaryKeyIndex(0);
                tmd.addColumn(new ColumnMetaData("CUST_NAME"));
                tmd.addColumn(new ColumnMetaData("STARTTN"));
                tmd.addColumn(new ColumnMetaData("ENDTN"));
                tmd.addColumn(new ColumnMetaData("TNCOUNT", Types.NUMERIC));
                tmd.addColumn(new ColumnMetaData("RESPONSECODE"));
                tmd.addColumn(new ColumnMetaData("LNUM"));
            }
        }

        return tmd;
    }

    
    public void multiUpsert ( Connection dbConn ) throws Exception
    {
        // Check event_code, if it is 'save' skip the TN logging  
        if(SAVE_EVENT_CODE.equals(event_code)){
            return;
        }
        
        if(! StringUtils.hasValue(portedlinerequestlist))
          return;

        if(oid!=null )
        {
            PreparedStatement pstmt = null;
            try
            {
                pstmt = dbConn.prepareStatement(" DELETE FROM "+this.getTableMetaData().getTableName()+" WHERE OID = ?");
                pstmt.setLong(1, oid);
                pstmt.execute();
            }
            catch(SQLException sqle)
            {
                if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
                    Debug.log(Debug.ALL_ERRORS,"An exception occurred while deleting " +
                            "records for oid["+oid+"]"+sqle.getMessage());
            }
            finally
            {
                if(pstmt!=null)
                    pstmt.close();
            }
        }
       // Seperate each record using delimiter "#"
       StringTokenizer tokenizer = new StringTokenizer(portedlinerequestlist,"#");
        while(tokenizer.hasMoreTokens())
        {
            // for each delimited record seperate each word using delimiter "|"
            String portedLineRequest = tokenizer.nextToken();
            StringTokenizer tokenizer1 = new StringTokenizer(portedLineRequest,"|");
            String lNum = tokenizer1.nextToken();
            String sTN = tokenizer1.nextToken();
            String eTN = tokenizer1.nextToken();
            String tnCount = tokenizer1.nextToken();
            String respCode = tokenizer1.nextToken();
            // insert record to the table
            insert(lNum,sTN,eTN,tnCount,respCode,dbConn);
        }

        

    }

    private void insert(String lNum, String startTN, String endTN, String tnCount, String respCode,Connection dbConn) throws Exception
    {
        ICPWirelessPortedTn portedTNPOJO = new ICPWirelessPortedTn();
        // copy current pojo
        portedTNPOJO.copy(this);
        // set the other attributes ...
        portedTNPOJO.lnum = StringUtils.hasValue(lNum)?lNum:null;
        portedTNPOJO.starttn = StringUtils.hasValue(startTN)?startTN:null;
        portedTNPOJO.endtn = StringUtils.hasValue(endTN)?endTN:null;
        portedTNPOJO.tncount = StringUtils.hasValue(tnCount)?Integer.parseInt(tnCount):-1;
        portedTNPOJO.responsecode = StringUtils.hasValue(respCode)?respCode:null;
        portedTNPOJO.cust_name = CustomerContext.getInstance().getCustomerID();
        // insert this record.
        portedTNPOJO.insert(dbConn);
    }
    // Should be static, as we only want one instance per POJO type.
    private static TableMetaData tmd = null;
    private static final String SAVE_EVENT_CODE = "save";

}
