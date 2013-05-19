/**
 * The purpose of this message processor is to delete the record (if exists) from
 * the SOA_SV_FAILEDSPLIST table and then insert the record into the 
 * SOA_SV_FAILEDSPLIST table. This component will ignore null value's column.
 
 * @author Ravi Madan Sharma
 * @version 1.1
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 
 
 *@see import com.nightfire.common.ProcessingException;
 *@see import com.nightfire.framework.db.DBInterface;
 *@see import com.nightfire.framework.db.DBLOBUtils;
 *@see import com.nightfire.framework.db.DatabaseException;
 *@see import com.nightfire.framework.message.MessageException;
 *@see import com.nightfire.framework.resource.ResourceException;
 *@see import com.nightfire.framework.util.DateUtils;
 *@see import com.nightfire.framework.util.FrameworkException;
 *@see import com.nightfire.framework.util.StringUtils;
 *@see import com.nightfire.framework.db.PersistentProperty;
 *@see import com.nightfire.framework.util.Debug;
 *@see import com.nightfire.framework.util.NVPair;
 *@see import com.nightfire.spi.common.driver.Converter;
 *@see import com.nightfire.spi.common.driver.MessageObject;
 *@see import com.nightfire.spi.common.driver.MessageProcessorContext;
 *@see import com.nightfire.spi.common.driver.MessageProcessorBase;
 *@see import com.nightfire.spi.neustar_soa.utils.SOAConstants;
 *@see import com.nightfire.spi.neustar_soa.utils.SOAUtility;
 *@see import com.nightfire.framework.db.DBConnectionPool;
 */

/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ravi.M			07/07/2004			Created
	2 			Ravi.M			09/07/2004			Modified for adding  
													failedSPListFlag.
	3			Ravi.M			07/29/2004			Formal review comments 
													incorporated.		
 	4			VRameshChimata	05/03/2007			Review comments incorporated.	
	
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DBLOBUtils;
import com.nightfire.framework.db.DatabaseException;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.DateUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.Converter;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
import com.nightfire.framework.db.DBConnectionPool;

public class FailedSPLogger extends MessageProcessorBase {

	/**
	 * Variable contains value of Table name
	 */
	private String tableName = null;

	/**
	 * Variable contains value of FailedSPListFlag.
	 */
	private String failedSPListFlag = null;

	/**
	 * Variable contains value of Seperator
	 */
	private String separator = null;

	/**
	 * Variable contains value of transaction logging
	 */
	private boolean usingContextConnection = true;

	/**
	 * Variable contains list of ColumnDatas which contains 
	 * all configured column data(Column type , Column Name ...)
	 */
	private List columns = null;

	/**
	 * Variable contains sql where clause
	 */
	private String sqlWhereStmt = null;

	/**
	 * The value of start ID.
	 */
	private String startIDValue = null;

	/**
	 * The value of end ID.
	 */
	private String endIDValue = null;

	/**
	 * Variable contains error buffer string
	 */
	private StringBuffer errorBuffer = null;

	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;
	
	Lock lock = new ReentrantLock();

