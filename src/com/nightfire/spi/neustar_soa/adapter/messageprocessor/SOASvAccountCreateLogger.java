package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import org.w3c.dom.Document;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * 
 * This is message processor in Validate Gateway chain and responsible to logs
 * input message into SOA_ACCOUNT table submitted by Customer after checking
 * wheather the SPID and ACCOUNTID has any existing combination.
 * 
 */
public class SOASvAccountCreateLogger extends MessageProcessorBase {

	public static final String SUBREQUESTTYPE_NODE = "SUBREQUESTTYPE_LOC";
	public static final String SPID_VAL = "SPID_LOC";
	public static final String ACCOUNTNAME_VAL = "ACCOUNT_NAME_LOC";
	public static final String CUSOTMERID_VAL = "CUSTOMERID_LOC";
	public static final String CREATEDBY_VAL = "CREATEDBY_LOC";
	public static final String CREATEDDATE_VAL = "CREATEDDATE_LOC";
	public static final String ACCOUNTID_VAL = "ACCOUNT_ID_LOC";
	public static final String SUBDOMAINID_VAL = "SUBDOMAINID_LOC";

	public static final String SvCreateRequest = "SvCreateRequest";
	public static final String SvReleaseRequest = "SvReleaseRequest";
	public static final String SvReleaseInConflictRequest = "SvReleaseInConflictRequest";

