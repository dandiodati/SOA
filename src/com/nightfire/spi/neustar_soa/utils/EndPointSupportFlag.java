package com.nightfire.spi.neustar_soa.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;


public class EndPointSupportFlag implements CachingObject
{
	private static HashMap hmEndPointUrlProp = new HashMap();
    private static String className = "NANCSupportFlag";
    private boolean usingContextConnection = true;	
    private String customerid;
    
    private LinkedList<String> guiNotificationFlagList = new LinkedList<String>();
    private LinkedList<String> svCreateNotifUrlList = new LinkedList<String>();    
    private LinkedList<String> svCreateAckNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svReleaseNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svReleaseAckNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svReleaseInConfNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svReleaseInConfAckNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svActivateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svDisconnectNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svStatusChangeNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svAttrChangeNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svPtoNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svRtdNotifUrlList = new LinkedList<String>();
    private LinkedList<String> t1CreateReqNotifUrlList = new LinkedList<String>();
    private LinkedList<String> t1ConcReqNotifUrlList = new LinkedList<String>();
    private LinkedList<String> t2FinalCreateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> t2FinalConcNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svCancelNotifUrlList = new LinkedList<String>();
    private LinkedList<String> svCancelAckNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npbCreateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npbActivateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npbModifyNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npbStatusChangeNotifUrlList = new LinkedList<String>();
    private LinkedList<String> spidCreateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> spidModifyNotifUrlList = new LinkedList<String>();
    private LinkedList<String> spidDeleteNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npaNxxFirstNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npaNxxCreateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npaNxxDeleteNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npaNxxXCreateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npaNxxXModifyNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npaNxxXDeleteNotifUrlList = new LinkedList<String>();
    private LinkedList<String> lrnCreateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> lrnDeleteNotifUrlList = new LinkedList<String>();
    private LinkedList<String> auditCreateNotifUrlList = new LinkedList<String>();
    private LinkedList<String> auditDeleteNotifUrlList = new LinkedList<String>();
    private LinkedList<String> auditDiscRepNotifUrlList = new LinkedList<String>();
    private LinkedList<String> auditResultsNotifUrlList = new LinkedList<String>();
    private LinkedList<String> npacSuccessReplyUrlList = new LinkedList<String>();
    private LinkedList<String> npacFailureReplyUrlList = new LinkedList<String>();
    private LinkedList<String> svQueryReplyUrlList = new LinkedList<String>();
    private LinkedList<String> npbQueryReplyUrlList = new LinkedList<String>();
    private LinkedList<String> spidQueryReplyUrlList = new LinkedList<String>();
    private LinkedList<String> npaNxxQueryReplyUrlList = new LinkedList<String>();
    private LinkedList<String> npaNxxXQueryReplyUrlList = new LinkedList<String>();
    private LinkedList<String> lrnQueryReplyUrlList = new LinkedList<String>();
    private LinkedList<String> auditQueryReplyUrlList = new LinkedList<String>();
    private LinkedList<String> npacDownNotifUrlList = new LinkedList<String>();
    private LinkedList<String> orderQueryReplyUrlList = new LinkedList<String>();
    
    //private static String dbname="jdbc:oracle:thin:@192.168.64.177:1521:BPELDEV";
   // private static String dbuser="soadev";
   // private static String dbpass="soadev";
    
    private static String dbname;
    private static String dbuser;
    private static String dbpass;
    