	/**
	 * Constructor.
	 */
	public FailedSPLogger() {

		Debug.log(
			Debug.OBJECT_LIFECYCLE,
			"Creating FailedSPLogger message-processor.");

		columns = new ArrayList();

	}

	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param  key  Property-key to use for locating initialization properties.
	 * @param  type Property-type to use for locating initialization 
	 * 				properties.
	 *
	 * @exception ProcessingException when initialization fails
	 */
	public void initialize(String key, String type)
		throws ProcessingException {
	
	
		// Call base class method to load the properties.
		super.initialize(key, type);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "FailedSPLogger: Initializing...");
		}

		errorBuffer = new StringBuffer();

		
		//Get configuration properties specific to this processor.
		startIDValue = getRequiredPropertyValue(
							SOAConstants.START_ID_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log(
			Debug.SYSTEM_CONFIG,
			"startID value is [" + startIDValue + "].");
		}
		
		endIDValue = getPropertyValue( SOAConstants.END_ID_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "endID value is [" + endIDValue + "].");
		}

		failedSPListFlag =
			getRequiredPropertyValue( 
							SOAConstants.FAILEDSPLIST_FLAG_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log(
			Debug.SYSTEM_CONFIG,
			"failedSPListFlag value is [" + failedSPListFlag + "].");
		}
		tableName = getRequiredPropertyValue(SOAConstants.TABLE_NAME_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log(
			Debug.SYSTEM_CONFIG,
			"Database table to update is [" + tableName + "].");
		}

		String strTemp = getPropertyValue(SOAConstants.TRANSACTIONAL_LOGGING_PROP);

		// if the TRANSACTIONAL_LOGGING property configured
		if (StringUtils.hasValue(strTemp)) {
			try {

				usingContextConnection = getBoolean(strTemp);

			} catch (FrameworkException e) {

				errorBuffer.append(
					"Property value for "
						+ SOAConstants.TRANSACTIONAL_LOGGING_PROP
						+ " is invalid. "
						+ e.getMessage()
						+ "\n");

			}
		}

		separator = getPropertyValue(SOAConstants.LOCATION_SEPARATOR_PROP);
		
		// If the separator is not configured use the default one.
		if (!StringUtils.hasValue(separator)) {

			separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;

		}

		sqlWhereStmt = getRequiredPropertyValue(SOAConstants.SQL_WHERE_STATEMENT_PROP);

		try {
			
			// populates the list for columndata.
			populateColumnData();

		} catch (ProcessingException e) {

			Debug.log(Debug.ALL_ERRORS, e.toString());
			
			throw new ProcessingException( 
						" Could not populate column data : "+e.toString());

		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "FailedSPLogger: Initialization done.");
		}
	}

	/**
	 * Extract data values from the context/input, and use them to
	 * delete and insert  row(s) in SOA_SV_FAILEDSPLIST table.
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

		// If the MessageObject is not available.
		if (inputObject == null)
		{
			return null;
		}
		
		this.mpContext = mpContext;
		
		this.inputObject = inputObject;		
		
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "FailedSPLogger: processing ... ");
		}

		Connection dbConn = null;
		
		// Getting the Start ID value.
		if (startIDValue != null) {
			startIDValue =
				 getValue(startIDValue);
		}
		
		// Getting the End ID value.
		if (endIDValue != null) {

			endIDValue =  getValue(endIDValue);

		} 
		// Getting the failedSPListFlag value.
		if (failedSPListFlag != null) {
			failedSPListFlag =
				 getValue(failedSPListFlag);
		}
		
		// Initilization value for start ID.
		long startID;

		// Initilization value for end ID
		long endID;
		
		// Initilization value for failedSPFlag.
		int failedSPFlag = -1;

		try {

			startID = Long.parseLong(startIDValue);

			// If End ID value is available.
			if (endIDValue != null)
			{
				endID = Long.parseLong(endIDValue);
			}
			else
			{
				endID = startID;
			}
			

			failedSPFlag = Integer.parseInt( failedSPListFlag );

		} catch (NumberFormatException nbrfex) {

			throw new MessageException(
				"Invalid start ID, end ID or failedSPFlag: " + nbrfex);

		}
		
		try {
			
			// Get a database connection from the appropriate location - based
			// on transaction characteristics.
			if (usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"Database logging is "
					  + "transactional, so getting connection from context.");
				}

				dbConn = mpContext.getDBConnection();
			} else {

				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"Database logging is not "
						+ "transactional, so getting connection "
						+ "from NightFire pool.");
				}

			   //Get a database connection from connection pool
			   dbConn = DBConnectionPool.getInstance(true).acquireConnection();
			}
			// If connection is not available.
			if(dbConn == null)
			{

				throw new ProcessingException("DBConnection is not available");
					
			}

		} catch ( FrameworkException e) {
			String errMsg =
				"ERROR: FailedSPLogger: Attempt to get database "
					+ "connection failed with error: "
					+ e.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// Re-throw the exception to the driver.
			if(e instanceof ResourceException )
			{
				
				throw new MessageException (e.getMessage());
				
			}else
			{
				
				throw new ProcessingException( errMsg );
				
			}
		}


		try {
			
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] processing SVID request" );
			// Process for all the SVID
			for (; startID <= endID; startID++) {

				if (startID != -1) {
					// Set the ID in context
					super.set(
					SOAConstants.ID_LOCATION,
						mpContext,
						inputObject,
						String.valueOf(startID));

					//Reset the values in cases where we're logging again.
					resetColumnValues();

					//Extract the column values from the arguments.
					extractMessageData(mpContext, inputObject);
					
					// If the notification contains FailedSPList, delete the record 
					// from SOA_SV_FAILEDSPLIST for the SVID and then insert into the same table
					if (failedSPFlag == 1) {
						try
						{
							lock.lock();
							deleteRecord(dbConn);
							insert(dbConn);
						}finally
						{
							lock.unlock();
						}

					} 
					// If the notification doesn't contain FailedSPList, delete the record
					// from SOA_SV_FAILEDSPLIST for the SVID
					else {						
						deleteRecord(dbConn);

					}

				}

			}

			// If the configuration indicates that this SQL operation
			// isn't part of the overall driver
			// transaction, commit the changes now.
			if (!usingContextConnection) {
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"Committing data inserted by FailedSPLogger to database.");
				}
			
				DBConnectionPool.getInstance(true).commit(dbConn);

			}

		} catch (FrameworkException e) {
			String errMsg =
				"ERROR: FailedSPLogger: Attempt to log to database "
					+ "failed with error: "
					+ e.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// If the configuration indicates that this SQL operation isn't 
			// part of the overall driver transaction, roll back 
			// any changes now.
			if (!usingContextConnection) {
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
				   Debug.MSG_STATUS,
				   "Rolling-back any database changes due to FailedSPLogger.");
				}

				try {
					//perform the rollback operation
					DBConnectionPool.getInstance(true).rollback(dbConn);

				} catch (ResourceException re) {

					Debug.log(Debug.ALL_ERRORS, re.getMessage());

				}
			}

			// Re-throw the exception to the driver.
			if (e instanceof MessageException)
			{
				throw (MessageException) e;
			}
			else
			{
				throw new ProcessingException(errMsg);
			}
				

		} finally {
			
			ThreadMonitor.stop( tmti );

			// If the configuration indicates that this SQL operation isn't
			// part of the overall driver transaction, return the connection
			// previously acquired back to the resource pool.
			if (!usingContextConnection ) {
				try {
					
					DBConnectionPool.getInstance(true).releaseConnection(
						dbConn);
						
					dbConn = null;

				} catch (ResourceException e) {

					Debug.log(Debug.ALL_ERRORS, e.toString());

				}
			}

		}

		// Always return input value to provide pass-through semantics.
		return (formatNVPair(inputObject));
	}

	/**
	 * This method populates the list for columndata
	 *
	 * @exception ProcessingException when initialization fails
	 **/
	private void populateColumnData() throws ProcessingException {

		// Variable for holding optional value.
		String optional = null;

		// Variable columnName value of column name.
		String columnName = null;

		// Variable colType value of column datatype.
		String colType = null;

		// Variable dateFormat value of dateformat.
		String dateFormat = null;

		// Variable location value of location.
		String location = null;

		// Variable defaultValue value of default value.
		String defaultValue = null;
		
		ColumnData cd = null;

		// Reading the property values of configured fields.
		for (int Ix = 0; true; Ix++) {
			optional =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.OPTIONAL_PREFIX_PROP,
						Ix));

			// If we can't find next optional property we're done
			if (!StringUtils.hasValue(optional)) {

				break;

			}
			// Getting the column name.
			columnName =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.COLUMN_NAME_PREFIX_PROP,
						Ix));
			// Getting the column type.
			colType =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.COLUMN_TYPE_PREFIX_PROP,
						Ix));
			// Getting the date format.
			dateFormat =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.DATE_FORMAT_PREFIX_PROP,
						Ix));
			// Getting the location.
			location =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.LOCATION_PREFIX_PROP,
						Ix));
			// Getting the default value.
			defaultValue =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.DEFAULT_PREFIX_PROP,
						Ix));

			try {

				// Create a new column data object and add it to the list.
				cd =
					new ColumnData(
						columnName,
						colType,
						dateFormat,
						location,
						defaultValue,
						optional);

				Debug.log(Debug.SYSTEM_CONFIG, cd.describe());

				// Adding column data object to list.
				columns.add(cd);

			} catch (FrameworkException e) {

				throw new ProcessingException(
					"ERROR: Could not create column"
						+ " data description:\n"
						+ e.toString());

			}
		}

		// Get the size of list. 
		int columnsCount = columns.size();

		if (sqlWhereStmt == null) // no WHERE clause to be appended
			{
			// If any of the required properties are absent, 
			// indicate error to caller.
			if (errorBuffer.length() > 0) {
				String errMsg = errorBuffer.toString();

				Debug.log(Debug.ALL_ERRORS, errMsg);

				throw new ProcessingException(errMsg);

			}

			return;

		}

		// Loop until all WhereColumn configuration properties have been read 
		int count = -1;

		int index = 0;

		// to find the number of Column values to be filled in WHERE clause.
		// this is counted by number of ? in the string
		while (index != -1) {
			count++;

			index = sqlWhereStmt.indexOf("?", index + 1);

		}
		// Getting the Location, column Type, date Format and default Value.
		for (int Ix = 0; true; Ix++) {
			location =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.WHERE_LOCATION_PREFIX_PROP,
						Ix));

			if (!StringUtils.hasValue(location)) {

				break;

			}

			colType =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.WHERE_COLUMN_TYPE_PREFIX_PROP,
						Ix));

			dateFormat =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.WHERE_DATE_FORMAT_PREFIX_PROP,
						Ix));

			defaultValue =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.WHERE_DEFAULT_PREFIX_PROP,
						Ix));

			try {
				// Create a new column data object and add it to the list.
				cd =
					new ColumnData(
						null,
						colType,
						dateFormat,
						location,
						defaultValue,
						"FALSE");

				Debug.log(Debug.SYSTEM_CONFIG, cd.describe());
				
				// Adding the column data to the list.
				columns.add(cd);

			} catch (FrameworkException e) {

				throw new ProcessingException(
					"ERROR: Could not create "
						+ "column data description:\n"
						+ e.toString());

			}
		}

		int whereColumnsCount = columns.size() - columnsCount;
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log(
					Debug.SYSTEM_CONFIG,
					"Maximum number of columns in where "
						+ "clause ["
						+ whereColumnsCount
						+ "].");
		}
		
		
		if (whereColumnsCount != count) {

			throw new ProcessingException(
				"ERROR: Unequal no. of fields in "
					+ "the whereStatement and whereAttributes");

		}

		
		//	If any of the required properties are absent, 
		// indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);

		}

	}

	/**
	* Delete the records from SOA_SV_FAILEDSPLIST table using the given connection.
	*
	* @param  dbConn  Connection the database connection to perform 
	* 				  the SQL delete operation against.
	* 
	* @exception  ProcessingException  thrown if processing fails.
	*/
	private void deleteRecord(Connection dbConn) throws ProcessingException {

		PreparedStatement pstmt = null;

		try {
									
			// Constructing the SQL DELETE Statement
			StringBuffer sqlStmt = new StringBuffer( "DELETE FROM " );
			
			sqlStmt.append( tableName );

			if (sqlWhereStmt != null)
			{
				sqlStmt.append( " " );
				
				sqlStmt.append( sqlWhereStmt );
			}
				
            if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
			Debug.log(
				Debug.NORMAL_STATUS,
				"Executing DELETE SQL:\n" + sqlStmt.toString());
			}

			// Get a prepared statement for the SQL statement.
			pstmt = dbConn.prepareStatement(sqlStmt.toString());

			// Populate the SQL statement using values obtained from the column 
			// data objects.
			if (sqlWhereStmt != null) {

				// Invoke populateDeleteSqlStatement for populating the Delete statement.
				populateDeleteSqlStatement(pstmt);

			}

			// Execute the SQL DELETE operation.
			pstmt.execute();

			// Commit the database
			dbConn.commit();

		} catch (SQLException sqle) {

			throw new ProcessingException(
				"ERROR: Could not delete row into database table ["
					+ tableName
					+ "]:\n"
					+ DBInterface.getSQLErrorMessage(sqle));

		} catch (ProcessingException e) {

			throw new ProcessingException(
				"ERROR: Could not delete row into "
					+ "database table ["
					+ tableName
					+ "]:\n"
					+ e.toString());

		} finally {
			if (pstmt != null) {
				try {

					pstmt.close();
					pstmt = null;

				} catch (SQLException sqle) {

					Debug.log(
						Debug.ALL_ERRORS,
						DBInterface.getSQLErrorMessage(sqle));

				}
			}
		}
	} // deleteRecord end


	/**
	 * Insert a single row into the database table using the given
	 * connection.
	 *
	 * @param  dbConn  The database connection to perform the SQL
	 * INSERT operation against.
	 *
	 * @exception  MessageException  Thrown on data errors.
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private void insert(Connection dbConn)
		throws MessageException, ProcessingException {

		// Make sure that at least one column value will be inserted.
		//		   validate( );

		PreparedStatement pstmt = null;

		try {
			// Create the SQL statement using the column data objects.
			String sqlStmt = constructInsertSqlStatement();
            
			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
		 	  Debug.log(Debug.NORMAL_STATUS, "Executing Insert SQL:\n" + sqlStmt);
			}

			// Get a prepared statement for the SQL statement.
			pstmt = dbConn.prepareStatement(sqlStmt);

			// Populate the SQL statement using values obtained from the
			// column data objects.
			populateInsertSqlStatement(pstmt);

			// Execute the SQL INSERT operation.
			pstmt.executeUpdate();

		} catch (SQLException sqle) {

			throw new ProcessingException(
				"ERROR: Could not insert row into database table ["
					+ tableName
					+ "]:\n"
					+ DBInterface.getSQLErrorMessage(sqle));

		} catch (FrameworkException e) {

			throw new ProcessingException(
				"ERROR: Could not insert row into database table ["
					+ tableName
					+ "]:\n"
					+ e.toString());

		} finally {
			if (pstmt != null) {
				try {

					pstmt.close();
					pstmt = null;

				} catch (SQLException sqle) {

					Debug.log(
						Debug.ALL_ERRORS,
						DBInterface.getSQLErrorMessage(sqle));

				}
			}
		}
	}


	/**
	 * Construct the SQL INSERT statement from the column data.
	 *
	 * @return SQL INSERT statement.
	 *
	 * @exception  DatabaseException  Thrown on data errors.
	 */
	private String constructInsertSqlStatement() throws DatabaseException {
        
		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		  Debug.log(Debug.DB_DATA, "Constructing SQL INSERT statement ...");
		}

		StringBuffer sb = new StringBuffer( );
        
		StringBuffer sbColumnName = new StringBuffer( );
        
		StringBuffer sbValue = new StringBuffer( );

		sb.append("INSERT INTO ");

		sb.append(tableName);

		sb.append(" ( ");

		boolean firstOne = true;

		Iterator iter = columns.iterator();
		
		ColumnData cd = null;

		// Append the names of columns with non-null values.
		while (iter.hasNext()) {
			
			 cd = (ColumnData) iter.next();

			// Skip columns with null values since null values aren't inserted.
			if (SOAUtility.isNull(cd.value) || SOAUtility.isNull(cd.columnName))
			{
				continue;
				
			}

			if (firstOne)
			{
				firstOne = false;
			}				
			else{
				
				sbColumnName.append( ", " );
				
				sbValue.append( ", " );
				
			}
				

			sbColumnName.append( cd.columnName );
			
			// If the current column is a date column, and the configuration
			// indicates that the current system date should be used for
			// the value, place it in the SQL statement now.
			if ( StringUtils.hasValue( cd.columnType )
				 && cd.columnType.equalsIgnoreCase( SOAConstants.COLUMN_TYPE_DATE )
				 && (cd.value instanceof String)
				 && ((String)( cd.value ) ).equalsIgnoreCase( SOAConstants.SYSDATE ) )
			{
				sbValue.append( SOAConstants.SYSDATE );
			}
			else
			{
				sbValue.append( "?" );
			}
		}

		sb.append( sbColumnName.toString() );
		
		sb.append( " ) VALUES ( " );
        
		sb.append( sbValue.toString() );
        
		sb.append( " )" );

        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		 Debug.log(Debug.DB_DATA, "Done constructing SQL INSERT statement.");
		}

		return ( sb.toString() );
	}


	/**
	 * Populate the SQL DELETE statement from the column data.
	 *
	 * @param  pstmt  The prepared statement object to populate.
	 *
	 * @exception Exception  thrown if population fails.
	 */
	private void populateDeleteSqlStatement(PreparedStatement pstmt)
		throws ProcessingException {

		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		  Debug.log(Debug.DB_DATA, "Populating SQL delete statement ...");
		}
		
		try
		{
				
			int Ix = 1; // First value slot in prepared statement.
	
			// Get iterator to iterate over column.
			Iterator iter = columns.iterator();
			
			ColumnData cd = null;
	
			// Iterate the column data  
			while (iter.hasNext()) {
				cd = (ColumnData) iter.next();
	            if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					Debug.log(
						Debug.DB_DATA,
						"Populating prepared-statement slot ["
							+ Ix
							+ "] with column data ."
							+ cd.describe());
				}
	
				if (SOAUtility.isNull(cd.value) || !SOAUtility.isNull(cd.columnName)) {
	
					continue;
	
				}
	
				// Default is no column type specified.
				if (!StringUtils.hasValue(cd.columnType)) {
	
					pstmt.setObject(Ix, cd.value);
	
				} else if (
					cd.columnType.equalsIgnoreCase(
						SOAConstants.COLUMN_TYPE_DATE)) {
	
					String val = (String) (cd.value);
	
					if (val.equalsIgnoreCase(SOAConstants.SYSDATE)) {
	
						//get current date to set a value for this column
						pstmt.setTimestamp(
							Ix,
							new java.sql.Timestamp(
								DateUtils.getCurrentTimeValue()));
	
						// Advance to next value slot in prepared statement.
						Ix++;
	
						continue;
	
					}
	
					// If we're not updating the current system date, the 
					// caller must have provided an actual date value, 
					// which we must now parse.
					if (!StringUtils.hasValue(cd.dateFormat)) {
	
						throw new FrameworkException(
							"ERROR: Configuration "
								+ "for date column does not specify date format.");
	
					}
	
					SimpleDateFormat sdf = new SimpleDateFormat(cd.dateFormat);
	
					java.util.Date d = sdf.parse(val);
	
					// Setting the positional parameter
					pstmt.setTimestamp(Ix, new java.sql.Timestamp(d.getTime()));
	
				} else {
	
					throw new FrameworkException(
						"ERROR: Invalid column-type "
							+ "property value ["
							+ cd.columnType
							+ "] given in "
							+ "configuration.");
	
				}
	
				// Advance to next value slot in prepared statement.
				Ix++;
	
			}
			
		} catch( Exception exception )
		{
			throw new ProcessingException( 
							"ERROR:  could not populate sql statement ."
							+ exception.toString() );

		}
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
	    	Debug.log(Debug.DB_DATA, "Done populating SQL DELETE statement.");
		}

	}

	/**
	 * Populate the SQL INSERT/UPDATE statement from the column data.
	 *
	 * @param  pstmt  The prepared statement object to populate.
	 * @param  isInsert  The prepared statement is select or insert.
	 *
	 * @exception Exception  thrown if population fails.
	 */
	private void populateInsertSqlStatement(PreparedStatement pstmt)
		throws ProcessingException {

        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		  Debug.log(Debug.DB_DATA, "Populating SQL INSERT statement ...");
		}
		
		try
		{
		
			int Ix = 1; // First value slot in prepared statement.
	
			// Get iterator to iterate over column list.
			Iterator iter = columns.iterator();
			
			ColumnData cd = null;
	
			// Iterate over list of Column Data's
			while (iter.hasNext()) {
				 cd = (ColumnData) iter.next();
	
				if (Debug.isLevelEnabled(Debug.MSG_DATA))
				{
					Debug.log(
									Debug.MSG_DATA,
									"Populating SQL statement for:\n" + cd.describe());
	
				}
				
				// Skip columns with null values since null values aren't inserted
				// or updated and also skip columns for update if update is false.
				if (SOAUtility.isNull(cd.value) || SOAUtility.isNull(cd.columnName)) {

	                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					  Debug.log(Debug.MSG_STATUS, "Skipping null column value.");
					}
	
					continue;
	
				}
	            if( Debug.isLevelEnabled(Debug.DB_DATA) ){
				Debug.log(
					Debug.DB_DATA,
					"Populating prepared-statement slot [" + Ix + "].");
				}
	
				// Default is no column type specified.
				if (!StringUtils.hasValue(cd.columnType)) {
	                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
						Debug.log(
							Debug.MSG_DATA,
							"Value for column ["
								+ cd.columnName
								+ "] is ["
								+ cd.value.toString()
								+ "].");
					}
	
					pstmt.setObject(Ix, cd.value);
	
				} 
				// If column type is DATE.
				else if ( 
					
					cd.columnType.equalsIgnoreCase(
						SOAConstants.COLUMN_TYPE_DATE)) {
	
					String val = (String) (cd.value);
	                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
						Debug.log(
							Debug.MSG_DATA,
							"Value for date column ["
								+ cd.columnName
								+ "] is ["
								+ val
								+ "].");
					}
	
					// SYSDATE is already in the text of the SQL statement used 
					// to create the prepared statement, so there's nothing more 
					// to do here.
					if (val.equalsIgnoreCase(SOAConstants.SYSDATE)) {
	                    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							Debug.log(
								Debug.MSG_STATUS,
								"Skipping date population "
									+ "since SYSDATE is already in SQL string.");
						}
	
						continue;
	
					}
	
					// If we're not inserting the current system date, 
					// the caller must have provided an actual date value,
					// which we must now parse.
					if (!StringUtils.hasValue(cd.dateFormat)) {
	
						throw new FrameworkException(
							"ERROR: Configuration for "
								+ " date column ["
								+ cd.columnName
								+ "] does not specify date format.");
	
					}
	
					SimpleDateFormat sdf = new SimpleDateFormat(cd.dateFormat);
	
					java.util.Date d = sdf.parse(val);
	
					pstmt.setTimestamp(Ix, new java.sql.Timestamp(d.getTime()));
					
				} else if (cd.columnType.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_TEXT_BLOB)) {
	
					if (Debug.isLevelEnabled(Debug.MSG_DATA))
						Debug.log(
							Debug.MSG_DATA,
							"Querying column [" + cd.describe() + "].");
	
					DBLOBUtils.setCLOB(pstmt, Ix, Converter.getString(cd.value));
	
				} else if (
					cd.columnType.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_BINARY_BLOB)) {
	
					byte[] bytes = null;
	
					if (cd.value instanceof byte[])
					{
					
						bytes = (byte[]) cd.value;
					}
					else if (cd.value instanceof String)
					{
					
						bytes = ((String) (cd.value)).getBytes();
					}
					else if (cd.value instanceof Document)
					{
					
						bytes = Converter.getString(cd.value).getBytes();
					}
					else {
	
						throw new FrameworkException(
							"ERROR: Value for database table ["
								+ tableName
								+ "], column ["
								+ cd.columnName
								+ "] of type ["
								+ cd.value.getClass().getName()
								+ "] can't be converted to byte stream.");
	
					}
	
					if (Debug.isLevelEnabled(Debug.MSG_DATA))
					{
					
						Debug.log(
							Debug.MSG_DATA,
							"Querying column [" + cd.describe() + "].");
					}
					
					DBLOBUtils.setBLOB(pstmt, Ix, bytes);
	
				} else {
	
					throw new FrameworkException(
						"ERROR: Invalid column-type property value ["
							+ cd.columnType
							+ "] given in configuration.");
	
				}
	
				// Advance to next value slot in prepared statement.
				Ix++;
			}
			
		}catch( Exception exception )
		{
			throw new ProcessingException( 
							"ERROR:  could not populate sql statement ."
							+ exception.toString() );

		}
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		Debug.log(
			Debug.DB_DATA,
			"Done populating SQL INSERT/UPDATE statement.");
		}
	}

	/**
	 * Reset the column values in the list.
	 */
	private void resetColumnValues() {

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "Resetting column values ...");
		}

		Iterator iter = columns.iterator();

		ColumnData cd = null;

		// While columns are available ...
		while (iter.hasNext()) {
			cd = (ColumnData) iter.next();

			cd.value = null;
		}
	}

	/**
	 * Extract data for each column from the input message/context.
	 *
	 * @param context The message context.
	 * @param inputObject The input object.
	 *
	 * @exception MessageException  thrown if required value can't be found.
	 * @exception ProcessingException thrown if any other processing 
	 * 			  error occurs.
	 */
	private void extractMessageData(
		MessageProcessorContext context,
		MessageObject inputObject)
		throws ProcessingException, MessageException {

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "Extracting message data ...");
		}
		//Get iterator to iterate over column list.
		Iterator iter = columns.iterator();

		ColumnData cd = null;

		// While columns are available ...
		while (iter.hasNext()) {
			cd = (ColumnData) iter.next();

			// If location was given, try to get a value from it.
			if (StringUtils.hasValue(cd.location)) {
				
				// Location contains one or more alternative locations that 
				// could contain the column's value.
				StringTokenizer st =
					new StringTokenizer(cd.location, separator);

				String loc = null;

				// While location alternatives are available.
				while (st.hasMoreTokens()) {
					// Extract a location alternative.
					loc = st.nextToken().trim();

					// If the value of location indicates that the input 
					// message-object's entire value should be used as 
					// the column's value, extract it.
					if (loc.equalsIgnoreCase(SOAConstants.PROCESSOR_INPUT)) {
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log(
								Debug.MSG_DATA,
								"Using message-object's"
									+ " contents as the column's value.");
						}

						cd.value = inputObject.get();

						break;

					}

					// Attempt to get the value from the context 
					// or input object.
					if (exists(loc, context, inputObject)) {

						cd.value = get(loc, context, inputObject);

					}

					// If we found a value, we're done with this column.
					if (!SOAUtility.isNull(cd.value)) {
                        if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log(
								Debug.MSG_DATA,
								"Found value for column ["
									+ cd.describe()
									+ "] at location ["
									+ loc
									+ "].");
						}

						break;

					}
				}
			}

			// If no value can be obtained ...
			if (SOAUtility.isNull(cd.value)) {
				// If the value is required ...
				if (cd.optional == false) {
					// Signal error to caller.
					throw new ProcessingException(
						"ERROR: Could not "
							+ "locate required value for column ["
							+ cd.describe()
							+ "], database table ["
							+ tableName
							+ "].");

				} else // No optional value is available, so putting 'NULL'.
					{

					cd.value = null;

				}
			}
		}

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "Done extracting message data .");
		}

	}

	/**
	 * This method tokenizes the input string and return an
	 * object for exsisting value in context or messageobject.
	 *
	 * @param  locations as a string
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  object
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	
	 */
	protected String getValue(String locations)
		throws MessageException, ProcessingException {
			
		// Tokenizing the locations	
		StringTokenizer st = new StringTokenizer(locations, SEPARATOR);
		
		String tok =null;
		
		// While tokens are available.
		while (st.hasMoreTokens()) {
			 tok = st.nextToken();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"Checking location [" + tok + "] for value...");
			}

			if (exists(tok, mpContext, inputObject))
			{
				return (String)(get(tok, mpContext, inputObject));
			}	
		}

		return null;
	}

	
	/**
	 * Class ColumnData is used to encapsulate a description of a single column
	 * and its associated value.
	 */
	private static class ColumnData {
		//	Property used to store Column Name for a column
		public final String columnName;

		//	Property used to store Column Type for a column
		public final String columnType;

		//	Property used to store date format 
		public final String dateFormat;

		//	Property used to store location for a column's value
		public final String location;

		// Property used to store default value
		public final String defaultValue;

		//	Property used to store optional flag
		public final boolean optional;

		//	Property used to store value for a column
		public Object value = null;

		public ColumnData(
			String columnName,
			String columnType,
			String dateFormat,
			String location,
			String defaultValue,
			String optional)
			throws FrameworkException {
				
			this.columnName = columnName;
			
			this.columnType = columnType;
			
			this.dateFormat = dateFormat;
			
			this.location = location;
			
			this.defaultValue = defaultValue;
			
			this.optional = StringUtils.getBoolean(optional);
		}

		/**
		 * This method used to describe a column
		 */
		public String describe() {
			StringBuffer sb = new StringBuffer();

			sb.append("Column description: Name [");
			sb.append(columnName);
			// If column is having columntype.
			if (StringUtils.hasValue(columnType)) {
				sb.append("], type [");
				sb.append(columnType);
			}
			// If column is having date format.
			if (StringUtils.hasValue(dateFormat)) {
				sb.append("], date-format [");
				sb.append(dateFormat);
			}
			// If column is having location 
			if (StringUtils.hasValue(location)) {
				sb.append("], location [");
				sb.append(location);
			}

			sb.append("], optional [");
			sb.append(optional);
			// If column is having default value.
			if (StringUtils.hasValue(defaultValue)) {
				sb.append("], default [");
				sb.append(defaultValue);
			}
			// If column is having value.
			if (value != null) {
				sb.append("], value [");
				sb.append(value);
			}

			sb.append("].");

			return (sb.toString());
		}
	}

	//	--------------------------For Testing--------------------------------//

	public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "all");

		props.put("LOG_FILE", "d:\\SOAlog.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		if (args.length != 3) {
			Debug.log(
				Debug.ALL_ERRORS,
				"SOADBUpdate: USAGE:  "
				  + " jdbc:oracle:thin:@192.168.1.240:1521:soa ravim ravim ");
			return;

		}
		try {

			DBInterface.initialize(args[0], args[1], args[2]);

		} catch (DatabaseException e) {
			Debug.log(
				null,
				Debug.MAPPING_ERROR,
				": " + "Database initialization failure: " + e.getMessage());
		}

		FailedSPLogger failedSPLogger = new FailedSPLogger();

		try {
			failedSPLogger.initialize("FULL_NEUSTAR", "FailedSPLogger");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("startID", "1000");

			mob.set("endID", "1001");

			mob.set("failedSPFlag", "1");
			
			mob.set("OBJECTTYPE", "OBJECTTYPE");
			
			mob.set("ID", "ID");
			
			mob.set("FAILEDSPLIST", "FAILEDSPLIST");
			
			mob.set("LOCATION_0", "SV");
			
			mob.set("LOCATION_1", "1000");
			
			mob.set("LOCATION_2", "ramesh");

			failedSPLogger.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {
			System.out.println(pex.getMessage());
		} catch (MessageException mex) {
			System.out.println(mex.getMessage());
		}

	} //end of main method

}