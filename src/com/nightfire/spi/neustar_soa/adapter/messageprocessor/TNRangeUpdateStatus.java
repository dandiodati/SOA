/**
 * The purpose of this processor is to update old active and download-failed-partial SV record(s) into 
 * SOA_SUBSCRIPTION_VERSION table with status=old  
 * 
 * @author vkyadav
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is
 * considered to be confidential and proprietary to NeuStar.
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
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
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.spi.common.driver.Converter;
 * @see com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
 * @see	com.nightfire.spi.neustar_soa.utils.SOAConstants;
 * @see com.nightfire.framework.message.parser.xml.XMLMessageParser;
 * @see com.nightfire.spi.neustar_soa.utils.SOAUtility;
 * @see com.nightfire.framework.util.CustomerContext;
 */

/**
 
 Revision History
 ---------------
 
 Rev#	Modified By			Date			Reason
 ----- ----------- ---------- --------------------------        
 1		vyadav			10/08/2009		Created
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import org.w3c.dom.Document;


import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DBLOBUtils;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.Converter;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class TNRangeUpdateStatus extends DBMessageProcessorBase {
	
	/**
	 * Name of the oracle table requested
	 */
	private String tableName = null;
	
	/**
	 * Name of the separator token
	 */
	private String separator = null;
	
	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
	private boolean usingContextConnection = true;
	
	private List<ColumnData> columns = null;
	
	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;
	
	/**
	 * Input message location
	 */
	private String inputMessage = null;
	
	/**
	 * Context location of notification
	 */
	private String notification = null;
	
	/**
	 * 	This variable used to get location of Region ID.
	 */
	private String regionIdLoc = null;
	
	/**
	 * 	This variable used to get value of Region ID.
	 */
	private String regionId = null;
	
	/**
	 * 	This variable used to get location of status .
	 */
	private String statusLoc = null;
	
	/**
	 * 	This variable used to get value of Status.
	 */
	private String status = null;


 	/**
	 * Constructor.
	 */
	public TNRangeUpdateStatus() {
		
		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ))
			Debug.log(Debug.OBJECT_LIFECYCLE, "Creating TNRangeUpdateStatus message-processor.");
		
		columns = new ArrayList<ColumnData>();
	}
	
	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param key Property-key to use for locating initialization properties.
	 * @param type Property-type to use for locating initialization properties.
	 *
	 * @exception ProcessingException when initialization fails
	 */
	public void initialize(String key, String type) throws ProcessingException {
		
		// Call base class method to load the properties.
		super.initialize(key, type);
		
		// Get configuration properties specific to this processor.
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "TNRangeUpdateStatus: Initializing...");
		
		StringBuffer errorBuffer = new StringBuffer();
		
		tableName = getRequiredPropertyValue(SOAConstants.TABLE_NAME_PROP,errorBuffer);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Database table to update to is ["+ tableName + "].");
		
		String strTemp = getPropertyValue(SOAConstants.TRANSACTIONAL_LOGGING_PROP);
		
		if (StringUtils.hasValue(strTemp)) {
			try {
				
				usingContextConnection = getBoolean(strTemp);
				
			} catch (FrameworkException e) {
				
				errorBuffer.append("Property value for "
						+ SOAConstants.TRANSACTIONAL_LOGGING_PROP
						+ " is invalid. " + e.getMessage() + "\n");
				
			}
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG )){
			
			Debug.log(Debug.SYSTEM_CONFIG,"Logger will participate in overall driver transaction? ["
					+ usingContextConnection + "].");
		}
		separator = getPropertyValue(SOAConstants.LOCATION_SEPARATOR_PROP);
		
		if (!StringUtils.hasValue(separator)) {
			separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location separator token [" + separator	+ "].");
		
		inputMessage = getRequiredPropertyValue(
				SOAConstants.INPUT_XML_MESSAGE_LOC_PROP, errorBuffer);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of inputMessage is ["+ inputMessage + "].");
		
		notification = getPropertyValue(SOAConstants.NOTIFICATION_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Configured value of notification is ["+ notification + "].");
		
		regionIdLoc = getPropertyValue(SOAConstants.REGION_ID_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location of region ID value ["
				+ regionIdLoc + "].");
		
		statusLoc = getPropertyValue(SOAConstants.STATUS_LOC_PROP);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Location of status  value ["
				+ statusLoc + "].");				



		String colName = null;
		
		String colType = null;
		
		String dateFormat = null;
		
		String defaultValue = null;
		
		String optional = null;
		
		String location = null;
		
		ColumnData cd = null;
		
		// Loop until all column configuration properties have been read ...
		for (int i = 0; true; i++) {
			colName = getPropertyValue(PersistentProperty.getPropNameIteration(
					SOAConstants.COLUMN_PREFIX_PROP, i));
			
			// If we can't find a column name, we're done.
			if (!StringUtils.hasValue(colName)) {
				break;
			}
			
			colType = getPropertyValue(PersistentProperty.getPropNameIteration(
					SOAConstants.COLUMN_TYPE_PREFIX_PROP, i));
			
			dateFormat = getPropertyValue(PersistentProperty
					.getPropNameIteration(SOAConstants.DATE_FORMAT_PREFIX_PROP,
							i));
			
			location = getPropertyValue(PersistentProperty
					.getPropNameIteration(SOAConstants.LOCATION_PREFIX_PROP, i));
			
			defaultValue = getPropertyValue(PersistentProperty
					.getPropNameIteration(SOAConstants.DEFAULT_PREFIX_PROP, i));
			
			optional = getRequiredPropertyValue(
					PersistentProperty.getPropNameIteration(
							SOAConstants.OPTIONAL_PREFIX_PROP, i), errorBuffer);
			
			try {
					// Create a new column data object and add it to the list.
					cd = new ColumnData(colName, colType, dateFormat, location,	defaultValue, optional);
	
					if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
						Debug.log(Debug.SYSTEM_CONFIG, cd.describe());
					}	
					columns.add(cd);
				 } catch (FrameworkException e) {
					throw new ProcessingException(
							"ERROR: Could not create column data description:\n"+ e.toString());
				}
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "Maximum number of columns to insert [" + columns.size() + "].");
		
		// If any of required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();
			
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS, errMsg);
			
			throw new ProcessingException(errMsg);
		}
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log(Debug.SYSTEM_CONFIG, "TNRangeUpdateStatus: Initialization done.");
	} //end of initialize method
	
	/**
	 * Extract data values from the context/input, and use them to
	 * update a row into the configured database table.
	 *
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	public NVPair[] process(MessageProcessorContext mpContext,MessageObject inputObject) 
	throws MessageException,ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside process method");
		
		if (inputObject == null) {
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, this.getClass() + " obj is null");
			return null;
		}
		
		this.mpContext = mpContext;
		
		this.inputObject = inputObject;		
		
		Connection dbConn = null;
		try
		{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );		
			
			// if statusLoc is configured
			if (statusLoc != null) {
				
				// Get the status value from context
				status = getValue(statusLoc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "status value :" + "[ " + status + " ].");			
			}
			
			// if regionIdLoc is configured
			if (regionIdLoc != null) {
				
				// Get the REGIONID value from context
				regionId = getValue(regionIdLoc);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log(Debug.SYSTEM_CONFIG, "Region ID value :" + "[ " + regionId + " ].");			
			}


	
			// if NOTIFICATION_TYPE is configured
			if (notification != null) {
				// Get the notification value from context
				notification = getValue(notification);
				
				if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
					Debug.log( Debug.SYSTEM_CONFIG, "Value of Notification is ["+ notification + "]." );			
			}
	
			try {
				// Get a database connection from the appropriate
				//location - based
				// on transaction characteristics.
				if (usingContextConnection) {
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
						Debug.log(Debug.MSG_STATUS, "Database logging is"
								+ " transactional, so getting "
								+ "connection from context.");
					}
					
					dbConn = mpContext.getDBConnection();
				} else {
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
						Debug.log(Debug.MSG_STATUS, "Database logging is not "
								+ "transactional, so getting"
								+ " connection from NightFire" + " pool.");
					}
	
					dbConn = DBConnectionPool.getInstance(true).acquireConnection();
				}
				
				if (dbConn == null) {
					// Throw the exception to the driver.
					throw new ProcessingException("DB "
							+ "connection is not available");
				}
				
			} catch (FrameworkException e) {
				String errMsg = "ERROR: TNRangeUpdateStatus:"
					+ " Attempt to get database connection"
					+ " failed with error: " + e.getMessage();
				
					Debug.log(Debug.ALL_ERRORS, errMsg);
				
				// Re-throw the exception to the driver.
				if (e instanceof MessageException) {
					
					throw (MessageException) e;
					
				} else {
					
					throw new ProcessingException(errMsg);
					
				}
			}
			// Update the SV Object table
			updateSvObject( dbConn );
		}
		finally
		{
			ThreadMonitor.stop(tmti);
		}
		return (formatNVPair(inputObject));
	}	
	
	/**
	 * This method is used to get Tn Svid List value from the input message if
	 * input message comes with TnSvidList node or with the TnSvIdRange node
	 *
	 * @param  parser  as XMLMessageParser
	 *
 	 * @return  TnList.
	 */

	private ArrayList<TnSvid> getTnSvidList(XMLMessageParser parser)
	{
		ArrayList<TnSvid> tnList = new ArrayList<TnSvid>();
		TnSvid tnSvid = null;
		
		String rootNodeTnSvIdList = "SOAToUpstream.SOAToUpstreamBody." + notification	+ ".Subscription.TnSvIdList";
		
		String rootNodeTnSvIdRange = "SOAToUpstream.SOAToUpstreamBody." + notification + ".Subscription.TnSvIdRange";

		try
		{
			// if the notification contains TnSvIdList node
			if (parser.exists(rootNodeTnSvIdList)) 
			{
				int tnSvIdCount = 0;
				String tnNode = null;
				String svidNode = null;
				String tn = null;
				long svid = -1;
				tnSvIdCount = parser.getChildCount(rootNodeTnSvIdList);

				// Add all the tn in tnList list
				 for (int i = 0; i < tnSvIdCount; i++)
				 {
					 tnNode = "SOAToUpstream.SOAToUpstreamBody."
										+ notification 
										+ ".Subscription.TnSvIdList.TnSvId("
										+ i + ")" + ".Tn";
					 svidNode = "SOAToUpstream.SOAToUpstreamBody."
							+ notification 
							+ ".Subscription.TnSvIdList.TnSvId("
							+ i + ")" + ".SvId";

					if (parser.exists( tnNode ))
					{
						// Get the value of TN
						tn = parser.getValue( tnNode );
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,	"TNRangeUpdateStatus: tn :\n"+ tn);
					}
					if (parser.exists( svidNode ))
					{
						// Get the value of svid
						svid = Long.parseLong(parser.getValue( svidNode ));
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,	"TNRangeUpdateStatus: svid :\n"+ svid);
					}
					if(tn != null && svid >0){
						tnSvid = new TnSvid (tn, svid);
						tnList.add(tnSvid);
					}
				 }			
			 }
			 else if (parser.exists(rootNodeTnSvIdRange)) 
			 {
				String startTn = null;
				String endStation = null;
				long startSvid = -1;
								
				String startTnNode = "SOAToUpstream.SOAToUpstreamBody."	+ notification + ".Subscription.TnSvIdRange.TnRange.Tn";
				String startSvidNode = "SOAToUpstream.SOAToUpstreamBody."	+ notification + ".Subscription.TnSvIdRange.SvIdRange.StartId";
				
				if (parser.exists( startTnNode ))
				{
					// Get the value of startTn
					startTn = parser.getValue( startTnNode );
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,	"TNRangeUpdateStatus: startTn :\n"+ startTn);
				}
				
				if (parser.exists( startSvidNode ))
				{
					// Get the value of TN
					startSvid = Long.parseLong(parser.getValue( startSvidNode ));
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,	"TNRangeUpdateStatus: svid :\n"+ startSvid);
				}
				
				String endTnNode = "SOAToUpstream.SOAToUpstreamBody."+ notification + ".Subscription.TnSvIdRange.TnRange.EndStation";
				if (parser.exists( endTnNode ))
				{
					// Get the value of endtn
					endStation = parser.getValue( endTnNode );
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,	"TNRangeUpdateStatus: endStation :\n"+ endStation);
				}
				if(startTn != null && endStation != null && startSvid > 0)
				tnList = getStartEndTnList(startTn, endStation, startSvid);
			  }
		}
		catch( MessageException re )
		{		
				Debug.log( Debug.ALL_ERRORS, re.getMessage() );
		}
		catch( FrameworkException fe )
		{		
				Debug.log( Debug.ALL_ERRORS, fe.getMessage() );
		}
		return tnList;
	}
    
	/**
	 * This method is used to get Tn svid List value
	 * 
	 * @param  startTn  as String
	 * @param  endStation  as String
 	 * @return  TnList.
	 */

	private ArrayList<TnSvid> getStartEndTnList(String startTn, String endStation, long startSvid ) {

		   int start=0;			  
		   int end=0;
		   ArrayList<TnSvid> tnList = new ArrayList<TnSvid>();
		   TnSvid tnSvid = null;

		   try
		   {
				start = Integer.parseInt(startTn.substring(
						SOAConstants.STARTTN_END_INDEX, SOAConstants.TN_END_INDEX));
	
				end = Integer.parseInt(endStation);
	
				String tnPrefix = startTn.substring(SOAConstants.TN_START_INDEX,
						SOAConstants.STARTTN_END_INDEX);
	 
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,
						"TNRangeUpdateStatus: Creating TnSvid object for TN and SVID:\n"
								+ startTn + " \n" + startSvid);

				tnSvid = new TnSvid (startTn, startSvid);
				
				tnList.add(tnSvid);
				
				for (int suffix = start + 1; suffix <= end; suffix++) {
					startSvid++;
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,
							"TNRangeUpdateStatus: Creating TnSvid object for TN and SVID:\n"
									+ tnPrefix	+ StringUtils.padNumber(suffix, 4, true, '0') + " \n" + startSvid);
					
					tnSvid = new TnSvid (tnPrefix + StringUtils.padNumber(suffix, 4, true, '0'), startSvid);
					tnList.add(tnSvid);
				}
		    }catch (Exception e){
				 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					 Debug.log(Debug.MSG_STATUS,"getStartEndTnList exception: " +e.toString());
		    }	
		   return tnList;
	   }

	/**
	 * Update the SV Objects table 
	 *
	 * @param  dbConn  Connection Object
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	
	
	private void updateSvObject(Connection dbConn) 
	throws MessageException, ProcessingException
	{			
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, this.getClass() + " Inside updateSvObject method");
				
		PreparedStatement updateStatement = null;		

		ArrayList<TnSvid> tnList = null;
		
		try {						
			
			// get input XML from context		
			Document doc = (Document) super.getDOM(inputMessage, mpContext,inputObject);
			
			XMLMessageParser parser = new XMLMessageParser(doc);
			// Reset the values in cases where we're logging again.
			resetColumnValues();
			
			// Extract the column values from the arguments.
			extractMessageData(mpContext, inputObject);			

			// Get the Tn svid list
			tnList = getTnSvidList(parser);

			boolean firstSv = true;
			if(tnList != null){
				Iterator<TnSvid> iterTnSvidList = tnList.iterator();
				// Process all the record one by one
					while (iterTnSvidList.hasNext()) {				
					// If it is the first record which is selected from DB
					if (firstSv)
					{
						String sqlStmt = constructUpdateSqlStatement();
			
						// Getting a prepared statement for the SQL statement.
						updateStatement = dbConn.prepareStatement(sqlStmt);
	
						firstSv = false;
					}
					
					if(updateStatement != null ){
						
					    //update SV Object table
						update(updateStatement,(TnSvid)iterTnSvidList.next());
					}
				}
			}
	
			if (updateStatement != null)
			{
				//executing the batch statement to update the batch sql statements.
				updateStatement.executeBatch();
			}
			// If the configuration indicates that this SQL operation
			// isn't part of the overall driver
			// transaction, commit the changes now.
			if (!usingContextConnection) {
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Committing data updated by TNRangeUpdateStatus to database.");
				
				try {
									
					DBConnectionPool.getInstance(true).commit(dbConn);
					
				} catch (ResourceException re) {
					
					if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
						Debug.log(Debug.ALL_ERRORS, re.getMessage());
					
				}
				
			}
		} catch (Exception e) {
			String errMsg = "ERROR: TNRangeUpdateStatus: Attempt to log to database "
				+ "failed with error: " + e.getMessage();
			
			Debug.log(Debug.ALL_ERRORS, errMsg);
			
			// If the configuration indicates that this SQL operation isn't part
			// of the overall driver transaction, roll back any changes now.
			if (!usingContextConnection) {
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Rolling-back any database changes due to database-logger.");
				
				try {
					
					DBConnectionPool.getInstance(true).rollback(dbConn);
					
				} catch (ResourceException re) {
					
					Debug.log(Debug.ALL_ERRORS, re.getMessage());
					
				}
			}
			
			// Re-throw the exception to the driver.
			if (e instanceof MessageException) {
				throw (MessageException) e;
			} else {
				throw new ProcessingException(errMsg);
			}
		} finally {
			
			try {
				// if updateStatement not closed
				if (updateStatement != null)
				{
					// close the update statement
					updateStatement.close();
					
					updateStatement = null;
				}				
				
			} catch (SQLException sqle) {
				
				Debug.log(Debug.ALL_ERRORS, DBInterface	.getSQLErrorMessage(sqle));
				
			}
			
			// If the configuration indicates that this SQL operation isn't
			// part of the overall driver transaction, return the connection
			// previously acquired back to the resource pool.
			if (!usingContextConnection && (dbConn != null)) {
				try {
					// release the connection to resource pool
					DBConnectionPool.getInstance(true).releaseConnection(dbConn);					
					dbConn = null;					
				} catch (ResourceException e) {
					
					Debug.log(Debug.ALL_ERRORS, e.toString());
					
				}
			}
			
		}
		
	}
	
	
	/**
	 * Reset the column values in the list.
	 */
	private void resetColumnValues() {
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "Resetting column values ...");
		
		Iterator<ColumnData> iter = columns.iterator();
		
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
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private void extractMessageData(MessageProcessorContext context,MessageObject inputObject) 
	throws MessageException,ProcessingException {
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "Extracting message data to log ...");
		
		Iterator<ColumnData> iter = columns.iterator();
		
		ColumnData cd = null;
		
		// While columns are available ...
		while (iter.hasNext()) {
			cd = (ColumnData) iter.next();
			
			if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
				Debug.log(Debug.MSG_DATA, "Extracting data for:\n"+ cd.describe());
			}
			
			// If location was given, try to get a value from it.
			if (StringUtils.hasValue(cd.location)) {
				// Location contains one or more alternative locations
				// that could contain the column's value.
				StringTokenizer st = new StringTokenizer(cd.location, separator);
				
				String loc = null;
				
				// While location alternatives are available ...
				while (st.hasMoreTokens()) {
					// Extract a location alternative.
					loc = st.nextToken().trim();
					
					// If the value of location indicates that the input
					// message-object's entire value should be used as the
					// column's value, extract it.
					if (loc.equalsIgnoreCase(INPUT_MESSAGE)
						|| loc.equalsIgnoreCase(SOAConstants.PROCESSOR_INPUT)) {
						
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
							Debug.log(Debug.MSG_DATA, "Using message-object's " + "contents as the column's value.");
						
						cd.value = inputObject.get();
						
						break;
						
					}
					
					//Attempt to get the value from the context or input object.
					if (exists(loc, context, inputObject)) {
						cd.value = get(loc, context, inputObject);
					}
					
					// If we found a value, we're done with this column.
					if (cd.value != null) {
						
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
							Debug.log(Debug.MSG_DATA, "Found value for column ["
								+ cd.columnName + "] at location [" + loc + "].");
						
						break;
						
					}
				}
			}
			
			// If no value was obtained from location in context/input,
			// try to set it from default value (if available).
			if (cd.value == null) {
				
				cd.value = cd.defaultValue;
				
				if (cd.value != null) {
					
					if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
						Debug.log(Debug.MSG_DATA, "Using default value for column [" + cd.columnName + "].");
				}
			}
			
			// If no value can be obtained ...
			if (cd.value == null) {
				// If the value is required ...
				if (cd.optional == false) {
					
					// Signal error to caller.
					throw new MessageException(
							"ERROR: Could not locate required value for column ["
							+ cd.columnName + "], database table ["
							+ tableName + "].");
					
				} else // No optional value is available, so just continue on.
				{
					if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
						Debug.log(Debug.MSG_DATA, "Skipping optional column ["
							+ cd.columnName + "] since no data is available.");
				}
			}
		}
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "Done extracting message data to log.");
	}
	
	/**
	 * Construct the SQL UPDATE statement from the column data.
	 *
	 * @return SQL UPDATE statement.
	 *
	 * @exception  DatabaseException  Thrown on data errors.
	 */
	private String constructUpdateSqlStatement()
	throws ProcessingException, MessageException{
		
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Constructing SQL UPDATE statement ...");
		
		// Construct the update statement
		StringBuffer sb = new StringBuffer();
		
		sb.append("UPDATE ");
		
		sb.append(tableName);
		
		sb.append(" SET ");
		
		boolean firstOne = true;
		
		Iterator<ColumnData> iter = columns.iterator();
		
		ColumnData cd = null;
		
		// Append the names of columns with non-null values.
		while (iter.hasNext()) {
			cd = (ColumnData) iter.next();
			
			// Skip columns with null values since null values aren't updated.
			// and skip if update is false
			if (SOAUtility.isNull(cd.value)) {
				continue;
			}
			
			if (firstOne) {
				firstOne = false;
			} else {
				sb.append(", ");
			}
			
			sb.append(cd.columnName);
			
			// If the current column is a date column, and the configuration
			// indicates that the current system date should be used for
			// the value, place it in the SQL statement now.
			if (StringUtils.hasValue(cd.columnType)
					&& cd.columnType
					.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_DATE)
					&& (cd.value instanceof String)
					&& ((String) (cd.value ))
					.equalsIgnoreCase(SOAConstants.SYSDATE)) {
				
				sb.append(" = ");
				sb.append(SOAConstants.SYSDATE);
				
			} else {
				
				sb.append(" = ? ");
				
			}
		}		
		sb.append(" WHERE ");
		sb.append(SOAConstants.PORTINGTN_COL);
		sb.append(" = ");
		sb.append(" ? ");
		// if it is a notification
		if (notification != null) {								

			// if notification type is Activate or PortToOriginal notification
			if (notification.equals(SOAConstants.SV_ACTIVATE_NOTIFICATION)
					|| notification.equals(SOAConstants.SV_DISCONNECT_NOTIFICATION)
					|| notification.equals(SOAConstants.SV_PTO_NOTIFICATION)
					|| (!notification
							.equals(SOAConstants.SOA_SV_DONOR_NOTIFICATION)
							&& status != null && (status.equals("old") || status
							.equals("active"))))
			{
				sb.append(" AND ");
				sb.append(SOAConstants.SVID_COL);
				sb.append(" != ");
				sb.append(" ? ");
				sb.append(" AND ");
				sb.append(SOAConstants.LASTUPDATE_COL);
				sb.append(" < SYSDATE-1");		
			}
		}
		sb.append(" AND ");
		sb.append(SOAConstants.STATUS_COL);
		sb.append(" IN ('active','download-failed-partial')");
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Done constructing SQL UPDATE statement. "
				+ sb.toString());
		
		return (sb.toString());
	}
	
	/**
	 * update SV Object table using the given statement.
	 *
	 * @param  dbConn  The database connection to perform the SQL update
	 * operation against.
	 * @param  tnSvid  object to perform the SQL update.
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private void update(PreparedStatement pstmt,TnSvid tnSvid)
	throws ProcessingException {
		
		// Make sure that at least one column value will be updated.
		validate();
						
		try {

			// Populate the SQL statement using values obtained from
			// the tnSvid data objects.
			populateSqlStatement(pstmt,tnSvid.tn,tnSvid.svid);
			
			// Add into batch
			pstmt.addBatch();
			
			
		} catch (SQLException sqle) {
			
			throw new ProcessingException(
					"ERROR: Could not update row into database table ["
					+ tableName + "]:\n"
					+ DBInterface.getSQLErrorMessage(sqle));
			
		} catch (Exception e) {
			
			throw new ProcessingException(
					"ERROR: Could not update row into database table ["
					+ tableName + "]:\n" + e.toString());
			
		}
	}
	
	/**
	 * Populate the SQL UPDATE statement from the column data.
	 *
	 * @param  pstmt  The prepared statement object to populate.
	 *
	 * @exception Exception  thrown if population fails.
	 */
	private void populateSqlStatement(PreparedStatement pstmt,String tn,long svid)
	throws ProcessingException {
		
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Populating SQL update statement ...");
		
		try {
			
			int Ix = 1; // First value slot in prepared statement.
			
			Iterator<ColumnData> iter = columns.iterator();
			
			ColumnData cd = null;
			
			// Append the names of columns with non-null values.
			while (iter.hasNext()) {
				cd = (ColumnData) iter.next();
				
				if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
					Debug.log(Debug.MSG_DATA, "Populating SQL statement for:\n"
							+ cd.describe());
				}
				
				//Skip columns with null values since null 
				//values aren't inserted
				// or updated and also skip columns 
				//for update if update is false.
				if (SOAUtility.isNull(cd.value)) {
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS, "Skipping null column value.");
					
					continue;
					
				}
				
				if ( Debug.isLevelEnabled( Debug.DB_DATA ))
					Debug.log(Debug.DB_DATA, "Populating prepared-statement slot [" + Ix + "].");
				
				// Default is no column type specified.
				if (!StringUtils.hasValue(cd.columnType)) {
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
						
						Debug.log(Debug.MSG_DATA, "Value for column ["
								+ cd.columnName + "] is [" + cd.value.toString()
								+ "].");
					}
						
					pstmt.setObject(Ix, cd.value);
					
				} 
				// if column type is date
				else if (cd.columnType
						.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_DATE)) {
					
					String val = (String) (cd.value);
					
					if ( Debug.isLevelEnabled( Debug.MSG_DATA )){
						Debug.log(Debug.MSG_DATA, "Value for date column ["
								+ cd.columnName + "] is [" + val + "].");
					}
						
					
					//SYSDATE is already in the text of the SQL statement 
					//used to create
					//the prepared statement, so there's nothing more to do here.
					if (val.equalsIgnoreCase(SOAConstants.SYSDATE)) {
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
							
							Debug.log(Debug.MSG_STATUS, "Skipping date population "
									+ "since SYSDATE is already in SQL string.");
						}						
						continue;
						
					}
					
					// If we're not inserting the current system date,
					//the caller must
					// have provided an actual date value,
					//which we must now parse.
					if (!StringUtils.hasValue(cd.dateFormat)) {
						
						throw new ProcessingException(
								"ERROR: Configuration for " + " date column ["
								+ cd.columnName
								+ "] does not specify date format.");
						
					}
					
					SimpleDateFormat sdf = new SimpleDateFormat(cd.dateFormat);
					
					java.util.Date d = sdf.parse(val);
					
					pstmt.setTimestamp(Ix, new java.sql.Timestamp(d.getTime()));
				}
				// if column type is Text BLOB
				else if (cd.columnType
						.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_TEXT_BLOB)) {
					
					if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
						Debug.log(Debug.MSG_DATA, "Querying column ["
								+ cd.describe() + "].");
					}
					
					DBLOBUtils
					.setCLOB(pstmt, Ix, Converter.getString(cd.value));
					
				} 
				// if column type is Binary BLOB
				else if (cd.columnType
						.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_BINARY_BLOB))
				{
					
					byte[] bytes = null;
					
					if (cd.value instanceof byte[]) {
						bytes = (byte[]) cd.value;
					}
					
					else if (cd.value instanceof String) {
						bytes = ((String) (cd.value)).getBytes();
					}
					
					else if (cd.value instanceof Document) {
						bytes = Converter.getString(cd.value).getBytes();
					} else {
						
						throw new ProcessingException(
								"ERROR: Value for database table ["
								+ tableName
								+ "], column ["
								+ cd.columnName
								+ "] of type ["
								+ cd.value.getClass().getName()
								+ "] can't be converted to byte stream.");
						
					}
					
					if (Debug.isLevelEnabled(Debug.MSG_DATA)) {
						Debug.log(Debug.MSG_DATA, "Querying column ["
								+ cd.describe() + "].");
					}
					DBLOBUtils.setBLOB(pstmt, Ix, bytes);
					
				} else {
					
					throw new ProcessingException(
							"ERROR: Invalid column-type property value ["
							+ cd.columnType
							+ "] given in configuration.");
					
				}
				
				// Advance to next value slot in prepared statement.
				Ix++;
			}							
			pstmt.setString(Ix,tn);
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,
					"TNRangeUpdateStatus: Populated updated statement for TN :\n"+ tn);
			// if it is a notification
			if (notification != null) {
				// if notification type is Activate or PortToOriginal or SvDisconnect
				if ( notification.equals(SOAConstants.SV_ACTIVATE_NOTIFICATION)
					 || notification.equals(SOAConstants.SV_DISCONNECT_NOTIFICATION)
					 || notification.equals(SOAConstants.SV_PTO_NOTIFICATION)					
					 || (!notification
								.equals(SOAConstants.SOA_SV_DONOR_NOTIFICATION)
								&& status != null && (status.equals("old") || status
								.equals("active"))))
				{
					Ix++;
					pstmt.setLong(Ix, svid);
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,
							"TNRangeUpdateStatus: Populated updated statement for svid :\n"+ svid);

				}
			}
		} catch (Exception exception) {
			throw new ProcessingException(
					"ERROR:  could not populate sql statement ."
					+ exception.toString());
			
		}
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Done populating SQL UPDATE statement.");
	}
	
	/**
	 * Check that columns were configured and at least one
	 * mandatory field has a value to insert.
	 *
	 * @exception ProcessingException  thrown if invalid
	 */
	private void validate() throws ProcessingException {
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "Validating column data ...");
		
		boolean valid = false;
		
		Iterator<ColumnData> iter = columns.iterator();
		
		ColumnData cd = null;
		
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
					"ERROR: No database column values are available to write to ["+ tableName + "].");
		}
		if ( Debug.isLevelEnabled( Debug.DB_DATA ))
			Debug.log(Debug.DB_DATA, "Done validating column data.");
	}
	
	/**
	 * This method tokenizes the input string and return an
	 * object for exsisting value in context or messageobject.
	 *
	 * @param  locations as a string
	 *
	 * @return  object
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 
	 */
	protected String getValue(String locations) throws MessageException,
	ProcessingException {
		StringTokenizer st = new StringTokenizer(locations,
				DBMessageProcessorBase.SEPARATOR);
		
		String tok = null;
		
		// While tokens are available ...
		while (st.hasMoreTokens()) {
			tok = st.nextToken();
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "Checking location [" + tok	+ "] for value...");
			
			// if the value of token exists in context or messageobject.
			if (exists(tok, mpContext, inputObject)) {
				return ((String) get(tok, mpContext, inputObject));
			}
		}
		
		return null;
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
		
		public Object value = null;
		
		public ColumnData(String columnName, String columnType,
				String dateFormat, String location, String defaultValue,
				String optional) throws FrameworkException {
			
			this.columnName = columnName;
			this.columnType = columnType;
			this.dateFormat = dateFormat;
			this.location = location;
			this.defaultValue = defaultValue;
			this.optional = StringUtils.getBoolean(optional);
		}
		
		/**
		 * To describe the column name, column type, data format, location
		 * and default value.
		 */
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
			
			if (value != null) {
				sb.append("], value [");
				sb.append(value);
			}
			
			sb.append("].");
			
			return (sb.toString());
		}
	}
	
	/**
	 * TnSVID class is used to encapsulate the Tn , svID info in objects.
	 * @author vkyadav
	 *
	 */
	private static class TnSvid {

        public String tn;
        public long svid;
        public TnSvid(String tn, long svid) throws FrameworkException {

            this.tn = tn;
            this.svid = svid;
            

        }

    }

	//--------------------------For Testing---------------------------------//
	
	public static void main(String[] args) {
		
		Properties props = new Properties();
		
		props.put("DEBUG_LOG_LEVELS", "ALL");
		
		props.put("LOG_FILE", "E:\\logmap.txt");
		
				
		if (args.length != 3) 
		{	
			return;
		}
		try {
			
			DBInterface.initialize(args[0], args[1], args[2]);
			
		} catch (DatabaseException e) {
			
			
		}
		
		TNRangeUpdateStatus rangeUpdate = new TNRangeUpdateStatus();
		
		try {
			
			rangeUpdate.initialize("FULL_NEUSTAR_SOA", "TNRangeUpdateStatus");
			
			MessageProcessorContext mpx = new MessageProcessorContext();
			
			MessageObject mob = new MessageObject();
				
			mob.set("StartTN", "8391");
			mob.set("EndTN", "8393");
			mob.set("REQUEST_HEADER_SUBREQUEST", "SvModifyRequest");
			//mob.set("STARTSVID","210841");
			//mob.set("RANGE_REFERENCEKEY_OUT_LOC","abcd");
			
			//mob.set("START","1");
			//mob.set("END","5");
			mob.set("NPA", "530");
			mob.set("NXX", "012");
			mob.set("SPID", "1111");
			mob.set("ONSP", "1235");
			mob.set("NNSP", "4567");
			mob.set("NOTIFICATION", "SvActivateNotification");
			//mob.set("SVID","2000");
			mob.set("LRN", "30000");
			mob.set("LNP", "lspp");
			mob.set("LASTREQUESTTYPE", "SvActivateRequest");
			//mob.set("REFERENCEKEY_OUT_LOC","ABC");
			//mob.set("TABLE_NAME","SOA_SUBSCRIPTION_VERSION");
			mob.set("KEY1", "<SOAMessage>" + "<SOAToUpstream>"
					+ "<SOAToUpstreamHeader>"
					+ "<DateSent value=\"10-20-2004-053236PM\" />"
					+ "<RegionId value=\"0003\" />" + "</SOAToUpstreamHeader>"
					+ "<SOAToUpstreamBody>" + "<SvActivateNotification>"
					+ "<Subscription>" + "<TnSvIdRange>" + "<TnRange>"
					+ "<Tn value=\"530-012-8391\" />"
					+ "<EndStation value=\"8393\" />" + "</TnRange>"
					+ "<SvIdRange>" + "<StartId value=\"210841\" />"
					+ "<EndId value=\"210843\" />" + "</SvIdRange>"
					+ "</TnSvIdRange>" + "</Subscription>"
					+ "<NewSP value=\"A111\" />" + "<OldSP value=\"1111\" />"
					+ "</SvActivateNotification>" + "</SOAToUpstreamBody>"
					+ "</SOAToUpstream>" + "</SOAMessage>");
			
			//mob.set("LASTMESSAGE","SvReleaseInConflictRequest");
			//mob.set("CREATEDBY","Jaganmohan");
			
			//mob.set("CREATEDDATE", "SYSDATE");
			
			rangeUpdate.process(mpx, mob);
			
		} catch (ProcessingException pex) {
		} catch (MessageException mex) {
		}
	} //end of main method	
}