	private static String GETCUSTOMERURL = "SELECT ENDPOINTURL, GUINOTIFICATIONFLAG, SVCREATENOTFLAG, SVCREATEACKNOTFLAG, SVRELEASENOTFLAG, " +
			"SVRELEASEACKNOTFLAG, SVRELEASEINCONFLICTNOTFLAG, SVRELEASEINCONFLICTACKNOTFLAG, SVACTIVATENOTFLAG, " +
			"SVSTATUSCHANGENOTFLAG, SVATTRIBUTECHANGENOTFLAG, SVDISCONNECTNOTFLAG, SVPTONOTFLAG, SVRETURNTODONORNOTFLAG, " +
			"T1CREATEREQNOTFLAG, T1CONCURRENCEREQNOTFLAG, T2FINALCREATENOTFLAG, T2FINALCONCURRENCENOTFLAG, SVCANCELNOTFLAG, " +
			"SVCANCELACKNOTFLAG, NPBCREATENOTFLAG, NPBACTIVATENOTFLAG, NPBMODIFYNOTFLAG, NPBSTATUSCHANGENOTFLAG, " +
			"SPIDCREATENOTFLAG, SPIDMODIFYNOTFLAG, SPIDDELETENOTFLAG, NPANXXFIRSTUSAGENOTFLAG, NPANXXCREATENOTFLAG, " +
			"NPANXXDELETENOTFLAG, NPANXXXCREATENOTFLAG, NPANXXXMODIFYNOTFLAG, NPANXXXDELETENOTFLAG, LRNCREATENOTFLAG, " +
			"LRNDELETENOTFLAG, AUDITCREATENOTFLAG, AUDITDELETENOTFLAG, AUDITDISCREPANCYREPORTNOTFLAG, AUDITRESULTSNOTFLAG, " +
			"NPACREQSUCCESSREPLYFLAG, NPACREQFAILUREREPLYFLAG, SVQUERYREPLYFLAG, NPBQUERYREPLYFLAG, SPIDERQUERYREPLYFLAG, " +
			"NPANXXQUERYREPLYFLAG, NPANXXXQUERYREPLYFLAG, LRNQUERYREPLYFLAG, AUDITQUERYREPLYFLAG, NPACSHUTDOWNNOTFLAG, " +
			"ORDERQUERYREPLYFLAG FROM SOA_ENDPOINTS WHERE CUSTOMERID=?";
	
