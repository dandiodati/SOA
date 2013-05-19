/**
 * This is a generic message-processor to log the messages into the database
 * table by validating the META_DATA_CHECK for the sql datatype
 * VARCHAR2/NUMBER.
 * 
 * @author Jigar Talati
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.framework.util.StringUtils;
 * @see com.nightfire.spi.common.driver.MessageObject;
 * @see com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see com.nightfire.framework.util.FrameworkException;
 * @see com.nightfire.framework.resource.ResourceException;
 * @see com.nightfire.framework.db.DBConnectionPool;
 * @see com.nightfire.framework.db.DatabaseException;
 * @see com.nightfire.framework.db.PersistentProperty;
 * @see com.nightfire.framework.db.DBLOBUtils;
 * @see com.nightfire.framework.db.SQLBuilder;
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.framework.db.PersistentSequence;
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants;
 * @see com.nightfire.spi.neustar_soa.utils.SOAUtility;
 * 
 */
 
/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Jigar Talati	09/27/2004			Created
	2			Jigar Talati	09/28/2004			Review Comments Incorporated
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.Document;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.adapter.util.DBMetaData;
import com.nightfire.adapter.util.TableMetaData;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DBLOBUtils;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.SQLBuilder;
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
import com.nightfire.spi.neustar_soa.utils.SOAUtility;


public class SOADatabaseLogger extends DBMessageProcessorBase {
	
	/**
	 * The boolean value of validMetaData
	 */
	boolean validMetaData = false;
	
	/**
	 * The value of colName
	 */
	private String colName = null;
		
	/**
	 * The value of colType
	 */	
	private String colType = null;
	
	/**
	 * The value of tableName
	 */	
	private String tableName = null;
	
	/**
	 * The value of separator
	 */	
	private String separator = null;
	
	/**
	 * The value of usingContextConnection
	 */	
	private boolean usingContextConnection = true;
	
	/**
	 * The value of useCustomerId
	 */	
	private boolean useCustomerId = true;
	
	/**
	 * The value of useInterfaceVersion
	 */	
	private boolean useInterfaceVersion = false;
	
	/**
	 * The value of useUserId
	 */	
	private boolean useUserId = false;
	
	/**
	 * The value of columns
	 */	
	private ArrayList columns;
	
	/**
	 * Constructor.
	 */
	public SOADatabaseLogger() {
		
		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log( Debug.OBJECT_LIFECYCLE,
				"Creating SOA database-logger message-processor.");
		}

