package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Properties;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.adapter.util.DBMetaData;
import com.nightfire.adapter.util.TableMetaData;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DBLOBUtils;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentSequence;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.Converter;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class SOAQueueLogger extends DBMessageProcessorBase {
	
	/**
	 * The value of usingContextConnection
	 */	
	private boolean usingContextConnection = true;
	
	/**
	 * The value of the MESSAGE_LOCATION property, indicating where the
	 * input message is.
	 */
	private String messageLoc;
	
	/**
	 * The value of the REGIONID_LOCATION property, indicating where the
	 * input message is.
	 */
	private String regionIdLoc;
	
	/**
	 * The value of the SPID_LOCATION property, indicating where the
	 * input message is.
	 */
	private String spidLoc;
	
	private String inputMessage;
	
	private String regionIdValue;
	
	private String spidValue;
	
	private String delay;
	  
	/**
	 * The name of the property indicating the location of the
	 * message to be queued.
	 */
	public static final String MESSAGE_LOC_PROP = "MESSAGE_LOCATION";

	/**
	 * The name of the property indicating the location of the
	 * regionID to be queued.
	 */
	public static final String REGIONID_LOC_PROP = "REGIONID_LOCATION";
	
	/**
	 * The name of the property indicating the location of the
	 * SPID to be queued.
	 */
	public static final String SPID_LOC_PROP = "SPID_LOCATION";
	
	/**
	 * The name of the property indicating the location of the
	 * DELAY to be queued.
	 */
	public static final String DELAY = "DELAY";
	
	/**
	 * The name of the table to insert the message
	 */
	public static final String TABLE_NAME = "SOA_QUEUE";
	
	public static final String SOA_QUEUE_SEQUENCE_NAME = "SOAQUEUEID";

	
	public static final String SOA_QUEUE_INSERT = " INSERT INTO SOA_QUEUE (ID, CUSTOMERID, MESSAGETYPE, PRIORITY," +
			" ARRIVALTIME, ERRORCOUNT, DEFERREDCOUNT, STATUS, MESSAGE, REGIONID, SPID) VALUES (?,?,?,?,?,?,?,?," +
			" ?,?,?)";
	
	//Reply constants, check in NPAC XML for setting priority of messages  
	private final static String RESPONSE_REPLY = "Reply";
	private final static String RESPONSE_QUERY_REPLY = "QueryReply";
	private final static String RECOVERY_REPLY = "RecoveryReply";
	
	
	/**
	 * Constructor.
	 */
	public SOAQueueLogger() {
		
		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log( Debug.OBJECT_LIFECYCLE,
				"Creating SOA Queue logger message-processor.");
		}
		
	}

	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param  key   Property-key to use for locating initialization properties.
	 * @param  type  Property-type to use for locating initialization properties.
	 *
	 * @exception ProcessingException when initialization fails
	 */
	public void initialize(String key, String type)
		throws ProcessingException {
			
		// Call base class method to load the properties.
		super.initialize(key, type);

		// Get configuration properties specific to this processor.
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "SOAQueueLogger: Initializing...");
		}
		
		StringBuffer errorBuffer = new StringBuffer();
			
		messageLoc = getRequiredPropertyValue(MESSAGE_LOC_PROP,	errorBuffer);
		
        regionIdLoc = getPropertyValue(REGIONID_LOC_PROP);
        
        spidLoc = getPropertyValue(SPID_LOC_PROP);
        
        delay = getPropertyValue(DELAY);
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
        	Debug.log(Debug.MSG_STATUS, "Configurable value for delay in ArrivalTime is (in Sec.)["+delay+"]");
        }
        
		String strTemp = getPropertyValue(SOAConstants.TRANSACTIONAL_LOGGING_PROP);
		
		if (StringUtils.hasValue(strTemp)) {
		
			try {
		
				usingContextConnection = getBoolean(strTemp);
				
			} catch (MessageException e) {
				
				errorBuffer.append(
					"Property value for "
						+ SOAConstants.TRANSACTIONAL_LOGGING_PROP
						+ " is invalid. "
						+ e.getMessage()
						+ "\n");
						
			}
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Logger will participate in overall driver transaction? ["
					+ usingContextConnection
					+ "].");
		}

		// If any of the required properties are absent, indicate error to 
		// caller.
		if (errorBuffer.length() > 0) {
			
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "SOAQueueLogger: Initialization done.");
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
	public NVPair[] process(
		MessageProcessorContext mpContext,
		MessageObject inputObject)
		throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;
		
		Connection dbConn = null;
		
		
		
		if (inputObject == null)
			return null;
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "SOAQueueLogger: processing ... ");
		}
		
		try {
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			inputMessage = getString(messageLoc, mpContext, inputObject);
			
			if ( exists( spidLoc, mpContext, inputObject ) ){
				
				spidValue = getString(spidLoc, mpContext, inputObject);
			}
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				
				Debug.log(Debug.MSG_STATUS, "Getting SPID value from context is ["+spidValue+"]");
				
			}
			if ( exists( regionIdLoc, mpContext, inputObject ) ){
				
				regionIdValue = getString(regionIdLoc, mpContext, inputObject);
			}
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				
				Debug.log(Debug.MSG_STATUS, "Getting RegionID value from context is ["+regionIdValue+"]");
				
			}
			
			// Get a database connection from the appropriate location - based 
			// on transaction characteristics.
			if (usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"SOA Queue logging is transactional, so getting connection " +
						"from context.");
				}

				dbConn = mpContext.getDBConnection();
				
			} else {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"SOA Queue logging is not transactional, so getting" +
											" connection from NightFire pool.");
				}

				dbConn = DBConnectionPool.getInstance(true).acquireConnection();
			}
			
			if (dbConn == null)
			{
				// Throw the exception to the driver.
				throw new ProcessingException( "DB connection is not available" );
			}
			
			// Write the data to the database.
			insert(dbConn);

			// If the configuration indicates that this SQL operation isn't part of 
			// the overall driver transaction, commit the changes now.
			if (!usingContextConnection) {
			    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Committing data inserted by soa-queue-logger to database.");
				}

				DBConnectionPool.getInstance(true).commit(dbConn);
				
			}
			
		} catch(ResourceException re){
			
			Debug.log(Debug.ALL_ERRORS, "ERROR: " +
							"SOADatabaseLogger: Attempt to " +
								"log to database failed with error: " + re );

			// If the configuration indicates that this SQL operation isn't part 
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Rolling-back any database changes due to soa-queue-logger.");
				}

				try {
					
					DBConnectionPool.getInstance(true).rollback(dbConn);
					
				} catch (ResourceException ree) {
					
					Debug.log(Debug.ALL_ERRORS, ree.getMessage());
				}
			}	
			
		} catch (Exception e) {
			
			String errMsg =
				"ERROR: SOAQueueLogger: Attempt to log to database failed with " +
				"error: "	+ e.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// If the configuration indicates that this SQL operation isn't part 
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Rolling-back any database changes due to soa-queue-logger.");
				}

				try {
					DBConnectionPool.getInstance(true).rollback(dbConn);
				} catch (ResourceException re) {
					Debug.log(Debug.ALL_ERRORS, re.getMessage());
				}
			}

			// Re-throw the exception to the driver.
			if (e instanceof MessageException){
			
				throw (MessageException) e;
				
			}
			else{	
			
				throw new ProcessingException(errMsg);
				
			}
			
		} finally {
			
			ThreadMonitor.stop(tmti);
			// If the configuration indicates that this SQL operation isn't 
			// part of the overall driver transaction, return the connection 
			// previously acquired back to the resource pool.
			if (!usingContextConnection && (dbConn != null)) {
			
				try {
			
					DBConnectionPool.getInstance(true).releaseConnection(
						dbConn);
						
				} catch (ResourceException re) {
					
					Debug.log(Debug.ALL_ERRORS, re.toString());
					
				}
			}
		}

		// Always return input value to provide pass-through semantics.
		return (formatNVPair(inputObject));
	}

	/**
	 * Insert a single row into the SOA_QUEUE table using the given 
	 * connection.
	 *
	 * @param  dbConn  The database connection to perform the SQL INSERT 
	 * operation against.
	 *
	 * @exception  MessageException  Thrown on data errors.
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private void insert(Connection dbConn)
		throws MessageException, ProcessingException {
			
		PreparedStatement pstmt = null;

		try {
            
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			
				Debug.log(
					Debug.MSG_STATUS,
					CustomerContext.getInstance().describe());
					
			}
			
			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
			  Debug.log(Debug.NORMAL_STATUS, "Executing SQL to insert message in SOA_QUEUE " +
			  		"table:\n" + SOA_QUEUE_INSERT);
			}

			// Get a prepared statement for the SQL statement.
			pstmt = dbConn.prepareStatement(SOA_QUEUE_INSERT);

			// Populate the SQL statement using values obtained from the column 
			// data objects.
			populateSqlStatement(pstmt);
			
			// Execute the SQL INSERT operation.
			int count = pstmt.executeUpdate();
            
			if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
				Debug.log(
					Debug.NORMAL_STATUS,
					"Successfully inserted ["
						+ count
						+ "] row into table ["
						+ TABLE_NAME
						+ "]");
			}
					
		} catch ( ParseException pe ){
			if( Debug.isLevelEnabled(Debug.DB_ERROR) ){
			Debug.log(Debug.DB_ERROR,"ERROR: " +
					"SimpleDateFormate Parseing failed in pupulateSqlStmt");
			}
					
		}catch ( SQLException sqle ) {
			
			throw new ProcessingException(
				"ERROR: Could not insert row into database table ["
					+ TABLE_NAME
					+ "]:\n"
					+ DBInterface.getSQLErrorMessage(sqle));
					
		}catch( DatabaseException de){
			if( Debug.isLevelEnabled(Debug.DB_ERROR) ){
				Debug.log(Debug.DB_ERROR,
								"ERROR: Could not insert row into database table ["
									+ TABLE_NAME
									+ "]:\n"
									+ de.getCause());
			}
								
		}catch (Exception e) {
			
			throw new ProcessingException(
				"ERROR: Could not insert row into database table ["
					+ TABLE_NAME
					+ "]:\n"
					+ e.toString());
					
		} finally {
			
			if ( pstmt != null )
		  	{
			  try
			  {

				  pstmt.close( );
				  pstmt = null;

			  }
			  catch ( SQLException sqle )
			  {

				  Debug.log( Debug.ALL_ERRORS,
							  DBInterface.getSQLErrorMessage( sqle ) );

			  }
		  	}
		}
	}

	
	/**
	 * Populate the SQL INSERT statement from the column data.
	 *
	 * @param  pstmt  The prepared statement object to populate.
	 *
	 * @exception FrameworkException, SQLException,ParseException
	 * 			  thrown if population fails.
	 */
	private void populateSqlStatement(
		PreparedStatement pstmt )
		throws FrameworkException, SQLException, ParseException {
		
		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){	
		 Debug.log(Debug.DB_DATA, "Populating SQL INSERT statement ...");
		}
		
		Integer idVal = new Integer( PersistentSequence.getNextSequenceValue( SOA_QUEUE_SEQUENCE_NAME, false ) );
		//ID
		pstmt.setInt(1, idVal);

		//CUSTOMERID
		pstmt.setString(2, "DEFAULT");
		
		//MESSAGESUBTYPE
		pstmt.setString(3, "soa");
		
		
		
		if(inputMessage.indexOf(RESPONSE_QUERY_REPLY) <= -1 && inputMessage.indexOf(RECOVERY_REPLY) <= -1  && inputMessage.indexOf(RESPONSE_REPLY) > -1)
	    {	
			//Priority
			pstmt.setString(4, "3");
			
	    }else{
	    	//Priority 
	    	pstmt.setString(4, "7");
	    }
		
		//Arrival Time
		java.sql.Date arrivalTime = new java.sql.Date( System.currentTimeMillis() );
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
			Debug.log(Debug.MSG_STATUS, "Current System Time in miliseconds ["+arrivalTime.getTime()+"]");
		}
		
	    if(inputMessage.indexOf(RESPONSE_QUERY_REPLY) > -1 || inputMessage.indexOf(RECOVERY_REPLY) > -1  
				|| inputMessage.indexOf(RESPONSE_REPLY) > -1)
	    {	
	    	pstmt.setTimestamp(5, new java.sql.Timestamp(arrivalTime.getTime()) );
			
	    }else{
	    	long arrivalT = arrivalTime.getTime();
	    	if(delay != null && StringUtils.isDigits(delay))
	    		arrivalT = arrivalT + Integer.parseInt(delay)*1000;
	    	
	    	if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				Debug.log(Debug.MSG_STATUS, "Logged ArrivalTime in milisecnods for Notifications" +
						"["+arrivalT+"]");
	    	}
	    	pstmt.setTimestamp(5, new java.sql.Timestamp(arrivalT) );
	    }
	    
		//Error Count
		pstmt.setInt(6, 0);
		
		//Deferred count
		pstmt.setInt(7, 0);
		
		//Status
		pstmt.setString(8, "awaitingDelivery");
		
		//Message
		DBLOBUtils.setCLOB(pstmt, 9, Converter.getString(inputMessage));
		
		//RegionID
		if(regionIdValue != null && StringUtils.isDigits(regionIdValue))
			pstmt.setInt(10, Integer.parseInt(regionIdValue));
		else
			pstmt.setNull(10, java.sql.Types.VARCHAR);
		
		//SPID
		if(spidValue != null)
			pstmt.setString(11, spidValue);
		else
			pstmt.setNull(11, java.sql.Types.VARCHAR);
		
		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log(Debug.DB_DATA, "Done Populating SQL INSERT Statement.");
		}
	}

	//	--------------------------For Testing---------------------------------//

	public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "E:\\logmap.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		if (args.length != 3) {

			Debug.log(
				Debug.ALL_ERRORS,
				"SOAQueueLogger: USAGE:  "
					+ " jdbc:oracle:thin:@192.168.148.61:1521:orcl soadb2 soadb2 ");

			return;

		}
		try {

			DBInterface.initialize(args[0], args[1], args[2]);

		} catch (DatabaseException e) {

			Debug.log(Debug.MAPPING_ERROR,
				": " + "Database initialization failure: " + e.getMessage());

		}

		SOAQueueLogger logger = new SOAQueueLogger();

		try {
			logger.initialize("FULL_NEUSTAR_SOA", "SOAQueueLogger");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("Name", "Talati");

			mob.set("Id", "7,1");
			
			mob.set("Address", "abcaaaaaaaaaaadefgh");
			
			mob.set("Area", "aaaaaaa");
			
			mob.set("City", "aaaaaeee");
			
			mob.set("Pin", "72,,,,");
			
			logger.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());

		}
	} //end of main method
}
