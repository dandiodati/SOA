package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.NodeList;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NFConstants;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class GetLsrTransaction extends MessageProcessorBase{
	
	/*
	 * Property indicating the output location for PON.
	 */
	public static final String OUTPUT_PON_LOC = "OUTPUT_PON_LOC";
	
	/*
	 * Property indicating the output location for DUEDATE.
	 */
	public static final String OUTPUT_DUEDATE_LOC = "OUTPUT_DUEDATE_LOC";
	
	/*
	 * Property indicating the output location for DUEDATE.
	 */
	public static final String DB_PROPFILE_PATH = "DB_PROPFILE_PATH";
	
	/*
	 * Property indicating the output location for DUEDATE.
	 */
	public static final String CUSTID_LOC = "CUSTID_LOC";
	
	
	//private variables
	private Connection dbCon = null;
	
	private String outputPon;
	private String outputDuedate;
	private String seaDBInfoPropPath;
	private String TnValue;
	private String ponValue;
	private Timestamp dueDateValue;
	private String customerId;
	private String custidValue;
	
	private final String lsrStoredProcedure = "{ call Get_Latest_LSR_Transaction(?, ?, ?, ?) }";
	
	private static boolean isDbPoolInt = false;
	/**
	 * Initializes this adapter with persistent properties
	 * 
	 * @param key
	 *            Property-key to use for locating initialization properties.
	 * 
	 * @param type
	 *            Property-type to use for locating initialization properties.
	 * 
	 * @exception ProcessingException
	 *                when initialization is not successful.
	 */
	public void initialize(String key, String type) throws ProcessingException {

		super.initialize(key, type);
        
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "GetLsrTransaction Initialing...");
		}

		StringBuffer sbuff = new StringBuffer();

		outputPon = getRequiredPropertyValue(OUTPUT_PON_LOC, sbuff);
		outputDuedate = getRequiredPropertyValue(OUTPUT_DUEDATE_LOC, sbuff);
		seaDBInfoPropPath = getRequiredPropertyValue(DB_PROPFILE_PATH, sbuff);
		customerId = getRequiredPropertyValue(CUSTID_LOC, sbuff);
		// Throw error if any of the required properties are missing
		if (sbuff.length() > 0) {
			throw new ProcessingException(sbuff.toString());
		}
		
		if(!isDbPoolInt){
			synchronized (GetLsrTransaction.class) {
				if(!isDbPoolInt){
					try{
						//Configure SEA DB Pool
						configureSeaDBPool(seaDBInfoPropPath);						
						//Set the DB pool initialize variable to true.
						//So that other running threads could not initialize SEA DB pool again.
						isDbPoolInt = true;						
					}
					catch(Exception ex){
						Debug.log(Debug.ALL_ERRORS, "Could not initialize SEA DB pool.");
						throw new ProcessingException(ex.getMessage());
					}									
				}
			}
		}
		
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "GetLsrTransaction Initialing done...");
		}
	}

	/**
	 * Extract data values from the context/input, and use them to
	 * insert a row into the configured database table.
	 *
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	public NVPair[] process(MessageProcessorContext mpContext,
			MessageObject inputObject) throws MessageException,
			ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;
		
		if (inputObject == null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "GetLsrTransaction:" + " input obj is Null");
			
			return null;
		}
		
		try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			custidValue = getValue(customerId, mpContext, inputObject);
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "CustomerID value is ["+custidValue+"]");
			
			//get the Tn from input message
			NodeList node = inputObject.getDOM().getElementsByTagName(SOAConstants.TN_NODE);
        	TnValue = node.item(0).getAttributes().item(0).getTextContent();
        	
        	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "PORTINGTN value is ["+TnValue+"]");
        	
        	//Get the Latest LSR Transaction from SEA DB.
        	getLSRData();

        	//set the PON value in context
			if(ponValue == null){
				ponValue = "null";
			}
			set(outputPon, mpContext, inputObject, ponValue);
			
			if(dueDateValue == null){
				//set the null string DueDate value in context
				set(outputDuedate, mpContext, inputObject, "null");
				if(Debug.isLevelEnabled(Debug.MSG_STATUS))
					Debug.log(Debug.MSG_STATUS, "" + "Due Date set in context is [null]");
			}
			else{				
				//set the actual DueDate value in context
				set(outputDuedate, mpContext, inputObject, dueDateValue.toString());
				if(Debug.isLevelEnabled(Debug.MSG_STATUS))
					Debug.log(Debug.MSG_STATUS, "" + "Due Date set in context is ["+dueDateValue.toString()+"]");
			}
			
		}catch (FrameworkException e) {
			String errMsg = "ERROR: GetLsrTransaction: processing failed with error: " + e.getMessage();
			
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, errMsg);
			
			// Re-throw the exception to the driver.
			if (e instanceof MessageException) {
				
				throw (MessageException) e;
				
			} else {
				
				throw new ProcessingException(errMsg);
				
			}
		}catch(Exception e){
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "Error occure while processing GetLsrTransaction processor.");
			
			// Re-throw the exception to the driver.
			if (e instanceof MessageException) {
				
				throw (MessageException) e;
				
			} else {
				
				throw new ProcessingException(e.getMessage());
				
			}
			
		}finally
		{
			ThreadMonitor.stop(tmti);
		}	
		
		return (formatNVPair(inputObject));
		
	}
	/**
	 * This method call the store procedure of SEA Database
	 * to fetch the latest LSR transaction records (PON and DUEDATE) from SEA_LSR_TRANS table.
	 */
	private void getLSRData() throws ProcessingException, MessageException {
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Inside the GetLsrTransaction.getLSRData method");
		
		CallableStatement cstmt = null;
		
		try{
			DBConnectionPool.setThreadSpecificPoolKey(SOAConstants.SEA_POOL_ID);
			
			//acquire the connection and call stored procedure.
			dbCon = DBConnectionPool.getInstance(SOAConstants.SEA_POOL_ID).acquireConnection();
			
			cstmt = dbCon.prepareCall(lsrStoredProcedure);			
			
			//setting input parameters
			cstmt.setString(1, TnValue);
			cstmt.setString(2, custidValue);
			
			//registering output parameters
			cstmt.registerOutParameter(3, Types.VARCHAR);
			cstmt.registerOutParameter(4, Types.TIMESTAMP);
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "executing stored procedure...");
			
			cstmt.execute();
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "Data retreiving from stored procedure...");
			
			ponValue = cstmt.getString(3); //PON
			dueDateValue = cstmt.getTimestamp(4); //DueDate	
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "Fetched following PON and DUEDATE from LSR receive record: \n" +
						"PON is ["+ponValue+"]" +
						"DUEDATE is ["+dueDateValue+"] \n");
		}
		catch(SQLException sqlex){
			if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) ){
				  Debug.log(Debug.ALL_ERRORS," SQL Exception:" + sqlex.getMessage());
			}
			throw new ProcessingException(sqlex);
		}
		
		catch(ResourceException re){
			if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) ){
				  Debug.log(Debug.ALL_ERRORS," SQL Exception: Could not acquire DB connection" + re.getMessage());
			}
			throw new ProcessingException(re);
		}
		catch(Exception e){
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "Could not fetch the latest LSR transaction from sea database.");
			throw new ProcessingException(e);
		}
		finally{
			try {
				// if statement not closed
				if (cstmt != null) {
					// close the statement
					cstmt.close();
					
					cstmt = null;
				}
			}catch (SQLException sqle) {
				
				if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) )
					Debug.log(Debug.ALL_ERRORS, DBInterface.getSQLErrorMessage(sqle));
			}
			try {
				// if resultSet not closed
				if (dbCon != null) {
					// close the resultSet
					DBConnectionPool.getInstance(SOAConstants.SEA_POOL_ID)
						.releaseConnection(dbCon);
				}
			} catch (ResourceException sqle) {
				
				if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) )
					Debug.log(Debug.ALL_ERRORS, "Error Occured while releasing the sea " +
						"db connection"+sqle.getMessage());
			}
			DBConnectionPool.setThreadSpecificPoolKey(NFConstants.NF_DEFAULT_RESOURCE);
		}
	}
	
	/**
	 * This method reads the configure property file and add the SEA DB pool in connection pools.
	 * @param propFileName
	 */
	private void configureSeaDBPool( String propFileName ) throws ProcessingException
	{
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Inside the GetLsrTransaction.configureSeaDBPool method");
		
		Properties props = new Properties();
		
		try {			
			FileUtils.loadProperties( props, propFileName );
			
			DriverManager.registerDriver((Driver) Class.forName(props.getProperty(SOAConstants
					.DBDRIVER)).newInstance());
			
			if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
				
				Debug.log(Debug.MSG_STATUS,"SEADBNAME=["+props.getProperty(SOAConstants.SEA_DBNAME)+"]");
				Debug.log(Debug.MSG_STATUS,"SEADBUSER=["+props.getProperty(SOAConstants.SEA_DBUSER)+"]");				
			}
			
			DBConnectionPool.addPoolConfiguration
				(SOAConstants.SEA_POOL_ID, props.getProperty(SOAConstants.SEA_DBNAME), props.getProperty(SOAConstants.SEA_DBUSER),
						props.getProperty(SOAConstants.SEA_DBPASSWORD));
			
		} catch (FrameworkException fe) {
			
			if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "Could not load properties from [" + propFileName + "] ");
			throw new ProcessingException(fe.getMessage());
			
		} catch (SQLException sqle) {			
			
			if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "ERROR:" + sqle.toString());
			throw new ProcessingException(sqle.getMessage());
			
		} 
		catch(Exception ex){
			if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "ERROR:" + ex.toString());
			throw new ProcessingException(ex.getMessage());
		}
	}
	
	/**
	 * This method tokenizes the input string and return an object for exsisting
	 * value in context or messageobject.
	 * 
	 * @param locations as a string
	 * @return object
	 * @exception ProcessingException Thrown if processing fails.
	 * @exception MessageException Thrown if message is bad.
	 */
	private String getValue(String locations, MessageProcessorContext mpContext,
            MessageObject inputObject) throws MessageException,
			ProcessingException {
		StringTokenizer st = new StringTokenizer(locations,
				DBMessageProcessorBase.SEPARATOR);

		String tok = null;

		while (st.hasMoreTokens()) {
			tok = st.nextToken();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(Debug.MSG_STATUS, "Checking location [" + tok
						+ "] for value...");
			}

			if (exists(tok, mpContext, inputObject)) {
				return ((String) get(tok, mpContext, inputObject));
			}
		}
		return null;
	}
	
}