		columns = new ArrayList();
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
		  Debug.log(Debug.SYSTEM_CONFIG, "SOADatabaseLogger: Initializing...");
		}
		
		// The value of location
		String location = null;

		// The value of defaultValue
		String defaultValue = null;
		
		// The value of optional
		String optional = null;
		
		// The value of metaDataCheck
		String metaDataCheck = null;
		
		// The value of dateFormat
		String dateFormat = null;
		
		// The value of Transactional Logging property
		String strTemp = null;
	
		StringBuffer errorBuffer = new StringBuffer();
			
		tableName = getRequiredPropertyValue(SOAConstants.TABLE_NAME_PROP, 
																errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Database table to log to is [" + tableName + "].");
		}

		strTemp = getPropertyValue(SOAConstants.TRANSACTIONAL_LOGGING_PROP);
		
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

		separator = getPropertyValue(SOAConstants.LOCATION_SEPARATOR_PROP);

		if (!StringUtils.hasValue(separator)) {
			
			separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;
		}	
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Location separator token [" + separator + "].");
		}

		// Loop until all column configuration properties have been read ...
		for (int Ix = 0; true; Ix++) {
			
			colName =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.COLUMN_PREFIX_PROP,
						Ix));

			// If we can't find a column name, we're done.
			if (!StringUtils.hasValue(colName)) {
			
				break;
			}
			
			colType =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.COLUMN_TYPE_PREFIX_PROP,
						Ix));

			dateFormat =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.DATE_FORMAT_PREFIX_PROP,
						Ix));

			location =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.LOCATION_PREFIX_PROP,
						Ix));

			defaultValue =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.DEFAULT_PREFIX_PROP,
						Ix));

			optional =
				getRequiredPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.OPTIONAL_PREFIX_PROP,
						Ix),
					errorBuffer);

			metaDataCheck =
				getRequiredPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.METADATACHECK_PREFIX_PROP,
						Ix),
					errorBuffer);

			try {
				// Create a new column data object and add it to the list.
				ColumnData cd =
					new ColumnData(
						colName,
						colType,
						dateFormat,
						location,
						defaultValue,
						optional,
						metaDataCheck);

				if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
				
					Debug.log(Debug.SYSTEM_CONFIG, cd.describe());
				}
				
				columns.add(cd);
			} catch (FrameworkException fe) {
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log(Debug.SYSTEM_CONFIG,
							"ERROR: Could not create column data description:\n"
							+ fe.toString());
				}
			}
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"Maximum number of columns to insert [" + columns.size() + "].");
		}

		strTemp = getPropertyValue(SOAConstants.USE_CUSTOMER_ID_PROP);

		if (StringUtils.hasValue(strTemp)) {
			try {
				useCustomerId = getBoolean(strTemp);
			} catch (FrameworkException e) {
				errorBuffer.append(
					"Property value for "
						+ SOAConstants.USE_CUSTOMER_ID_PROP
						+ " is invalid. "
						+ e.getMessage()
						+ "\n");
			}
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"To use customer id in SQL statement?[" + useCustomerId + "].");
		}

		// If any of the required properties are absent, indicate error to 
		// caller.
		if (errorBuffer.length() > 0) {
			
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "SOADatabaseLogger: Initialization done.");
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
		  Debug.log(Debug.MSG_STATUS, "SOADatabaseLogger: processing ... ");
		}
		
		// Reset the values in cases where we're logging again.
		resetColumnValues();
		
		try {
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			// Extract the column values from the arguments.
			extractMessageData(mpContext, inputObject);
			
			// Get a database connection from the appropriate location - based 
			// on transaction characteristics.
			if (usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Database logging is transactional, so getting connection " +
						"from context.");
				}

				dbConn = mpContext.getDBConnection();
				
			} else {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Database logging is not transactional, so getting" +
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
						"Committing data inserted by database-logger to database.");
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
						"Rolling-back any database changes due to database-logger.");
				}

				try {
					
					DBConnectionPool.getInstance(true).rollback(dbConn);
					
				} catch (ResourceException ree) {
					
					Debug.log(Debug.ALL_ERRORS, ree.getMessage());
				}
			}	
			
		} catch (Exception e) {
			
			String errMsg =
				"ERROR: SOADatabaseLogger: Attempt to log to database failed with " +
				"error: "	+ e.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			// If the configuration indicates that this SQL operation isn't part 
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Rolling-back any database changes due to database-logger.");
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
	 * Insert a single row into the database table using the given 
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
			
		// Make sure that at least one column value will be inserted.
		validate();

		PreparedStatement pstmt = null;

		try {
            
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			
				Debug.log(
					Debug.MSG_STATUS,
					CustomerContext.getInstance().describe());
					
			}
			
			//Check if userid, interface version need to be logged or not to 
			// the table.
			TableMetaData currTableInfo =
				DBMetaData.getInstance().getTableMetaData(dbConn, tableName);

			if (Debug.isLevelEnabled(Debug.MSG_STATUS)){
			
				Debug.log(Debug.MSG_STATUS, currTableInfo.describe());
				
			}

			//We log to the table only if the CustomerContext has the data and the
			//table has the column.
			//InterfaceVersion check
			if ((currTableInfo.existsInterfaceVersion())
				&& (CustomerContext.getInstance().getInterfaceVersion() != null)){
					
				
				useInterfaceVersion = true;
			}
			
			//UserId check.
			if ((currTableInfo.existsUserId())
				&& (CustomerContext.getInstance().getUserID() != null)){
				
				useUserId = true;
			
			}

			Iterator iter = columns.iterator();

			// Append the names of columns with non-null values.
			while (iter.hasNext()) {

				ColumnData cd = (ColumnData) iter.next();

				// Skip columns with null values since null values aren't 
				// inserted.
				if (SOAUtility.isNull(cd.value)){
	
					continue;
	
				}
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log(Debug.SYSTEM_CONFIG,
							"ColumnName is  ["+cd.columnName+"]");
				}
				
				if (cd.metaDataCheck) {
				    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log(Debug.SYSTEM_CONFIG,
							"Meta Data Check is True for ["+cd.columnName+"]");
					}
						
					validMetaData = existMetaData(cd, dbConn);

				}
				
				if(cd.metaDataCheck && !validMetaData){
					
					iter.remove();
				}

			}
	
			// Create the SQL statement using the column data objects.
			String sqlStmt = constructSqlStatement();
            if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){
			  Debug.log(Debug.NORMAL_STATUS, "Executing SQL:\n" + sqlStmt);
			}

			// Get a prepared statement for the SQL statement.
			pstmt = dbConn.prepareStatement(sqlStmt);

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
						+ tableName
						+ "]");
			}
					
		} catch ( ParseException pe ){
			if( Debug.isLevelEnabled(Debug.DB_ERROR) ){
			Debug.log(Debug.DB_ERROR,"ERROR: " +
					"SimpleDateFormate Parseing failed in pupulateSqlStmt");
			}
					
		}catch(ResourceException re){
			if( Debug.isLevelEnabled(Debug.DB_ERROR) ){	
			  Debug.log( Debug.DB_ERROR, "Failed to validate MetaData " );
			}
	
			throw new ProcessingException( re );
				 			
		} catch ( SQLException sqle ) {
			
			throw new ProcessingException(
				"ERROR: Could not insert row into database table ["
					+ tableName
					+ "]:\n"
					+ DBInterface.getSQLErrorMessage(sqle));
					
		}catch( DatabaseException de){
			if( Debug.isLevelEnabled(Debug.DB_ERROR) ){
				Debug.log(Debug.DB_ERROR,
								"ERROR: Could not insert row into database table ["
									+ tableName
									+ "]:\n"
									+ de.getCause());
			}
								
		}catch (Exception e) {
			
			throw new ProcessingException(
				"ERROR: Could not insert row into database table ["
					+ tableName
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
	 * Construct the SQL INSERT statement from the column data.
	 * 
	 * @return SQL INSERT statement.
	 * 
	 * @throws DatabaseException
	 * 
	 */
	private String constructSqlStatement()
		throws DatabaseException {
		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){	
		 Debug.log(Debug.DB_DATA, "Constructing SQL INSERT statement ...");
		}

		StringBuffer sb = new StringBuffer();
		
		StringBuffer sbColumnName = new StringBuffer();
		
		StringBuffer sbValue = new StringBuffer();
		
		sb.append("INSERT INTO ");
		
		sb.append(tableName);
		
		sb.append(" ( ");

		boolean firstOne = true;

		Iterator iter = columns.iterator();

		// Append the names of columns with non-null values.
		while (iter.hasNext()) {
	
			ColumnData cd = (ColumnData) iter.next();

			// Skip columns with null values since null values aren't 
			// inserted.
			if (SOAUtility.isNull(cd.value)){
			
				continue;
			
			}
			if (firstOne) {
				
				firstOne = false;
			
			}
			else {
			
				sbColumnName.append(", ");
				
				sbValue.append(", ");
			
			}
				
			sbColumnName.append(cd.columnName);
			
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

		if (useCustomerId) {
			
			if ( firstOne )
			{
				firstOne = false;
			}
			else
			{
				sbColumnName.append( ", " );
				
				sbValue.append( ", " );
				
			}
			
			SQLBuilder.insertCustomerID( sbColumnName );
			
			sbValue.append( "?" );
		}

		if (useInterfaceVersion) {
			
			if ( firstOne )
			{
				firstOne = false;
			}
			else
			{
				sbColumnName.append( ", " );
				
				sbValue.append( ", " );
				
			}
			
			SQLBuilder.insertInterfaceVersion( sbColumnName );
			
			sbValue.append( "?" );
		}

		if (useUserId) {
			
			if ( firstOne )
			{
				firstOne = false;
			}
			else
			{
				sbColumnName.append( ", " );
				
				sbValue.append( ", " );
				
			}
			
			SQLBuilder.insertUserID( sbColumnName );
			
			sbValue.append( "?" );
			
		}

		sb.append( sbColumnName.toString() );
		
		sb.append( " ) VALUES ( " );
        
		sb.append( sbValue.toString() );
        
		sb.append( " )" );
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		  Debug.log(Debug.DB_DATA, "Done constructing SQL INSERT statement.");
		}

		return (sb.toString());
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

		int Ix = 1; // First value slot in prepared statement.

		Iterator iter = columns.iterator();
		
			while (iter.hasNext()) {

				ColumnData cd = (ColumnData) iter.next();
	
				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
					Debug.log(
						Debug.NORMAL_STATUS,
						"Populating SQL statement for:\n" + cd.describe());
	
				// Skip columns with null values since null values aren't inserted.
				if (SOAUtility.isNull(cd.value)) {
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					 Debug.log(Debug.MSG_STATUS, "Skipping null column value.");
					}
	
					continue;
				}
	            if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
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
	
				} else if (cd.columnType.equalsIgnoreCase
											(SOAConstants.COLUMN_TYPE_DATE)) {
												
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
	
					// SYSDATE is already in the text of the SQL statement used to
					//  create
					// the prepared statement, so there's nothing more to do here.
					if (val.equalsIgnoreCase(SOAConstants.SYSDATE)) {
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log(
								Debug.MSG_STATUS,
								"Skipping date population since SYSDATE is already in " +
								"SQL string.");
						}
	
						continue;
					}
	
					// If we're not inserting the current system date, the caller 
					// must have provided an actual date value, which we must now 
					// parse.
					if (!StringUtils.hasValue(cd.dateFormat)) {
						throw new FrameworkException(
							"ERROR: Configuration for date column ["
								+ cd.columnName
								+ "] does not specify date format.");
					}
	
					SimpleDateFormat sdf = new SimpleDateFormat(cd.dateFormat);
	
					java.util.Date d = sdf.parse(val);
	
					pstmt.setTimestamp(Ix, new java.sql.Timestamp(d.getTime()));
					
				} else if (cd.columnType.equalsIgnoreCase
										(SOAConstants.COLUMN_TYPE_TEXT_BLOB)) {
					
					if (Debug.isLevelEnabled(Debug.MSG_DATA))
					
						Debug.log(
							Debug.MSG_DATA,
							"Logging column [" + cd.describe() + "].");
	
					DBLOBUtils.setCLOB(pstmt, Ix, Converter.getString(cd.value));
					
				} else if (
				
					cd.columnType.equalsIgnoreCase
									(SOAConstants.COLUMN_TYPE_BINARY_BLOB)) {
						
					byte[] bytes = null;
	
					if (cd.value instanceof byte[]){
					
						bytes = (byte[]) cd.value;
						
					}
					else if (cd.value instanceof String) {
					
						bytes = ((String) (cd.value)).getBytes();
						
					}
					else if (cd.value instanceof Document) {
						
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
	
					if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
					
						Debug.log(
							Debug.MSG_DATA,
							"Logging column [" + cd.describe() + "].");
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
			
		
		//Populate customerid, interfaceversion, userid.
		if ( useCustomerId ) {
		
			//Use Ix slot number as-is, because it was already incremented
			//  before exiting above loop.
			SQLBuilder.populateCustomerID(pstmt, Ix);
		
			++Ix;
			
		}
		
		if ( useInterfaceVersion ) {
			
			SQLBuilder.populateInterfaceVersion(pstmt, Ix);
			
			++Ix;
			
		}
		if ( useUserId ) {
			
			SQLBuilder.populateUserID(pstmt, Ix);
			
		}
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		 Debug.log(Debug.DB_DATA, "Done populating SQL INSERT statement.");
		}
	}

	/**
	 * This method will compair the database column name, column type and
	 * column size with the configured properties.
	 * 
	 * @param ColumnData cd
	 * @param Connection dbConn
	 * 
	 * @throws Exception ResourceException.
	 */
	private boolean existMetaData( ColumnData cd, Connection dbConn )
												throws ResourceException {

		int colSize;

		ArrayList columnName = new ArrayList();

		ArrayList columnType = new ArrayList();

		ArrayList columnSize = new ArrayList();

		boolean existData = false;
		
		ResultSet rs = null;
		
		int cdValue;
		
		try{
			
			DatabaseMetaData dmd = dbConn.getMetaData();
	
			rs = dmd.getColumns(null, null, tableName, null);
	
			while (rs.next()) {
	
				colName = rs.getString( "COLUMN_NAME" );
	
				colType = rs.getString( "DATA_TYPE" );
	
				colSize = rs.getInt( "COLUMN_SIZE" );
	
				columnName.add( colName );
	
				columnType.add( colType );
	
				columnSize.add(Integer.valueOf(colSize ));
				
			}
	
			for (int i = 0; i < columnName.size(); i++) {
	
				if (((String) columnName.get(i)).equalsIgnoreCase(cd.columnName)) {
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){	
						Debug.log(Debug.SYSTEM_CONFIG,
								"Input Column Name : "+(String)columnName.get(i));
					}
						
					colType = ( String ) columnType.get(i);
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log(Debug.SYSTEM_CONFIG,
									"Input Column Type : "+colType);
					}
					
							
					colSize =  Integer.parseInt(columnSize.get(i).toString());
				    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log(Debug.SYSTEM_CONFIG,
										"Input Column size : "+ colSize);
					}
					
								
					if ( colType.equals(SOAConstants.NUMBER_TYPE)
							&& StringUtils.isDigits(cd.value.toString())){ 
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log(Debug.SYSTEM_CONFIG, "Column Numaric Value is : "+
												Integer.parseInt(cd.value.toString()));
					}
					
						cdValue = Integer.parseInt(cd.value.toString());
						
						if(colSize >= (String.valueOf(cdValue).length())){
							
							existData = true;
							
						}
						
					}else if ( colType.equals(SOAConstants.VARCHAR2_TYPE)){
						
						if( colSize >= (cd.value.toString()).length()){
								
								existData = true;

						}
					}
			
			}
		}
		}catch( SQLException se ){
						
			throw new ResourceException("Could not compair column data " +
													"with Database data" + se );
			
		}catch(NumberFormatException nf){
					
			Debug.log(Debug.ALL_ERRORS, "Column size is wrong");
							
		}finally {
			
			try
			{
				if (rs != null)
				{
					
					rs.close();
					rs = null;
					
				}
			}
			catch ( SQLException sqle )
			{

				Debug.log( Debug.ALL_ERRORS, 
					DBInterface.getSQLErrorMessage(sqle) );

			}			

		}
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(Debug.SYSTEM_CONFIG,
						"MetaData check validation is [" + existData + "]");
		}
						
		return existData;
	}

	/**
	 * Extract data for each column from the input message/context.
	 *
	 * @param context The message context.
	 * @param inputObject The input object.
	 *
	 * @exception MessageException  thrown if required value can't be found.
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private void extractMessageData(
		MessageProcessorContext context,
		MessageObject inputObject)
		throws MessageException, ProcessingException {
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "Extracting message data to log ...");
		}
		
		String location = null;
		
		StringTokenizer st;
		
		ColumnData cd;
		
		Iterator iter = columns.iterator();

		// While columns are available ...
		while ( iter.hasNext() ) {
		
			cd = (ColumnData) iter.next();

			if ( Debug.isLevelEnabled(Debug.MSG_DATA) ){
			
				Debug.log(
					Debug.MSG_DATA,
						"Extracting data for:\n" + cd.describe());
						
			}
			
			// If location was given, try to get a value from it.
			if ( StringUtils.hasValue(cd.location) ) {
				
				// Location contains one or more alternative locations that 
				// could contain the column's value.
				st = new StringTokenizer( cd.location, separator );

				// While location alternatives are available ...
				while ( st.hasMoreTokens() ) {
				
					// Extract a location alternative.
					location = st.nextToken().trim();

					// If the value of location indicates that the input 
					// message-object's entire value should be used as the
					// column's value, extract it.
					if ( location.equalsIgnoreCase(INPUT_MESSAGE)
						|| location.equalsIgnoreCase(SOAConstants.PROCESSOR_INPUT) ) {
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log(
								Debug.MSG_DATA,
								"Using message-object's contents as the column's value.");
						}

						cd.value = inputObject.get();
																		
						break;
						
					}

					// Attempt to get the value from the context or input object.
					if ( exists(location, context, inputObject) ){
					
						cd.value = get( location, context, inputObject );
						
					}
					// If we found a value, we're done with this column.
					if ( cd.value != null ) {
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log(
								Debug.MSG_DATA,
								"Found value for column ["
									+ cd.columnName
									+ "] at location ["
									+ location
									+ "].");
						}

						break;
					}
				}
			}

			// If no value was obtained from location in context/input, try to
			// set it from default value (if available).
			if (cd.value == null) {
				
				cd.value = cd.defaultValue;

				if (cd.value != null){
					if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
					Debug.log(
						Debug.MSG_DATA,
						"Using default value for column ["
							+ cd.columnName
							+ "].");
					}
				}
			}

			// If no value can be obtained ...
			if (cd.value == null) {
				
				// If the value is required ...
				if (cd.optional == false) {
				
					// Signal error to caller.
					throw new MessageException(
						"ERROR: Could not locate required value for column ["
							+ cd.columnName
							+ "], database table ["
							+ tableName
							+ "].");
				
				} else{ // No optional value is available, so just continue on.
				    if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
						Debug.log(
							Debug.MSG_DATA,
							"Skipping optional column ["
								+ cd.columnName
								+ "] since no data is available.");
					}
				}
			}
		}
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "Done extracting message data to log.");
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
		
		ColumnData cd; 
		
		// While columns are available ...
		while (iter.hasNext()) {
		
			cd = (ColumnData) iter.next();

			cd.value = null;
			
		}
	}

	/**
	 * Check that columns were configured and at least one
	 * mandatory field has a value to insert.
	 *
	 * @exception ProcessingException  thrown if invalid
	 */
	private void validate() throws ProcessingException {
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS, "Validating column data ...");
		}

		boolean valid = false;

		Iterator iter = columns.iterator();
		
		ColumnData cd;
		
		// While columns are available ...
		while (iter.hasNext()) {
		
			cd = (ColumnData) iter.next();

			// If we've found at least one value to insert, its valid.
			if (cd.value != null) {
		
				valid = true;

				break;
			}
		}

		if (!valid) {
		
			throw new ProcessingException(
				"ERROR: No database column values are available to write to ["
					+ tableName
					+ "].");
					
		}
        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		 Debug.log(Debug.DB_DATA, "Done validating column data.");
		}
	}

	/**
	 * Class ColumnData is used to encapsulate a description of a single column
	 * and its associated value.
	 */
	private static class ColumnData {
		
		public final String columnName;
		
		public final String columnType;
		
		public final String dateFormat;
		
		public final String location;
		
		public final String defaultValue;
		
		public final boolean optional;
		
		public final boolean metaDataCheck;

		public Object value = null;

		public ColumnData( String columnName, String columnType,
				String dateFormat, String location, String defaultValue,
					String optional, String metaDataCheck)
												throws FrameworkException {
													
			this.columnName = columnName;
			
			this.columnType = columnType;
			
			this.dateFormat = dateFormat;
			
			this.location = location;
			
			this.defaultValue = defaultValue;
			
			this.optional = StringUtils.getBoolean(optional);
			
			this.metaDataCheck = StringUtils.getBoolean(metaDataCheck);
			
		}

		public String describe() {
			
			StringBuffer sb = new StringBuffer();

			sb.append("Column description: Name [");
			
			sb.append(columnName);

			if (StringUtils.hasValue(columnType)) {
			
				sb.append("], type [");
			
				sb.append(columnType);
				
			}

			if (StringUtils.hasValue(dateFormat)) {
				
				sb.append("], date-format [");
				
				sb.append(dateFormat);
				
			}

			if (StringUtils.hasValue(location)) {
				
				sb.append("], location [");
				
				sb.append(location);
				
			}

			if (StringUtils.hasValue(defaultValue)) {
				
				sb.append("], default [");
				
				sb.append(defaultValue);
				
			}

			sb.append("], optional [");
			
			sb.append(optional);

			sb.append("], metaDataCheck [");
			
			sb.append(metaDataCheck);

			if (value != null) {
				
				sb.append("], value [");
				
				sb.append(value);
				
			}

			sb.append("].");

			return (sb.toString());
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
				"TNRangeLogger: USAGE:  "
					+ " jdbc:oracle:thin:@192.168.1.7:1521:soa jigar jigar ");

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

		SOADatabaseLogger logger = new SOADatabaseLogger();

		try {
			logger.initialize("FULL_NEUSTAR_SOA", "SOADatabaseLogger");

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
