/**
 * This takes the SV data for a specific service provider in a specific region
 * from lines of an SV BDD file and then uses this data to update the
 * SOA_SUBSCRIPTION_VERSION table in the database.
 * 
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.spi.neustar_soa.file.NPACTokenConsumer 
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants
 * @see com.nightfire.spi.neustar_soa.adapter.NPACConstants
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			04/12/2004			Created
	2			Ashok			05/06/2004			LASTREQUESTTYPE,
													LASTREQUESTDATE,
													LASTRESPONSE, 
													LASTRESPONSEDATE 
													assigned to NULL,
													Insert records in 
													LAST_NPAC_RESPONSE_TIME
													table , populate customer
													Id from Customer_lookup
													table and generate Sequence
													Id also
	3			Ashok			06/30/2004			Database design changed
	4. 			D.Subbarao		02/15/2006			Implemented some abstract methods 
													with empty. 
    5. 			D.Subbarao		02/16/2006 			Modified.	
	6.			Manoj Kumar		05/24/2006			SVType and Alternative Spid columns added 
													in SOA_SUBSCRIPTION_VERSION_TEMP 
													and SOA_SUBSCRIPTION_VERSION table 
 */

package com.nightfire.spi.neustar_soa.file;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class SVTokenConsumer extends BDDTokenConsumer {

	/**
	 * The SQL insert statement used to insert new SV data.
	 */

    
	public static final String INSERT = "insert into "
										+ SOAConstants.SV_TABLE
										+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, "
										+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
										+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
										+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
										+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

	
	
	/**
	 * The SQL insert statement used to insert new SV data in SV TEMP table.
	 */
    
	
	public static final String INSERT_TEMP = "insert into "
										+ SOAConstants.SV_TEMP_TABLE
										+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, "
										+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
										+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
										+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
										+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
	
	/**
	 * The SQL update statement used to update existing
	 * rows of NPANXX_X table.
	 */
	private static final String UPDATE = "update "
										+ SOAConstants.NPANXXX_TABLE
										+ " set "
										+ SOAConstants.ACTIVEFLAG_COL
										+ " ="
										+ SOAConstants.NPANXXX_ACTIVEFLAG
										+ " where "
										+ SOAConstants.SPID_COL
										+ " = ? and "
										+ SOAConstants.NPA_COL
										+ " = ? and "
										+ SOAConstants.NXX_COL
										+ " = ? and "
										+ SOAConstants.DASHX_COL
										+ " = ?";

	/**
	 * The SQL select statement used to get customer Id
	 * from  CUSTOMER_LOOKUP table based on spid.
	 */
	private static final String SELECT = "select "
										+ SOAConstants.CUSTOMERID_COL
										+ " from "
										+ SOAConstants.CUSTOMER_LOOKUP_TABLE
										+ " where "
										+ SOAConstants.TPID_COL
										+ " = ? ";

	/**
	 * The SQL insert statement used to insert record
	 * in  LAST_NPAC_RESPONSE_TIME table .
	 */
	private static final String INSERT_LAST_NOTIF_TIME = "insert into "
									+ SOAConstants.LAST_NPAC_RESPONSE_TIME_TABLE
									+ " values( ?, ?, ?, ?) ";

	/**
	 * The SQL Select statement used to get Sequence ID.	 
	 */
	private static final String SELECT_SEQUENCE =
								"Select NEUSTARFULLSOAREFKEY.nextVal from dual";

	/**
	 * This is the precompiled SQL statement that  will
	 * be used to insert  data into the SV TEMP table.
	 */
	private PreparedStatement tempInsertStatement;
	
	/**
	 * This is the precompiled SQL statement that  will
	 * be used to insert  data into the LAST_NPAC_RESPONSE_TIME table.
	 */
	private PreparedStatement insertStatement;

	/**
	 * This is the precompiled SQL statement that  will
	 * be used to select customer Id from CUSTOMER_LOOKUP table.
	 */
	private PreparedStatement selectCusIdStatement;

	/**
	 * This is the precompiled SQL statement that  will
	 * be used to select Sequence Id from DUAL table.
	 */
	private PreparedStatement selectSeqStatement;
	
	/**
	 * this variable contains records skipped
	 */
	protected  int recordSkipped = 0;

	/**
	 * This is used to insert customer ID in SV records.
	 */
	private String customerID = "DEFAULT";

	/**
	 * This consumer will only process SV's belonging to this SPID.
	 */
	private String spid;

	/**
	 * This identifies the region that for the data is being imported.
	 * This will be set into the SOA_SUBSCRIPTION_VERSION table.
	 * This is an int value from 0 to 7.
	 */
	private String region;

	/**
	 * This will give Last Notification Time stamp to be insert into
	 * LAST_NPAC_RESPONSE_TIME
	 */

	private Date lastNotificationTS;

	/**
	 * Constructor.
	 *
	 * @param spid String the SPID if the service provider whose data we
	 *                    are importing. SV data not belonging to this
	 *                    SPID will be ignored.
	 * @param region String the index of the region for the SV
	 *                      data being processed.
	 * @param	svBDD	boolean 
	 * @throws NumberFormatException if the given region value is not numeric
	 *                               or if it is not in the valid range of
	 *                               regions from 0 to 7.
	 */
	public SVTokenConsumer(String spid, String region, boolean svBDD)
											throws NumberFormatException {

		this.spid = spid;

		super.svBDD = svBDD;

		int regionValue = Integer.parseInt(region);

		if (regionValue < 0 || regionValue >= NPACConstants.REGION_COUNT) {

			throw new NumberFormatException(
				"[" + region + "] is not a valid region.");

		}

		this.region = region;

	}

	/**
	 *
	 * @param filename String the filename is given because it may contain
	 *                        important date information.
	 * @throws FrameworkException
	 */
	public void init(String filename) throws FrameworkException {

		// initialize the DB connection
		super.init(filename);

		try {

			tempInsertStatement = prepareStatement(INSERT_TEMP);

		} catch (SQLException sqlex) {

			Debug.error(
				"Could not prepare insert statement for sv temp table:\n"
					+ INSERT_TEMP
					+ "\n"
					+ sqlex);
		
		}
		
		try {

			selectCusIdStatement = prepareStatement(SELECT);

		} catch (SQLException sqlex) {

			Debug.error(
				"Could not prepare Select statement for customer Id:\n"
					+ SELECT
					+ "\n"
					+ sqlex);
			
		}

		try {

			insertStatement = prepareStatement(INSERT_LAST_NOTIF_TIME);

		} catch (SQLException sqlex) {

			Debug.error(
				"Could not prepare insert statement for "
					+ " LAST_NPAC_RESPONSE_TIME:\n"
					+ INSERT_LAST_NOTIF_TIME
					+ "\n"
					+ sqlex);

		}

		try {

			selectSeqStatement = prepareStatement(SELECT_SEQUENCE);

		} catch (SQLException sqlex) {

			Debug.error(
				"Could not prepare select statement for Sequence no:\n"
					+ SELECT_SEQUENCE
					+ "\n"
					+ sqlex);

		}

	}
	
	/**
	 * Called to process a line of data from a delimited file. This first
	 * tries to insert the data into the database. If this fails (perhaps
	 * because the row already exists), then an attempt is made to update
	 * the row instead.
	 *
	 * @param tokens String[] the tokenized line of input.
	 * @throws FrameworkException if the attempt to insert and the attempt
	 *                            to update both fail.
	 */
	public boolean process(String[] tokens, String tokenType) throws FrameworkException{
	
		boolean errorFlag = false;
	
		try 
		{
	
			//	check to see if we want to process this particular line
			// of tokens
			if( ! accept( tokens ) ){

				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					 Debug.log(Debug.MSG_STATUS,
							   "Skipping tokens: "+
							   DelimitedFileReader.getString(tokens));
				}
				
				recordSkipped++;
				
				
				return errorFlag ;

			}else
			{
				// try to insert in sv temp table , inserting record to check
				// duplicate
				setTempInsertParams( tempInsertStatement , tokens );
				
				tempInsertStatement.execute();
			}
     
        
		}catch (SQLException sqlex) 
		{

			 if (Debug.isLevelEnabled(Debug.DB_STATUS)) {

			   Debug.log(Debug.DB_STATUS,
						 "Insert failed: " +
						 sqlex.getMessage() + "\nAdding in failed List ...");
	
				errorFlag = true;
				
				tokens[ tokens.length - 2 ] = sqlex.getMessage().trim();
			
				failedList.add(tokens);
				
				return errorFlag ;

			}
			
			
	 	}
		 	
			
		errorFlag = super.process( tokens, null );	  
  
	  	return errorFlag;

   }

	/**
	 * Returns the SQL statement used to insert a full row of data into the
	 * SOA_SUBSCRIPTION_VERSION table.
	 *
	 * @return String the SQL insert statement.
	 */
	protected String getInsertSQL() {

		return INSERT;

	}

	/**
	 * Returns the SQL statement used to update a row of data in the
	 * SOA_NPANXX_X table.
	 *
	 * @return String the SQL update statement.
	 */
	protected String getUpdateSQL() {

		return UPDATE;

	}
	
	/**
	 * This method is called by the parent class to determine if it
	 * should process the given tokens or not. This is used to filter out
	 * any SV data that does not belong to this consumer's SPID.
	 *
	 * @param tokens String[] a line of tokens from a data file.
	 * @return boolean true if the tokens should be processed by this consumer
	 *                 and false otherwise.
	 */
	private boolean accept(String[] tokens) {

		// If the token NNSP or ONSP value is the same as our SPID value,
		// then we return true, meaning that we do want to process this row.		
		return tokens[3].equals(spid) || tokens[21].equals(spid) ;

	}

	/**
	 * This method is used to set spid in to preparedStatement which is
	 * is used to select Customer ID.
	 *
	 * @param selectStatement PreparedStatement the precompiled select statement.
	 * @throws SQLException if an error occurs while setting the parameter
	 *                      values on the insert statement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date. This is not expected
	 *                            to happen.
	 */
	protected void setSelectParams(PreparedStatement selectStatement)
		throws SQLException, FrameworkException {

		//SPID	   
		selectStatement.setString(1, spid);

	}

	/**
	 * This method is used to set values in to prepared statement which is used
	 * is used to insert records in last Notification table.
	 *
	 * @param insertStatement PreparedStatement the precompiled insert statement.
	 * @throws SQLException if an error occurs while setting the parameter
	 *                      values on the insert statement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date. This is not expected
	 *                            to happen.
	 */
	protected void setLastNotifTimeInsertParams(
									PreparedStatement insertStatement)
									throws SQLException, FrameworkException {
		
		//SPID
		insertStatement.setString(1, spid);

		//REGION
		insertStatement.setString(2, region);
		
		//DATETIME
		insertStatement.setTimestamp(
			3,
			new Timestamp(lastNotificationTS.getTime()));
			
		//	TYPE
		insertStatement.setInt( 4, 0 );

	}

	/**
	 * This method is used to set values in to prepared statement which is
	 * used to insert record in to SV table
	 *
	 * @param insertStatement PreparedStatement the precompiled insert statement.
	 * @param params String[] The tokens from a line of input. These
	 *                        values are used as the parameters of the
	 *                        insertStatement.
	 * @throws SQLException if an error occurs while setting the parameter
	 *                      values on the insert statement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date. This is not expected
	 *                            to happen.
	 */
	protected void setInsertParams(
								PreparedStatement insertStatement,
								String[] params)
								throws SQLException, FrameworkException {
			
		int column = 1;

		// CUSTOMERID
		insertStatement.setString(column, customerID);

		//Now reference key is the perimary field it has to insert
		//  so getting message key
		int refNo = getSequenceID();

		// REFERENCE KEY
		insertStatement.setInt(++column, refNo);

		// SPID Value
		insertStatement.setString(++column, spid);

		// REGIONID ID
		insertStatement.setString(++column, region);

		// NNSP
		insertStatement.setString(++column, params[3]);

		// ONSP
		insertStatement.setString(++column, params[21]);

		// LNPTYPE
		insertStatement.setString(++column, SOAUtility.getLnpType( params[16]) );

		// PORTINGTN
		StringBuffer tn = new StringBuffer(params[1]);
		if (!tn.toString().equals("")) {

			tn.insert(3, "-").insert(7, "-");

		}

		insertStatement.setString(++column, tn.toString());

		// NNSP_DUEDATE
		Date nnspDueDate = parseDate(params[22]);
		if (nnspDueDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(nnspDueDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// ONSP_DUEDATE
		Date onspDueDate = parseDate(params[23]);
		if (onspDueDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(onspDueDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// SVID
		insertStatement.setString(++column, params[0]);

		// LRN
		insertStatement.setString(++column, params[2]);

		// CLASSDPC
		insertStatement.setString(++column, params[5]);

		// CLASSSSN
		insertStatement.setString(++column, params[6]);

		// CNAMDPC
		insertStatement.setString(++column, params[11]);

		// CNAMSSN
		insertStatement.setString(++column, params[12]);

		// ISVMDPC
		insertStatement.setString(++column, params[9]);

		// ISVMSSN
		insertStatement.setString(++column, params[10]);

		// LIDBDPC
		insertStatement.setString(++column, params[7]);

		// LIDBSSN
		insertStatement.setString(++column, params[8]);

		// WSMSCDPC
		insertStatement.setString(++column, params[18]);

		// WSMSCSSN
		insertStatement.setString(++column, params[19]);

		// PORTINGTOORIGINAL
		insertStatement.setString(++column, params[36]);

		// BILLINGID
		insertStatement.setString(++column, params[15]);

		// ENDUSERLOCATIONTYPE
		insertStatement.setString(++column, params[14]);

		// ENDUSERLOCATIONVALUE
		insertStatement.setString(++column, params[13]);

		// AUTHORIZATIONFLAG
		insertStatement.setString(++column, params[24]);

		// CAUSECODE
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

		// CONFLICTDATE
		Date conflictDate = parseDate(params[27]);
		if (conflictDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(conflictDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// AUTHORIZATIONDATE
		Date authorizationDate = parseDate(params[25]);
		if (authorizationDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(authorizationDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// CUSTOMERDISCONNECTDATE
		Date customerDisconnectDate = parseDate(params[40]);
		if (customerDisconnectDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(customerDisconnectDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// EFFECTIVERELEASEDATE 
		insertStatement.setNull(++column, java.sql.Types.DATE);

		// STATUS
		insertStatement.setString(++column, SOAUtility.getStatus( params[20] ));

		// Last Response
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

		// Last Response Date
		insertStatement.setNull(++column, java.sql.Types.DATE);

		// COMPLETEDDATE
		Date completedDate = parseDate(params[28]);
		if (completedDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(completedDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// ACTIVATEDDATE
		Date activatedDate = parseDate(params[26]);
		if (activatedDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(activatedDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// Last Request Type
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

		// Last Request Date 
		insertStatement.setNull(++column, java.sql.Types.DATE);

		// LASTMESSAGE		
		// if status is active then set last message "SvActivateNotification" otherwise
		// "SvStatusChangeNotification"
		if( params[20].equals("1"))
		{
			
			insertStatement.setString(++column, SOAConstants.SV_ACTIVATE_NOTIFICATION );
			
		}else
		{
			
			insertStatement.setString(++column, SOAConstants.SV_STS_CHANGE_NOTIFICATION );
			
		}		

		// OBJECTCREATIONDATE
		Date creationDate = parseDate(params[31]);
		if (creationDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(creationDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// OBJECTMODIFIEDDATE
		Date modifiedDate = parseDate(params[30]);
		if (modifiedDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(modifiedDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// PRECANCELLATIONSTATUS
		insertStatement.setString(++column, SOAUtility.getStatus( params[39] ));

		// STATUSOLDDATE
		Date statusOldDate = parseDate(params[29]);
		if (statusOldDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(statusOldDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		//	OLDSPCONFLICTRESOLUTIONDATE
		Date oldSPconflictResolutionDate = parseDate(params[34]);
		if (oldSPconflictResolutionDate != null) {
		
			insertStatement.setTimestamp(
			++column,
			new Timestamp(oldSPconflictResolutionDate.getTime()));
		
		} else {
		
			insertStatement.setNull(++column, java.sql.Types.DATE);
		
		}
		
		// NEWSPCONFLICTRESOLUTIONDATE
		Date newSPconflictResolutionDate = parseDate(params[35]);
		if (newSPconflictResolutionDate != null) {
		
			insertStatement.setTimestamp(
				++column,
				new Timestamp(newSPconflictResolutionDate.getTime()));
		
		} else {
		
			insertStatement.setNull(++column, java.sql.Types.DATE);
		
		}
		
		// OLDSPCANCELETIONDATE 
		Date oldCanceletionDate = parseDate(params[32]);
		if (oldCanceletionDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(oldCanceletionDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		// NEWSPCANCELETIONDATE
		Date newCanceletionDate = parseDate(params[33]);
		if (newCanceletionDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(newCanceletionDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}

		//CANCELEDDATE
		insertStatement.setNull(++column, java.sql.Types.DATE);

		//BUSINESSTMERTYPE
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

		//BUSINESSHOURS
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

		//FAILEDSPFLAG
		insertStatement.setNull(++column, java.sql.Types.INTEGER);

		// Created By
		insertStatement.setString(++column, SOAConstants.SYSTEM_USER);

		// Created Date
		// use current time
		insertStatement.setTimestamp(
			++column,
			new Timestamp(System.currentTimeMillis()));

		//This function is called here to set max notification time for current 
		// record                             
		getMaxLastNotification(params);
		
		
		// SV Type
		if (params.length>40)
		{
			if (params[41] == null || params[41].equals(""))
			{
				insertStatement.setNull(++column,java.sql.Types.VARCHAR);
			}
			else{
				insertStatement.setString(++column,SOAUtility.getSVType(params[41]));
			}
		}
		else{
				insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		}

		//Alternative SPID
		if (params.length>41)
		{
			if (params[42] == null || params[42].equals(""))
			{
				insertStatement.setNull(++column, java.sql.Types.VARCHAR);
			}
			else{
				insertStatement.setString(++column,params[42]);
			}
		}
		else{
				insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		}
		
		
		
	}
	
	/**
	 * This method is used to set values in to prepared statement which is
	 * used to insert record in to SV Temp table
	 *
	 * @param insertStatement PreparedStatement the precompiled insert statement.
	 * @param params String[] The tokens from a line of input. These
	 *                        values are used as the parameters of the
	 *                        insertStatement.
	 * @throws SQLException if an error occurs while setting the parameter
	 *                      values on the insert statement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date. This is not expected
	 *                            to happen.
	 */
	protected void setTempInsertParams(PreparedStatement insertStatement,
										String[] params)
										throws SQLException,FrameworkException{

		int column = 1;

		// CUSTOMERID
		insertStatement.setString(column, customerID);
		
		

		// SPID Value
		insertStatement.setString(++column, spid);
		
		// REGIONID ID
		insertStatement.setString(++column, region);
		
		// NNSP
		insertStatement.setString(++column, params[3]);
		
		// ONSP
		insertStatement.setString(++column, params[21]);
		
		// LNPTYPE
		insertStatement.setString(++column, SOAUtility.getLnpType( params[16]) );
		
		// PORTINGTN
		StringBuffer tn = new StringBuffer(params[1]);
		if (!tn.toString().equals("")) {

			tn.insert(3, "-").insert(7, "-");

		}

		insertStatement.setString(++column, tn.toString());
		
		// NNSP_DUEDATE
		Date nnspDueDate = parseDate(params[22]);
		if (nnspDueDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(nnspDueDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// ONSP_DUEDATE
		Date onspDueDate = parseDate(params[23]);
		if (onspDueDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(onspDueDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// SVID
		insertStatement.setString(++column, params[0]);
		
		// LRN
		insertStatement.setString(++column, params[2]);
		
		// CLASSDPC
		insertStatement.setString(++column, params[5]);
		
		// CLASSSSN
		insertStatement.setString(++column, params[6]);
		
		// CNAMDPC
		insertStatement.setString(++column, params[11]);
		
		// CNAMSSN
		insertStatement.setString(++column, params[12]);
		
		// ISVMDPC
		insertStatement.setString(++column, params[9]);
		
		// ISVMSSN
		insertStatement.setString(++column, params[10]);
		
		// LIDBDPC
		insertStatement.setString(++column, params[7]);
		
		// LIDBSSN
		insertStatement.setString(++column, params[8]);
		
		// WSMSCDPC
		insertStatement.setString(++column, params[18]);
		
		// WSMSCSSN
		insertStatement.setString(++column, params[19]);
		
		// PORTINGTOORIGINAL
		insertStatement.setString(++column, params[36]);
		
		// BILLINGID
		insertStatement.setString(++column, params[15]);
		
		// ENDUSERLOCATIONTYPE
		insertStatement.setString(++column, params[14]);
		
		// ENDUSERLOCATIONVALUE
		insertStatement.setString(++column, params[13]);
		
		// AUTHORIZATIONFLAG
		insertStatement.setString(++column, params[24]);
		
		// CAUSECODE
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		
		// CONFLICTDATE
		Date conflictDate = parseDate(params[27]);
		if (conflictDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(conflictDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// AUTHORIZATIONDATE
		Date authorizationDate = parseDate(params[25]);
		if (authorizationDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(authorizationDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// CUSTOMERDISCONNECTDATE
		Date customerDisconnectDate = parseDate(params[40]);
		if (customerDisconnectDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(customerDisconnectDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// EFFECTIVERELEASEDATE 
		insertStatement.setNull(++column, java.sql.Types.DATE);
		
		// STATUS
		insertStatement.setString(++column, SOAUtility.getStatus( params[20] ));
		
		// Last Response
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		
		// Last Response Date
		insertStatement.setNull(++column, java.sql.Types.DATE);
		
		// COMPLETEDDATE
		Date completedDate = parseDate(params[28]);
		if (completedDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(completedDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// ACTIVATEDDATE
		Date activatedDate = parseDate(params[26]);
		if (activatedDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(activatedDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// Last Request Type
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		
		// Last Request Date 
		insertStatement.setNull(++column, java.sql.Types.DATE);
		
		// LASTMESSAGE
		// if status is active then set last message "SvActivateNotification" otherwise
		// "SvStatusChangeNotification"
		if( params[20].equals("1"))
		{
			
			insertStatement.setString(++column, SOAConstants.SV_ACTIVATE_NOTIFICATION );
			
		}else
		{
			
			insertStatement.setString(++column, SOAConstants.SV_STS_CHANGE_NOTIFICATION );
			
		}
		
		// OBJECTCREATIONDATE
		Date creationDate = parseDate(params[31]);
		if (creationDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(creationDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// OBJECTMODIFIEDDATE
		Date modifiedDate = parseDate(params[30]);
		if (modifiedDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(modifiedDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// PRECANCELLATIONSTATUS
		insertStatement.setString(++column, SOAUtility.getStatus( params[39] ));
		
		// STATUSOLDDATE
		Date statusOldDate = parseDate(params[29]);
		if (statusOldDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(statusOldDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		//	OLDSPCONFLICTRESOLUTIONDATE
		Date oldSPconflictResolutionDate = parseDate(params[34]);
		if (oldSPconflictResolutionDate != null) {
		
			insertStatement.setTimestamp(
			++column,
			new Timestamp(oldSPconflictResolutionDate.getTime()));
		
		} else {
		
			insertStatement.setNull(++column, java.sql.Types.DATE);
		
		}
		
		// NEWSPCONFLICTRESOLUTIONDATE
		Date newSPconflictResolutionDate = parseDate(params[35]);
		if (newSPconflictResolutionDate != null) {
		
			insertStatement.setTimestamp(
				++column,
				new Timestamp(newSPconflictResolutionDate.getTime()));
		
		} else {
		
			insertStatement.setNull(++column, java.sql.Types.DATE);
		
		}
		
		// OLDSPCANCELETIONDATE 
		Date oldCanceletionDate = parseDate(params[32]);
		if (oldCanceletionDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(oldCanceletionDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		// NEWSPCANCELETIONDATE
		Date newCanceletionDate = parseDate(params[33]);
		if (newCanceletionDate != null) {

			insertStatement.setTimestamp(
				++column,
				new Timestamp(newCanceletionDate.getTime()));

		} else {

			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		//CANCELEDDATE
		insertStatement.setNull(++column, java.sql.Types.DATE);
		
		//BUSINESSTMERTYPE
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		
		//BUSINESSHOURS
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		
		//FAILEDSPFLAG
		insertStatement.setNull(++column, java.sql.Types.INTEGER);
		
		// Created By
		insertStatement.setString(++column, SOAConstants.SYSTEM_USER);
		
		// Created Date
		// use current time
		insertStatement.setTimestamp(
			++column,
			new Timestamp(System.currentTimeMillis()));

		// SV Type
		if (params.length>40)
		{
			if (params[41] == null || params[41].equals(""))
			{
				insertStatement.setNull(++column,java.sql.Types.VARCHAR);
			}
			else{
				insertStatement.setString(++column,SOAUtility.getSVType(params[41]));
			}
		}
		else{
				insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		}

		//Alternative SPID
		if (params.length>41)
		{
			if (params[42] == null || params[42].equals(""))
			{
				insertStatement.setNull(++column, java.sql.Types.VARCHAR);
			}
			else{
				insertStatement.setString(++column,params[42]);
			}
		}
		else{
				insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		}
		
		
			
	}

	/**
	 * This method will be used to update Activate Flag in NPA_NXX_X table
	 *
	 * @param updateStatement PreparedStatement the precompiled update statement.
	 * @param params String[] The tokens from a line of input. These
	 *                        values are used to populate parameters of the
	 *                        updateStatement.
	 * @throws SQLException if an error occurs while setting the parameter
	 *                      values on the update statement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date. This is not expected
	 *                            to happen.
	 */
	protected void setUpdateParams(
								PreparedStatement updateStatement,
								String[] params)
								throws SQLException, FrameworkException {

		int column = 1;

		// SPID
		updateStatement.setString(column, params[3]);

		// NPA
		updateStatement.setString(++column, params[1].substring(0, 3));

		// NXX
		updateStatement.setString(++column, params[1].substring(3, 6));

		//DASHX
		updateStatement.setString(++column, params[1].substring(6, 7));

	}

	/**
	 * This method will be used to to get the Max last notification time in
	 * current record and set it globally
	 *    
	 * @param params String[] The tokens from a line of input. These
	 *                        values are used to populate parameters of the
	 *                        updateStatement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date. This is not expected
	 *                            to happen.
	 */
	private void getMaxLastNotification(String[] params)
											throws FrameworkException {
		
		
		//Creating date array for all time stamps in a SV records
		Date dates[] =
			{
				parseDate(params[22]),
				parseDate(params[23]),
				parseDate(params[25]),
				parseDate(params[26]),
				parseDate(params[27]),
				parseDate(params[28]),
				parseDate(params[29]),
				parseDate(params[30]),
				parseDate(params[31]),
				parseDate(params[32]),
				parseDate(params[33]),
				parseDate(params[34]),
				parseDate(params[35]),
				parseDate(params[37]),
				parseDate(params[38]),
				parseDate(params[40])};

		List datesList = new LinkedList();

		// ignoring null
		for (int i = 0; i < dates.length; i++) {

			if (dates[i] != null)
				datesList.add(dates[i]);

		}

		// creating new date array which will contain Dates only,not null values
		Date lastNotifDates[] = new Date[datesList.size()];

		datesList.toArray(lastNotifDates);

		// Sorting Date array in ascending order
		Arrays.sort(lastNotifDates);

		if (lastNotificationTS != null) {

			if (lastNotificationTS
				.before(lastNotifDates[lastNotifDates.length - 1])) {
				lastNotificationTS = lastNotifDates[lastNotifDates.length - 1];

			}

		} else {

			lastNotificationTS = lastNotifDates[lastNotifDates.length - 1];
		}

	}

	/**
	 * This method will be used to insert record 
	 * into Last Notification time table.
	 *
	 * @throws SQLException if an error occurs while setting the parameter
	 *                      values on the update statement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date.
	 */
	public void setLastNotificationTime( )
							throws SQLException, FrameworkException {
		try {
			
			if( lastNotificationTS != null )
			{
			
				setLastNotifTimeInsertParams(insertStatement);
		
				insertStatement.execute();
				
			}

		} catch (SQLException sqlEx) {

			String error =
				"Could not insert data into "
					+ " LAST_NPAC_RESPONSE_TIME table : "
					+ sqlEx;

			Debug.error(error);		

		}

	}

	/**
	 * This method will be used to get customer id.
	 *
	 * @throws SQLException if an error occurs while setting the parameter
	 *                      values on the update statement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date. This is not expected
	 *                            to happen.
	 */

	public void setCustometID() throws SQLException, FrameworkException {

		ResultSet rs = null;

		try {

			setSelectParams(selectCusIdStatement);

			rs = selectCusIdStatement.executeQuery();

			if (rs.next())
				this.customerID = rs.getString(1);

		} catch (SQLException sqlex) {

			String error = "Could not get Customer ID from DB : " + sqlex;

			Debug.error(error);			

		}
		
		rs.close();
	}

	/**
	 * This method will be used to get sequence id.
	 *
	 * @throws SQLException if an error occurs while setting the parameter
	 *                      values on the update statement.
	 * @throws FrameworkException if one of the params is in an invalid format.
	 *                            For example, a bad date. This is not expected
	 *                            to happen.
	 */

	protected int getSequenceID() throws SQLException, FrameworkException {

		ResultSet rs = null;

		int seqNo = -1;

		try {

			rs = selectSeqStatement.executeQuery();

			if (rs.next())
				seqNo = rs.getInt(1);

		} catch (SQLException sqlex) {

			rs.close();

			String error = "Could not get Sequence ID : " + sqlex;

			Debug.error(error);

			throw new FrameworkException(error);

		}

		rs.close();

		return seqNo;

	}

	/**
	 * Closes prepared statements and releases the database connection.
	 *
	 * @param success boolean whether or not the processing succeeded. This
	 *                        is used to determine whether we should rollback
	 *                        or commit.
	 */
	public void cleanup(boolean success) {
		
		
		try {
			
			tempInsertStatement.close();

		} catch (SQLException sqlex) {

			Debug.error("Could not close temp insert statement:\n" + sqlex);

		}
		try {

			selectCusIdStatement.close();

		} catch (SQLException sqlex) {

			Debug.error(
				"Could not close select customer id statement:\n" + sqlex);

		}
		
		try {
			
			insertStatement.close();

		} catch (SQLException sqlex) {

			Debug.error("Could not close insert statement:\n" + sqlex);

		}

		try {

			selectSeqStatement.close();

		} catch (SQLException sqlex) {

			Debug.error(
				"Could not close select Seq Number statement:\n" + sqlex);

		}

		// release connection
		super.cleanup(success);

	}
	
	/**
	 * This method will return array of records which contains no of
	 * updated record in base table , Sv table and NPB table
	 * 
	 * @return int[]
	 */
	public int[] getRecords()
	{
		int [] recordsArr = new int[3];

		recordsArr[0] = recordSkipped;

		recordsArr[1] = 0 ;

		recordsArr[2] = 0 ;

		return recordsArr;
   
	}
	  
}