    private static EndPointSupportFlag createInstance (String customerid)
    {
        Connection dbConn = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;        
        EndPointSupportFlag endPointUrlProp = null;   
        try
        {
            //dbConn = DBInterface.acquireConnection();
        	
			dbConn = DBConnectionPool.getInstance(true).acquireConnection();
		
			if (dbConn == null) {
				throw new ProcessingException("DB connection is not available");
			}
        	
           // Class.forName("oracle.jdbc.driver.OracleDriver");
           // dbConn = DriverManager.getConnection(dbname, dbuser, dbpass);        
        	
        	pstmt = dbConn.prepareStatement(GETCUSTOMERURL); 
            pstmt.setString(1, customerid);
            rset = pstmt.executeQuery();
            
            endPointUrlProp = new EndPointSupportFlag();
            endPointUrlProp.setCustomer(customerid);
            while(rset.next()){
            	
            	if(1 == rset.getInt(2) ){
            		endPointUrlProp.setguiNotificationFlagList(rset.getString(1));
            	}
            	if(1 == rset.getInt(3) ){
            		endPointUrlProp.setSvCreateNotifFlag(rset.getString(1));
            	}
            	if(1 == rset.getInt(4) ){
            		endPointUrlProp.setSvCreateAckNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(5) ){
            		endPointUrlProp.setSvReleaseNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(6) ){
            		endPointUrlProp.setSvReleaseAckNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(7) ){
            		endPointUrlProp.setSvReleaseInConfNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(8) ){
            		endPointUrlProp.setSvReleaseInConfAckNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(9) ){
            		endPointUrlProp.setSvActivateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(10) ){
            		endPointUrlProp.setSvStatusChangeNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(11) ){
            		endPointUrlProp.setSvAttrChangeNotifFlag(rset.getString(1));     		
            	}
            	if(1 == rset.getInt(12) ){
            		endPointUrlProp.setSvDisconNotifFlag(rset.getString(1));           		
            	}
            	if(1 == rset.getInt(13) ){
            		endPointUrlProp.setPTONotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(14) ){
            		endPointUrlProp.setRtdNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(15) ){
            		endPointUrlProp.setT1CreateReqNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(16) ){
            		endPointUrlProp.setT1ConcReqNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(17) ){
            		endPointUrlProp.setT2FinalCreateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(18) ){
            		endPointUrlProp.setT2FinalConcNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(19) ){
            		endPointUrlProp.setSvCancelNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(20) ){
            		endPointUrlProp.setSvCancelAckNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(21) ){
            		endPointUrlProp.setNpbCreateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(22) ){
            		endPointUrlProp.setNpbActivateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(23) ){
            		endPointUrlProp.setNpbModifyNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(24) ){
            		endPointUrlProp.setNpbStatusChangeNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(25) ){
            		endPointUrlProp.setSpidCreateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(26) ){
            		endPointUrlProp.setSpidModifyNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(27) ){
            		endPointUrlProp.setSpidDeleteNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(28) ){
            		endPointUrlProp.setNpaNxxFirstNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(29) ){
            		endPointUrlProp.setNpaNxxCreateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(30) ){
            		endPointUrlProp.setNpaNxxDeleteNotifFlag(rset.getString(1));            	                                         	
            	}
            	if(1 == rset.getInt(31) ){
            		endPointUrlProp.setNpaNxxXCreateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(32) ){
            		endPointUrlProp.setNpaNxxXModifyNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(33) ){
            		endPointUrlProp.setNpaNxxXDeleteNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(34) ){
            		endPointUrlProp.setLrnCreateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(35) ){
            		endPointUrlProp.setLrnDeleteNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(36) ){
            		endPointUrlProp.setAuditCreateNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(37) ){
            		endPointUrlProp.setAuditDeleteNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(38) ){
            		endPointUrlProp.setAuditDiscRepNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(39) ){
            		endPointUrlProp.setAuditResultsNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(40) ){
            		endPointUrlProp.setNpacSuccessReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(41) ){
            		endPointUrlProp.setNpacFailureReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(42) ){
            		endPointUrlProp.setSvQueryReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(43) ){
            		endPointUrlProp.setNpbQueryReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(44) ){
            		endPointUrlProp.setSpidQueryReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(45) ){
            		endPointUrlProp.setNpaNxxQueryReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(46) ){
            		endPointUrlProp.setNpaNxxXQueryReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(47) ){
            		endPointUrlProp.setLrnQueryReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(48) ){
            		endPointUrlProp.setAuditQueryReplyFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(49) ){
            		endPointUrlProp.setNpacDownNotifFlag(rset.getString(1));            		
            	}
            	if(1 == rset.getInt(50) ){
            		endPointUrlProp.setOrderQueryReplyFlag(rset.getString(1));            		
            	}
            }
                        
        } catch (Exception e ) {
            Debug.error("createInstance(): Unable to acquire Database connection. Error: " + e.toString());
            
        } /*catch (Exception e) {
            Debug.error("createInstance(): Error: " + e.toString());
        }*/ finally {
            try{
            	if(rset !=null){
            		rset.close();
            	}
            	if(pstmt!=null){
            		pstmt.close();
            	}
            }catch(SQLException sqle){
            	Debug.error("createInstance(): Error while closing ResultSet and Statements objects");
            }            
        	/** Here try is required to release the DB Connection Pool Instance */            
        	try {
               // DBInterface.releaseConnection(dbConn);
        		DBConnectionPool.getInstance(true).releaseConnection(dbConn);
				dbConn = null;
            }
            catch (Exception e) {
                Debug.error("createInstance(): Error: " + e.toString());
            }
        }
        return endPointUrlProp;
    }
    
    
	/**
     * This method would check if the instance of the EndPointSupportFlag already exists,
     * if find, the instance from cache will return else new instance will return.
     *
     * @param spid String
     * @return EndPointSupportFlag instance
     * @throws NancPropException on error
     */
    public static EndPointSupportFlag getInstance (String customerid) throws NancPropException
    {
        if (!StringUtils.hasValue(customerid)) {
            throw new NancPropException("getInstance(customerid): customerid is null or empty String.");
        }

        EndPointSupportFlag endPointUrlProp = null;
        
        endPointUrlProp = (EndPointSupportFlag) hmEndPointUrlProp.get(customerid);

        if (endPointUrlProp == null)
        {
            
        	synchronized (hmEndPointUrlProp) {
            	
            	endPointUrlProp = createInstance(customerid);
            	
                if (endPointUrlProp!=null) {
                	hmEndPointUrlProp.put(endPointUrlProp.getCustomer(), endPointUrlProp);
                }
                if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                    Debug.log(Debug.SECURITY_CONFIG,
                            "getInstance(customerid): Total instance in pool for endPointUrlProp are [" + hmEndPointUrlProp.size() + "]");
                }
            }
        }
        else
        {
        	
        	if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                Debug.log(Debug.SECURITY_CONFIG,
                        "getInstance(customerid): Instance found in pool for spid [" + customerid + "]");
            }
        }

        if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
            Debug.log(Debug.SECURITY_CONFIG,
                    "getInstance(spid): Returning instance for spid [" + customerid + "]");
        }

        return endPointUrlProp;
    }

    /**
     * This method makes the hash map
     * containing the SOA End Point URL Properties
     * per customer wise null (EMPTY).
     */
    public void flushCache() throws FrameworkException 
    {
        if (hmEndPointUrlProp!=null)
        {
            synchronized(hmEndPointUrlProp)
            {
            	hmEndPointUrlProp.clear();
            	
                if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                    Debug.log(Debug.SECURITY_CONFIG,
                            "EndPointSupportFlag(): Flushing the Properties...");
                }
            }
        } else {
            if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                Debug.log(Debug.SECURITY_CONFIG,
                        "EndPointSupportFlag(): Being null no need to flush the Properties...");
            }
        }
    }

    private void setCustomer( String customerid )
    {
        this.customerid = customerid;
    }
    public String getCustomer()
    {
        return customerid;
    }
    
    private void setguiNotificationFlagList( String guiNotificationUrl)
    {
        this.guiNotificationFlagList.add(guiNotificationUrl);        
    }

    public LinkedList<String> getguiNotificationFlagList()
    {
        return guiNotificationFlagList;
    } 
    
    
    private void setSvCreateNotifFlag( String svCreateNotifUrl )
    {
        this.svCreateNotifUrlList.add(svCreateNotifUrl);        
    }

    public LinkedList<String> getSvCreateNotifFlag()
    {
        return svCreateNotifUrlList;
    }    
    
    private void setSvCreateAckNotifFlag( String svCreateAckNotifUrl )
    {
        this.svCreateAckNotifUrlList.add(svCreateAckNotifUrl);        
    }
    
    public LinkedList<String> getSvCreateAckNotifFlag()
    {
        return svCreateAckNotifUrlList;
    }
    
    private void setSvReleaseNotifFlag( String svReleaseNotifUrl )
    {
        this.svReleaseNotifUrlList.add(svReleaseNotifUrl);        
    }
    
    public LinkedList<String> getSvReleaseNotifFlag()
    {
        return svReleaseNotifUrlList;
    }
    
    private void setSvReleaseAckNotifFlag( String svReleaseAckNotifUrl )
    {
        this.svReleaseAckNotifUrlList.add(svReleaseAckNotifUrl);        
    }
    
    public LinkedList<String> getSvReleaseAckNotifFlag()
    {
        return svReleaseAckNotifUrlList;
    }
    
    private void setSvReleaseInConfNotifFlag( String svReleaseInConfNotifUrl )
    {
        this.svReleaseInConfNotifUrlList.add(svReleaseInConfNotifUrl);        
    }
    
    public LinkedList<String> getSvReleaseInConfNotifFlag()
    {
        return svReleaseInConfNotifUrlList;
    }
    
    private void setSvReleaseInConfAckNotifFlag( String svReleaseInConfAckNotifUrl )
    {
        this.svReleaseInConfAckNotifUrlList.add(svReleaseInConfAckNotifUrl);        
    }
    
    public LinkedList<String> getSvReleaseInConAckfNotifFlag()
    {
        return svReleaseInConfAckNotifUrlList;
    }
    
    private void setSvActivateNotifFlag( String svActivateNotifUrl )
    {
        this.svActivateNotifUrlList.add(svActivateNotifUrl);        
    }
    
    public LinkedList<String> getSvActivateNotifFlag()
    {
        return svActivateNotifUrlList;
    }
    
    private void setSvStatusChangeNotifFlag( String svStatusChangeNotifUrl )
    {
        this.svStatusChangeNotifUrlList.add(svStatusChangeNotifUrl);  
    }
    
    public LinkedList<String> getSvStatusChangeNotifFlag()
    {
        return svStatusChangeNotifUrlList;
    }
    
    private void setSvAttrChangeNotifFlag( String svAttrChangeNotifUrl )
    {
        this.svAttrChangeNotifUrlList.add(svAttrChangeNotifUrl);  
    }
    
    public LinkedList<String> getSvAttrChangeNotifFlag()
    {
        return svAttrChangeNotifUrlList;
    }
    
    private void setSvDisconNotifFlag( String svDisconNotifUrl )
    {
        this.svDisconnectNotifUrlList.add(svDisconNotifUrl);        
    }
    
    public LinkedList<String> getSvDisconNotifFlag()
    {
        return svDisconnectNotifUrlList;
    }
    
    private void setPTONotifFlag( String svPtoNotifUrl )
    {
        this.svPtoNotifUrlList.add(svPtoNotifUrl);        
    }
    
    public LinkedList<String> getSvPtoNotifFlag()
    {
        return svPtoNotifUrlList;
    }
    
    private void setRtdNotifFlag( String svRtdNotifUrl )
    {
        this.svRtdNotifUrlList.add(svRtdNotifUrl);        
    }
    
    public LinkedList<String> getSvRtdNotifFlag()
    {
        return svRtdNotifUrlList;
    }
    
    private void setT1CreateReqNotifFlag( String t1CreateReqNotifUrl )
    {
        this.t1CreateReqNotifUrlList.add(t1CreateReqNotifUrl);        
    }
    
    public LinkedList<String> getT1CreateReqNotifFlag()
    {
        return t1CreateReqNotifUrlList;
    }
    
    private void setT1ConcReqNotifFlag( String t1ConcReqNotifUrl )
    {
        this.t1ConcReqNotifUrlList.add(t1ConcReqNotifUrl);        
    }
    
    public LinkedList<String> getT1ConcReqNotifFlag()
    {
        return t1ConcReqNotifUrlList;
    }
    
    private void setT2FinalCreateNotifFlag( String t2FinalCreateNotifUrl )
    {
        this.t2FinalCreateNotifUrlList.add(t2FinalCreateNotifUrl);        
    }
    
    public LinkedList<String> getT2FinalCreateNotifFlag()
    {
        return t2FinalCreateNotifUrlList;
    }
    
    private void setT2FinalConcNotifFlag( String t2FinalConcNotifUrl )
    {
        this.t2FinalConcNotifUrlList.add(t2FinalConcNotifUrl);
    }
    
    public LinkedList<String> getT2FinalConcNotifFlag()
    {
        return t2FinalConcNotifUrlList;
    }
    
    private void setSvCancelNotifFlag( String svCancelNotifUrl )
    {
        this.svCancelNotifUrlList.add(svCancelNotifUrl);
    }
    
    public LinkedList<String> getSvCancelNotifFlag()
    {
        return svCancelNotifUrlList;
    }
    
    private void setSvCancelAckNotifFlag( String svCancelAckNotifUrl )
    {
        this.svCancelAckNotifUrlList.add(svCancelAckNotifUrl);
    }
    
    public LinkedList<String> getSvCancelAckNotifFlag()
    {
        return svCancelAckNotifUrlList;
    }
    
    private void setNpbCreateNotifFlag( String npbCreateNotifUrl )
    {
        this.npbCreateNotifUrlList.add(npbCreateNotifUrl);
    }
    
    public LinkedList<String> getNpbCreateNotifFlag()
    {
        return npbCreateNotifUrlList;
    }
    
    private void setNpbActivateNotifFlag( String npbActivateNotifUrl )
    {
        this.npbActivateNotifUrlList.add(npbActivateNotifUrl);
    }
    
    public LinkedList<String> getNpbActivateNotifFlag()
    {
        return npbActivateNotifUrlList;
    }
    
    private void setNpbModifyNotifFlag( String npbModifyNotifUrl )
    {
        this.npbModifyNotifUrlList.add(npbModifyNotifUrl);
    }
    
    public LinkedList<String> getNpbModifyNotifFlag()
    {
        return npbModifyNotifUrlList;
    }
	
    private void setNpbStatusChangeNotifFlag( String npbStatusChangeNotifUrl )
    {
        this.npbStatusChangeNotifUrlList.add(npbStatusChangeNotifUrl);
    }
    
    public LinkedList<String> getNpbStatusChangeNotifFlag()
    {
        return npbStatusChangeNotifUrlList;
    }
    
    private void setSpidCreateNotifFlag( String spidCreateNotifUrl )
    {
        this.spidCreateNotifUrlList.add(spidCreateNotifUrl);
    }
    
    public LinkedList<String> getSpidCreateNotifFlag()
    {
        return spidCreateNotifUrlList;
    }
    
    private void setSpidModifyNotifFlag( String spidModifyNotifUrl )
    {
        this.spidModifyNotifUrlList.add(spidModifyNotifUrl);
    }
    
    public LinkedList<String> getSpidModifyNotifFlag()
    {
        return spidModifyNotifUrlList;
    }
    
    private void setSpidDeleteNotifFlag( String spidDeleteNotifUrl )
    {
        this.spidDeleteNotifUrlList.add(spidDeleteNotifUrl);
    }
    
    public LinkedList<String> getSpidDeleteNotifFlag()
    {
        return spidDeleteNotifUrlList;
    }
    
    private void setNpaNxxFirstNotifFlag( String npaNxxFirstNotifUrl )
    {
        this.npaNxxFirstNotifUrlList.add(npaNxxFirstNotifUrl);
    }
    
    public LinkedList<String> getNpaNxxFirstNotifFlag()
    {
        return npaNxxFirstNotifUrlList;
    }
    
    private void setNpaNxxCreateNotifFlag( String npaNxxCreateNotifUrl )
    {
        this.npaNxxCreateNotifUrlList.add(npaNxxCreateNotifUrl);
    }
    
    public LinkedList<String> getNpaNxxCreateNotifFlag()
    {
        return npaNxxCreateNotifUrlList;
    }
    
    private void setNpaNxxDeleteNotifFlag( String npaNxxDeleteNotifUrl )
    {
        this.npaNxxDeleteNotifUrlList.add(npaNxxDeleteNotifUrl);
    }
    
    public LinkedList<String> getNpaNxxDeleteNotifFlag()
    {
        return npaNxxDeleteNotifUrlList;
    }
    
    private void setNpaNxxXCreateNotifFlag( String npaNxxXCreateNotifUrl )
    {
        this.npaNxxXCreateNotifUrlList.add(npaNxxXCreateNotifUrl);
    }
    
    public LinkedList<String> getNpaNxxXCreateNotifFlag()
    {
        return npaNxxXCreateNotifUrlList;
    }
    
    private void setNpaNxxXModifyNotifFlag( String npaNxxXModifyNotifUrl )
    {
        this.npaNxxXModifyNotifUrlList.add(npaNxxXModifyNotifUrl);
    }
    
    public LinkedList<String> getNpaNxxXModifyNotifFlag()
    {
        return npaNxxXModifyNotifUrlList;
    }
    
    private void setNpaNxxXDeleteNotifFlag( String npaNxxXDeleteNotifUrl )
    {
        this.npaNxxXDeleteNotifUrlList.add(npaNxxXDeleteNotifUrl);
    }
    
    public LinkedList<String> getNpaNxxXDeleteNotifFlag()
    {
        return npaNxxXDeleteNotifUrlList;
    }
    
    private void setLrnCreateNotifFlag( String lrnCreateNotifUrl )
    {
        this.lrnCreateNotifUrlList.add(lrnCreateNotifUrl);
    }
    
    public LinkedList<String> getNpaLrnCreateNotifFlag()
    {
        return lrnCreateNotifUrlList;
    }
    
    private void setLrnDeleteNotifFlag( String lrnDeleteNotifUrl )
    {
        this.lrnDeleteNotifUrlList.add(lrnDeleteNotifUrl);
    }
    
    public LinkedList<String> getNpaLrnDeleteNotifFlag()
    {
        return lrnDeleteNotifUrlList;
    }
    
    private void setAuditCreateNotifFlag( String auditCreateNotifUrl )
    {
        this.auditCreateNotifUrlList.add(auditCreateNotifUrl);
    }
    
    public LinkedList<String> getAuditCreateNotifFlag()
    {
        return auditCreateNotifUrlList;
    }
    
    private void setAuditDeleteNotifFlag( String auditDeleteNotifUrl )
    {
        this.auditDeleteNotifUrlList.add(auditDeleteNotifUrl);
    }
    
    public LinkedList<String> getAuditDeleteNotifFlag()
    {
        return auditDeleteNotifUrlList;
    }
    
    private void setAuditDiscRepNotifFlag( String auditDiscRepNotifUrl )
    {
        this.auditDiscRepNotifUrlList.add(auditDiscRepNotifUrl);
    }
    
    public LinkedList<String> getAuditDiscRepNotifFlag()
    {
        return auditDiscRepNotifUrlList;
    }
    
    private void setAuditResultsNotifFlag( String auditResultsNotifUrl )
    {
        this.auditResultsNotifUrlList.add(auditResultsNotifUrl);
    }
    
    public LinkedList<String> getAuditResultsNotifFlag()
    {
        return auditResultsNotifUrlList;
    }
    
    private void setNpacSuccessReplyFlag( String npacSuccessReplyUrl )
    {
        this.npacSuccessReplyUrlList.add(npacSuccessReplyUrl);
    }
    
    public LinkedList<String> getNpacSuccessReplyFlag()
    {
        return npacSuccessReplyUrlList;
    }
    
    private void setNpacFailureReplyFlag( String npacFailureReplyUrl )
    {
        this.npacFailureReplyUrlList.add(npacFailureReplyUrl);
    }
    
    public LinkedList<String> getNpacFailureReplyFlag()
    {
        return npacFailureReplyUrlList;
    }
    
    private void setSvQueryReplyFlag( String svQueryReplyUrl )
    {
        this.svQueryReplyUrlList.add(svQueryReplyUrl);
    }
    
    public LinkedList<String> getSvQueryReplyFlag()
    {
        return svQueryReplyUrlList;
    }
    
    private void setNpbQueryReplyFlag( String npbQueryReplyUrl )
    {
        this.npbQueryReplyUrlList.add(npbQueryReplyUrl);
    }
    
    public LinkedList<String> getNpbQueryReplyFlag()
    {
        return npbQueryReplyUrlList;
    }
    
    private void setSpidQueryReplyFlag( String spidQueryReplyUrl )
    {
        this.spidQueryReplyUrlList.add(spidQueryReplyUrl);
    }
    
    public LinkedList<String> getSpidQueryReplyFlag()
    {
        return spidQueryReplyUrlList;
    }
    
    private void setNpaNxxQueryReplyFlag( String npaNxxQueryReplyUrl )
    {
        this.npaNxxQueryReplyUrlList.add(npaNxxQueryReplyUrl);
    }
    
    public LinkedList<String> getNpaNxxQueryReplyFlag()
    {
        return npaNxxQueryReplyUrlList;
    }
    
    private void setNpaNxxXQueryReplyFlag( String npaNxxXQueryReplyUrl )
    {
        this.npaNxxXQueryReplyUrlList.add(npaNxxXQueryReplyUrl);
    }
    
    public LinkedList<String> getNpaNxxXQueryReplyFlag()
    {
        return npaNxxXQueryReplyUrlList;
    }
    
    private void setLrnQueryReplyFlag( String lrnQueryReplyUrl )
    {
        this.lrnQueryReplyUrlList.add(lrnQueryReplyUrl);
    }
    
    public LinkedList<String> getLrnQueryReplyFlag()
    {
        return lrnQueryReplyUrlList;
    }
    
    private void setAuditQueryReplyFlag( String auditQueryReplyUrl )
    {
        this.auditQueryReplyUrlList.add(auditQueryReplyUrl);
    }
    
    public LinkedList<String> getAuditQueryReplyFlag()
    {
        return auditQueryReplyUrlList;
    }
    
    private void setNpacDownNotifFlag( String npacDownNotifUrl )
    {
        this.npacDownNotifUrlList.add(npacDownNotifUrl);
    }
    
    public LinkedList<String> getNpacDownNotifFlag()
    {
        return npacDownNotifUrlList;
    }
    
    private void setOrderQueryReplyFlag( String orderQueryReplyUrl )
    {
        this.orderQueryReplyUrlList.add(orderQueryReplyUrl);
    }
    
    public LinkedList<String> getOrderQueryReplyFlag()
    {
        return orderQueryReplyUrlList;
    }
    
    /*
	 * A single private instance of this class will be created to assist in cache flushing.
 	 */
	private EndPointSupportFlag(){
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG )){
			
			Debug.log(Debug.SYSTEM_CONFIG, className + " : Initializing...");
		}
			
		try
        {
            CacheManager.getRegistrar().register( this );
                       
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );
        }
	}
	
    //Used exclusively for cache flushing.
    private static EndPointSupportFlag flush = new EndPointSupportFlag( );
    
   
}