	private boolean usingContextConnection = true;
	/**
	 * This variable contains MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;

	private MessageObject inputObject = null;
	private String subRequestNode;
	private String spidProp = null;
	private String customerIdLoc = null;
	private String createdByLoc = null;
	private String subDomainIdLOC = null;
	
	
	private static final String CLASSNAME = "SOASvAccountCreateLogger";

	private final static String SOA_REQUEST_ACCOUNT_TABLE = "SOA_ACCOUNT";
	
	public static final String ACCOUNT_INSERT = "insert into SOA_ACCOUNT"
			+ "(SPID, ACCOUNTNAME, CUSTOMERID, CREATEDBY, CREATEDDATE, ACCOUNTID,SUBDOMAINID)"
			+ " values (?,?,?,?,?,?,?)";

	/**
	 * Initializes this object via its persistent properties.
	 * 
	 * @param key
	 *            Property-key to use for locating initialization properties.
	 * @param type
	 *            Property-type to use for locating initialization properties.
	 * 
	 * @exception ProcessingException
	 *                when initialization fails
	 * @throws
	 */
	public void initialize(String key, String type) throws ProcessingException {
		
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG, CLASSNAME + ", Initializing...");
		}
		
		super.initialize(key, type);		
		
		StringBuffer sbuff = new StringBuffer();
		
		spidProp = getRequiredPropertyValue(SPID_VAL, sbuff);
		customerIdLoc = getRequiredPropertyValue(CUSOTMERID_VAL, sbuff);
		createdByLoc = getRequiredPropertyValue(CREATEDBY_VAL, sbuff);
		subDomainIdLOC = getPropertyValue(SUBDOMAINID_VAL);
		subRequestNode = getRequiredPropertyValue(SUBREQUESTTYPE_NODE, sbuff);

		// get the transactional logging from configuration property.
		String strTemp = getPropertyValue(SOAConstants.TRANSACTIONAL_LOGGING_PROP);
		
		if (StringUtils.hasValue(strTemp)) {
			try {
				usingContextConnection = getBoolean(strTemp);
			} catch (FrameworkException e) {
				sbuff.append("Property value for "
						+ SOAConstants.TRANSACTIONAL_LOGGING_PROP
						+ " is invalid. " + e.getMessage() + "\n");
			}
		}
		
		// Throw error if any of the required properties are missing
		if (sbuff.length() > 0) {
			
			String errMsg = sbuff.toString( );
			
			Debug.log( Debug.ALL_ERRORS, CLASSNAME + " Error while initializing SOASvAccountCreateLogger :"+errMsg );
			
			throw new ProcessingException(sbuff.toString());
		}
		
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, CLASSNAME + " Initializing done...");
		}
	}

	/**
	 * Extract data values from the context/input, and use them to insert a
	 * row(s) into the SOA_ACCOUNT database table.
	 * 
	 * @param mpContext
	 *            The context
	 * @param inputObject
	 *            Input message to process.
	 * 
	 * @return The given input, or null.
	 * 
	 * @exception ProcessingException
	 *                Thrown if processing fails.
	 * @exception MessageException
	 *                Thrown if message is bad.
	 */

	public NVPair[] process(MessageProcessorContext mpcontext,
			MessageObject inputobject) throws MessageException,
			ProcessingException {
		
		if ( inputobject == null )
            return null;
		
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,CLASSNAME + " Processing...");
		}
		
		ThreadMonitor.ThreadInfo tmti = null;

		this.mpContext = mpcontext;
		this.inputObject = inputobject;
		
		Connection dbConn = null;
		
		try{
			tmti = ThreadMonitor.start("Message-processor [" + getName()
					+ "] started processing the request");

			String subRequestValue = getValue(subRequestNode, mpContext, inputObject);
			String spid = getValue(spidProp, mpContext, inputObject);
			String subDomainIdValue = getValue(subDomainIdLOC, mpContext, inputObject);
			String customerIdValue = getValue(customerIdLoc, mpContext, inputObject);
			String createdByValue = getValue(createdByLoc, mpContext, inputObject);
			
			ArrayList passtnList = (ArrayList) CustomerContext.getInstance().get("successTNList");										

			if ((subRequestValue.equals(SvCreateRequest) || subRequestValue.equals(SvReleaseRequest) || subRequestValue.equals(SvReleaseInConflictRequest)) )
			{	
				
				if(passtnList != null && passtnList.size() >0 && passtnList.get(0) != null)							
				{
					String accountIdValue =null;				
					String accountNameValue = null;
	
					if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
						Debug.log(Debug.NORMAL_STATUS,CLASSNAME + 
								" Request type value is: "								
										+ subRequestValue);
					}
					
					Document doc = inputObject.getDOM();
					XMLMessageParser inputParser = new XMLMessageParser(doc);
	
					String accountID = SOAConstants.REQUEST_BODY_PATH + "."
							+ subRequestValue + "." + SOAConstants.ACCOUNT_ID_NODE;
					String accountName = SOAConstants.REQUEST_BODY_PATH + "."
							+ subRequestValue + "." + SOAConstants.ACCOUNT_NAME_NODE;
					
					if (inputParser.exists(accountID)) {
						accountIdValue = inputParser.getValue(accountID);
					}
					if (inputParser.exists(accountName)) {
						accountNameValue = inputParser.getValue(accountName);
					}
	
					if(StringUtils.hasValue(accountIdValue) && StringUtils.hasValue(accountNameValue)){
						// Get a database connection from the appropriate location
						// based on transaction characteristics.
						if ( usingContextConnection )
			            {
			                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			                    Debug.log( Debug.NORMAL_STATUS, CLASSNAME + " Database logging is transactional, " +
			                    		"so getting connection from context." );
	
			                dbConn = mpContext.getDBConnection( );
			            }
			            else
			            {
			                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			                    Debug.log( Debug.NORMAL_STATUS, CLASSNAME + " Database logging is not transactional, " +
			                    		"so getting connection from NightFire pool." );
	
			                dbConn = DBConnectionPool.getInstance( true ).acquireConnection( );
			            }
						
						if (dbConn == null) {
							throw new ProcessingException("DB connection is not available");
						}
						
						//Check the existence of Account present in Request					
						String accountExist = (String) CustomerContext.getInstance().get("AccountExist");
						
						if(StringUtils.hasValue(accountExist)){						
							if(accountExist.equalsIgnoreCase("NO")){
								//If Account not present in SOA_ACCOUNT table then insert it as new entry.
								insertSoaAccount(dbConn, spid, accountNameValue, customerIdValue, createdByValue, accountIdValue, subDomainIdValue);
							}else{
								if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				                    Debug.log( Debug.NORMAL_STATUS, CLASSNAME + " Incoming AccountId is already present in SOA_ACCOUNT table. " +
				                    		"AccountId is["+accountIdValue+"]" );
							}									
						}					
			            // If the configuration indicates that this SQL operation isn't part of the overall driver
			            // transaction, commit the changes now.
			            if ( !usingContextConnection )
			            {
			                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			                    Debug.log( Debug.NORMAL_STATUS, CLASSNAME + " Committing data inserted to database." );
	
			                DBConnectionPool.getInstance( true ).commit( dbConn );
			            }
			            
					}else{
						if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
		                    Debug.log( Debug.NORMAL_STATUS, CLASSNAME + " Both AccountId and AccountName does not exist in request XML. " +
		                    		"Hence skipping the Account creation process.");
					}
				}

			} 
			else {
				if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                    Debug.log( Debug.NORMAL_STATUS, CLASSNAME + " Invalid request type["+subRequestValue+"]. Hence skipping the Account creation process.");
			}
		}
		catch(Exception e){
			if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
				Debug.log(Debug.ALL_ERRORS, CLASSNAME + ", Error : " + e.getMessage());
			}
			// If the configuration indicates that this SQL operation isn't part of the overall driver
	        // transaction, roll back any changes now.
	        if ( !usingContextConnection )
	        {
	            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	                Debug.log( Debug.MSG_STATUS, "Rolling-back any database changes due to logger." );
	            
	            try
	            {
	                DBConnectionPool.getInstance( true ).rollback( dbConn );
	            }
	            catch( ResourceException re )
	            {
	                Debug.log( Debug.ALL_ERRORS, CLASSNAME + re.getMessage() );
	            }
	        }
		}
		finally
        {
			ThreadMonitor.stop( tmti );
			
			// If the configuration indicates that this SQL operation isn't part of the overall driver
            // transaction, return the connection previously acquired back to the resource pool.
            if ( !usingContextConnection && (dbConn != null) )
            {
                try
                {
                    DBConnectionPool.getInstance(true).releaseConnection( dbConn );
                    dbConn = null;
                }
                catch ( Exception e )
                {
                    Debug.log( Debug.ALL_ERRORS, CLASSNAME + e.toString() );
                }
            }
        }		

		return (formatNVPair(inputObject));

	}
	/**
	 * insert message in SOA_ACCOUNT table
	 * 
	 * @param dbConn
	 * @throws SQLException
	 * @throws FrameworkException
	 */
	private void insertSoaAccount(Connection dbConn, String spid, String accountNameValue, String customerIdValue, String createdByValue,String accountIdValue,
			String subDomainIdValue) throws ProcessingException {

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
			Debug.log(Debug.NORMAL_STATUS, CLASSNAME + ", insertSoaAccount() method.");
		}
		PreparedStatement pstmt = null;
		
		try {
			pstmt = dbConn.prepareStatement(ACCOUNT_INSERT);

			Date datetime = new Date();
			pstmt.setString(1, spid);
			pstmt.setString(2, accountNameValue);
			pstmt.setString(3, customerIdValue);
			pstmt.setString(4, createdByValue);
			pstmt.setTimestamp(5,new Timestamp(datetime.getTime()));
			pstmt.setString(6, accountIdValue);
			pstmt.setString(7, subDomainIdValue);

			int count = pstmt.executeUpdate();
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS, CLASSNAME + "Finished Executing SQL.. Inserted [" + count + "] row into table ["
                       + SOA_REQUEST_ACCOUNT_TABLE + "]" );
			
		} 
		catch ( SQLException sqle )
        {
             Debug.log(Debug.ALL_ERRORS, CLASSNAME + "ERROR: Could not insert row into database table [" + SOA_REQUEST_ACCOUNT_TABLE
            		 + "]:\n" + DBInterface.getSQLErrorMessage(sqle) );
        }
        catch ( Exception e )
        {
            Debug.log(Debug.ALL_ERRORS, CLASSNAME + "ERROR: Could not insert row into database table [" + SOA_REQUEST_ACCOUNT_TABLE
            		+ "]:\n" + e.toString() );
        }
        finally
        {
            if ( pstmt != null )
            {
                try
                {
                    pstmt.close( );
                }
                catch ( SQLException sqle )
                {
                    Debug.log( Debug.ALL_ERRORS, DBInterface.getSQLErrorMessage(sqle) );
                }
            }
        }
	}
	
	/**
	 * This method tokenizes the input string and return an object for exsisting
	 * value in context or messageobject.
	 * 
	 * @param locations
	 *            as a string
	 * @param mpContext
	 *            The context
	 * @param inputObject
	 *            Input message to process.
	 * 
	 * @return object
	 * 
	 * @exception ProcessingException
	 *                Thrown if processing fails.
	 * @exception MessageException
	 *                Thrown if message is bad.
	 */
	private String getValue(String locations,
			MessageProcessorContext mpContext, MessageObject inputObject)
			throws MessageException, ProcessingException {
		StringTokenizer st = new StringTokenizer(locations,
				DBMessageProcessorBase.SEPARATOR);

		String tok = null;

		while (st.hasMoreTokens()) {
			tok = st.nextToken();
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS, CLASSNAME + " Checking location [" + tok
						+ "] for value...");
			}

			if (exists(tok, mpContext, inputObject)) {
				return ((String) get(tok, mpContext, inputObject));
			}
		}
		return null;
	}

}