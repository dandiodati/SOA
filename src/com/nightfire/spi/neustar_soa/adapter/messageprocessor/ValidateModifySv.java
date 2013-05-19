/**
 * The purpose of this processor is to perform ModifySv Request validations
 * according to the status the and if validation fails, generate Error XML.
 *
 * @author Jigar Talati
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.framework.db.DBConnectionPool;
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.framework.db.DatabaseException;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.message.parser.MessageParserException;
 * @see com.nightfire.framework.message.parser.xml.XMLMessageParser;
 * @see com.nightfire.framework.resource.ResourceException;
 * @see com.nightfire.framework.rules.ErrorCollection;
 * @see com.nightfire.framework.rules.RuleError;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.FrameworkException;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.framework.util.StringUtils;
 * @see com.nightfire.spi.common.driver.MessageObject;
 * @see com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants;
 *
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------		--------------------------
	1			Jigar Talati	10/19/2004		Created
	2			Jigar Talati	10/21/2004		Review comments incorporated.
*/


package com.nightfire.spi.neustar_soa.adapter.messageprocessor;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.MessageParserException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.rules.ErrorCollection;
import com.nightfire.framework.rules.RuleError;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class ValidateModifySv extends DBMessageProcessorBase {
	
	/**
	 * The value of InputMessage.
	 */
	private String inputMsg = null;
	
	/**
	  * The value of SPID Location
	  */
	private String spidLocation = null;
	
	/**
	  * The value of SPID
	  */
	private String spidValue = null;
	
	/**
	  * The value of SVID Location
	  */
	private String svIdLocation = null;
	
	/**
	  * The value of RegionId
	  */
	private String regionIdValue = null;
	
	/**
	  * The value of RegionId Location
	  */
	private String regionIdLocation = null;

	/**
	 * The value of npa Location
	 */
	private String npaLocation = null;

	/**
	 * The value of nxx Location
	 */
	private String nxxLocation = null;
	

	/**
	 * The value of start TN Location
	 */
	private String startTNLocation = null;

	/**
	 * The value of end TN Location
	 */
	private String endTNLocation = null;

	/**
	 * To seperate column
	 */ 
	private String separator = null;

	/**
	 * To store error collection
	 */ 
	private String errorLocation = null;

	/**
	 * This variable used to get value for tnDelimiter.
	 */	
	private String tnDelimiter = null;
	
	/**
	 * The value of flag 
	 *
	 */
	private boolean flag = true;
	
	/**
	 * The value of NewSP 
	 *
	 */
	private String newSP = null;
	
	/**
	 * The value of OldSP 
	 *
	 */
	private String oldSP = null;
	
	/**
	 * The value of status
	 *
	 */
	private String status = null;
	
	/**
	 * The value of PortingToOriginal
	 *
	 */
	boolean portingToOriginal = false;
	
	/**
	 * The value of Error Collection.
	 *
	 */
	Collection errorCol = null;
	
	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;

	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;	
	
    /**
     * Constructor.
     */
    public ValidateModifySv ( )
    {

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log( Debug.OBJECT_LIFECYCLE,
							"Creating ValidateModifySv message-processor." );
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
	public void initialize ( String key, String type )throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        // Get configuration properties specific to this processor.
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "ValidateModifySv: Initializing..." );
		}

        StringBuffer errorBuffer = new StringBuffer( );
        
        inputMsg = getRequiredPropertyValue(SOAConstants.INPUT_MSG_LOC, errorBuffer);
		
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
					"Location of Input Message [" + inputMsg + "]." );
		}

		separator = getPropertyValue( SOAConstants.LOCATION_SEPARATOR_PROP );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of separotor is ["
														+ separator + "]." ); 
		}
														          					   
        if ( !StringUtils.hasValue( separator ) ){
        
			separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;
        
        }

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
							"Location separator token [" + separator + "]." );
		}
		
		spidLocation = getRequiredPropertyValue( SOAConstants.SPID_PROP, 
																errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of SPID is ["
														+ spidLocation + "]." );
		}
		
		svIdLocation = getPropertyValue( SOAConstants.INPUT_LOC_SVID_PROP );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of SVID is ["
														+ svIdLocation + "]." );
		}
		
		regionIdLocation = getPropertyValue( SOAConstants.REGION_ID_PROP );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of RegionID is [" 
													 	+ regionIdLocation + "]." );
		}

		npaLocation = getPropertyValue( SOAConstants.NPA_PROP );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of NPA is ["	
														+ npaLocation + "]." );
		}

		nxxLocation = getPropertyValue( SOAConstants.NXX_PROP );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of NXX is ["	
														+ nxxLocation + "]." );
		}

		startTNLocation = getPropertyValue( SOAConstants.START_TN_PROP );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of start TN is ["
													  +startTNLocation + "]." );
		}

		endTNLocation = getPropertyValue( SOAConstants.END_TN_PROP );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of end TN is ["
													   + endTNLocation + "]." );
		}

		tnDelimiter = getPropertyValue( SOAConstants.TN_DELIMITER_PROP );

		if (!StringUtils.hasValue(tnDelimiter))
		{
		
			tnDelimiter = SOAConstants.DEFAULT_DELIMITER;
			
		}

		errorLocation = getRequiredPropertyValue(
						SOAConstants.ERROR_RESULT_LOCATION_PROP, errorBuffer);
		
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
					"Location of error  [" + errorLocation + "]." );
		}

        // If any of the required properties are absent,
        // indicate error to caller
        if ( errorBuffer.length() > 0 )
        {

            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );

        }

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,"ValidateModifySv: " +
        											"Initialization done." );
		}

    }


    /**
     * Extract data values from the context/input, and use them to
     * query for rows from a database table.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext,
    						  MessageObject inputObject )
                        					throws MessageException,
                        						   ProcessingException
    {
		
    	ThreadMonitor.ThreadInfo tmti = null;
		if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
			Debug.log( Debug.MSG_STATUS, "ValidateModifySv: processing ... " );
		}

		if ( inputObject == null )
		{
		
            return null;
            
		}
		
		this.mpContext = mpContext ;
		
		this.inputObject = inputObject ;
		
		flag = true; 
		
		//The value of result
		String result = null;
		
		// The value of selectSql
		String selectSql = null;
		
		// The value of dbConn
		Connection dbConn = null;
				
		// The value of SVID
		String svIdValue = null;

		// The value of npa
		String npaValue = null;

		// The value of nxx
		String nxxValue = null;

		// The value of start TN
		String startTNValue = null;

		// The value of end TN
		String endTNValue = null;
		
		// The value of input message
		String inputDoc = null;
		
		StringBuffer tn = null;

		PreparedStatement pstmt = null;
		
		if (svIdLocation != null)
		{
			svIdValue =  getValue( svIdLocation );

			if ( svIdValue != null)
			{
				flag = false;
			}
		}

		if (regionIdLocation != null)
		{
			regionIdValue =  getValue( regionIdLocation );
		}

		if (npaLocation != null)
		{
			npaValue =  getValue( npaLocation );
		}

		if (nxxLocation != null)
		{
			nxxValue =  getValue( nxxLocation );
		}
		
		if (startTNLocation != null)
		{
			startTNValue =  getValue( startTNLocation );
						
		}
		
		if (endTNLocation != null)
		{
			endTNValue =  getValue( endTNLocation );
			
		}
		
		if( startTNValue != null && endTNValue == null ) {
			
			endTNValue = startTNValue;
			
		}
		
		inputDoc = super.getString( inputMsg, mpContext, inputObject );
		
		spidValue = super.getString( spidLocation,mpContext, inputObject ); 

		errorCol = new ArrayList();
		
		try
        {
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			// Get a database connection from the context.			

			dbConn = mpContext.getDBConnection( );

			if( dbConn == null )
			{
				
				throw new ProcessingException("Connection is not acquired ," +
														"it is null ");
				
			}
			
			selectSql = constructSqlStatement(flag);

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"SELECT STATEMENT : " + selectSql );
			}
			
			// Get a prepared statement for the SQL statement.
			pstmt = dbConn.prepareStatement( selectSql );														
			
			if(svIdValue != null)
			{

				populateSelectSql(pstmt, svIdValue);
		
				result = validateModify(inputDoc);
		
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log(Debug.SYSTEM_CONFIG, "Error XML : "+result);
				}

			}
			
			else if( startTNValue != null )
			
			{
				int startTN = 0;

				int endTN = 0;

				try
				{

					startTN = Integer.parseInt(startTNValue);

					endTN = Integer.parseInt(endTNValue);

				}
				catch(NumberFormatException nbrfex)
				{
					
					throw new MessageException("Invalid start TN & end TN: " 
																	+ nbrfex);

				}

				// for each TN
				for (int i = startTN; i <= endTN; i++ )
				{
					tn = new StringBuffer();
					
					tn.append( npaValue );
					
					tn.append( tnDelimiter );
					
					tn.append( nxxValue );
					
					tn.append( tnDelimiter );
					
					tn.append( StringUtils.padNumber( i, 
										SOAConstants.TN_LINE, true, '0' ) );
					
					
					if (Debug.isLevelEnabled(Debug.MSG_STATUS))
					{

						Debug.log(Debug.MSG_STATUS, 
										"Logging TN: [" + tn.toString() + "]");

					}
					
					populateSelectSql(pstmt, tn.toString());
					
					result = validateModify(inputDoc);
									
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log(Debug.SYSTEM_CONFIG, "Error XML : "+result);
					}
					
					if(result != null){
						
						break;
						
					}

				} // END OF FOR LOOP
				
			}

			// Place the result in the configured location.
			if ( result != null )
			{
				set(errorLocation, mpContext, inputObject, result);
			}
			
		}

		catch ( FrameworkException e )
		{
			String errMsg = "ERROR: ValidateModifySv : "+ e.getMessage();

			Debug.log( Debug.ALL_ERRORS, errMsg );

			if(e instanceof MessageParserException){
				
				throw ( MessageParserException )e;
				
			}else if( e instanceof MessageException ){
			
				throw ( MessageException )e;
				
			}else {
				
				throw new ProcessingException( errMsg );
				
			}
			
		}
		catch(SQLException se){
			
			Debug.log( Debug.ALL_ERRORS, se.toString() );
			
		}
			
		finally
		{
			ThreadMonitor.stop(tmti);
			try
			{				
				if ( pstmt != null )
				{
				  pstmt.close( );
				  
				  pstmt = null;
				  				 
				}
			}
			catch ( SQLException sqle )
			{

				Debug.log( Debug.ALL_ERRORS, "Unable to close the statement" );

			}			
			
		}

		if( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
			Debug.log( Debug.MSG_DATA, "Modify Request Validation success  ");
		}

        // Pass the input on to the output.
        return( formatNVPair( inputObject ) );

    }

	/**
	 * Construct the SQL SELECT statement.
	 * 
	 * @return SQL INSERT statement.
	 * 
	 */
	private String constructSqlStatement(boolean flag) {
										
		if( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log(Debug.DB_DATA, "Constructing SQL SELECT statement ...");
		}

		StringBuffer sb = new StringBuffer();
		
		sb.append("SELECT ");

		sb.append(SOAConstants.NNSP_COL);

		sb.append(" , ");
		
		sb.append(SOAConstants.ONSP_COL);
		
		sb.append(" , ");
		
		sb.append(SOAConstants.PORTINGTOORIGINAL_COL);
		
		sb.append(" , ");

		sb.append(SOAConstants.STATUS_COL);
		
		sb.append(" FROM ");
		
		sb.append(SOAConstants.SV_TABLE);
		
		sb.append(" WHERE ");
		
		sb.append(SOAConstants.SPID_COL);
		
		sb.append(" = ?");
		
		sb.append(" AND ");
		
		if(flag) {
			
			sb.append(SOAConstants.PORTINGTN_COL);
			
			sb.append(" = ?");
		
			sb.append(" ORDER BY CREATEDDATE DESC ");
			
			sb.insert(7, "/*+ INDEX(" + SOAConstants.SV_TABLE +" SOA_SV_INDEX_2) */ ");
		}else {
			sb.append(SOAConstants.SVID_COL);
			
			sb.append(" = ?");
			
			sb.append(" AND ");
			
			sb.append(SOAConstants.REGION_COL);
			
			sb.append(" = ?");
			
			sb.insert(7, "/*+ INDEX(" + SOAConstants.SV_TABLE +" SOA_SV_INDEX_1) */ ");		
		}
		
		
		return sb.toString();
	}
	
	
	/**
	 * This method populate the value for NewSP, OldSP, Status and 
	 * PortingToOriginal by executing the select query
	 * 
	 * @param PreparedStatement pstmt
	 * @param String tnSvid
	 * @throws Exception SQLException
	 */
	private void populateSelectSql( PreparedStatement pstmt, String tnSvid )
									throws SQLException {

	ResultSet rs = null;
	
	try{
		 if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log( Debug.DB_DATA, "Populate SQL statement ..." );
		 }
		 	
		 pstmt.setString( 1, spidValue );
		 
		 if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			 Debug.log( Debug.DB_DATA,"spidValue : "+spidValue );
		 }
   			
   		 pstmt.setString( 2, tnSvid );	
   		 
		 if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			 Debug.log( Debug.DB_DATA,"tnSvid : "+tnSvid );
		 }
   		 
   		 if( !flag ){
   		 	
			pstmt.setString( 3, regionIdValue );
			if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			 Debug.log( Debug.DB_DATA,"regionIdValue : "+regionIdValue );
			}
   		 			
   		 }

		 rs = pstmt.executeQuery();

		 if( rs.next() ){
		 	
		 	newSP = rs.getString(SOAConstants.NNSP_COL);
		 	
			if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA,"New SP : "+newSP );
			}
		 	
		 	oldSP = rs.getString(SOAConstants.ONSP_COL);
		 	
			if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA,"Old SP : "+oldSP );
			}
		 	
		 	status = rs.getString(SOAConstants.STATUS_COL);
		 	
			if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA,"Status : "+status );
			}
		 	
		 	portingToOriginal = rs.getBoolean(SOAConstants.
		 												PORTINGTOORIGINAL_COL);
		 	
			if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA,"PortingToOriginal : "
															+portingToOriginal);
			}
		 }

	}catch( SQLException se ){

		if ( Debug.isLevelEnabled( Debug.DB_ERROR ) ){
			Debug.log( Debug.DB_ERROR, "Could not execute SELECT Query "+se );
		}
		
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

				if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ) ){
					Debug.log( Debug.ALL_ERRORS, 
						DBInterface.getSQLErrorMessage(sqle) );
				}

			}			

		}
										
	}
	
	/**
	 * This method, check the spid value and status and validate the request
	 * 
	 * @param errorCollection
	 * @return
	 * @throws MessageException
	 */
	private String validateModify( String inputDoc ) 
							throws ProcessingException,
									MessageParserException, MessageException{
				
		String errorXML = null;

			XMLMessageParser parser = new XMLMessageParser( inputDoc );
			
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Message "+parser.getMessage() );
			}
		
			if( spidValue.equals(newSP) && 
				(((status.equals(SOAConstants.PENDING_STATUS) || 
					status.equals(SOAConstants.CONFLICT_STATUS)) &&
												portingToOriginal) || 
						status.equals(SOAConstants.DISCONNECT_PENDING_STATUS))){
					
					validateNewSP( parser );
					
					validateGTTData( parser );					
						
					if ( !status.equals(SOAConstants.DISCONNECT_PENDING_STATUS) )
					{
						validateDisconnectPending( parser );
					}
					
					if(errorCol.size() > 0){
					
						errorXML = generateErrors();
					
					}
				
			}else if(spidValue.equals(newSP) && 
					(status.equals(SOAConstants.PENDING_STATUS) || 
						status.equals(SOAConstants.CONFLICT_STATUS) ||
								status.equals(SOAConstants.ACTIVE_STATUS))){	
					
					validateNewSP( parser );
					
					validateDisconnectPending( parser );
					
					if(errorCol.size() > 0){
					
						errorXML = generateErrors();
					
					}
								
							
			}else if(spidValue.equals(oldSP) && 
					(status.equals(SOAConstants.PENDING_STATUS) || 
								status.equals(SOAConstants.CONFLICT_STATUS))){
					
					validateGTTData( parser );	
					
					validateDisconnectPending( parser );
					
					if(errorCol.size() > 0){
					
						errorXML = generateErrors();
					
					}
				
			}
	
	
		return errorXML;
	}
	
	/**
	 * This method, validate the request if spid is NewSP, status is pending, or
	 * active or conflict
	 * 
	 * @param XMLMessageParser parser
	 * 
	 */	
	private void validateNewSP( XMLMessageParser parser ) 
									throws ProcessingException, MessageParserException{
		
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,"Inside validateNewSP ");
		}
		
		TreeMap errorMap = new TreeMap();
			
		if( parser.exists(SOAConstants.ROOT_NODE + SOAConstants.OLDSPDUEDATE_NODE)){
			
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
										"OldSPDueDate Node should Not exist ");
			}
			
			errorMap.put(SOAConstants.OLDSPDUEDATE_NODE,
					parser.getValue(SOAConstants.ROOT_NODE + SOAConstants.OLDSPDUEDATE_NODE));	

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE + SOAConstants.OLDSPAUTHORIZATION_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"OldSPAuthorization Node should Not exist ");
			}
			
			errorMap.put(SOAConstants.OLDSPAUTHORIZATION_NODE, 
				parser.getValue(SOAConstants.ROOT_NODE+SOAConstants.OLDSPAUTHORIZATION_NODE));		

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE + SOAConstants.CAUSECODE_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
											"CauseCode Node should Not exist ");
			}
	
			errorMap.put(SOAConstants.CAUSECODE_NODE, 
						parser.getValue(SOAConstants.ROOT_NODE + SOAConstants.CAUSECODE_NODE));

		}
		
		if (status.equals(SOAConstants.ACTIVE_STATUS) || 
						status.equals(SOAConstants.DISCONNECT_PENDING_STATUS))
		{
			if( parser.exists( SOAConstants.ROOT_NODE + SOAConstants.NEWSPDUEDATE_NODE) ){

				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
									"NewSPDueDate Node should Not exist ");
				}

				errorMap.put(SOAConstants.NEWSPDUEDATE_NODE, 
						parser.getValue(SOAConstants.ROOT_NODE +
									SOAConstants.NEWSPDUEDATE_NODE));

			}
		}
					
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,"Error MAP :  " +
											errorMap.size());
		}
		
		if(errorMap.size() > 0){
			
			errorCol.add(errorMap);
			
		}	
	}
	
	/**
	 * This method, validate the request if spid is NewSP/OldSP, status is 
	 * pending, or disconnect-pending or conflict, and PortToOriginal is true.
	 * 
	 * @param XMLMessageParser parser
	 * 
	 */	
	private void validateGTTData( XMLMessageParser parser )
							throws ProcessingException, MessageParserException{
		
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,"Inside validateNewSPPTO ");
		}
	
		TreeMap errorMap = new TreeMap();

		if( parser.exists(SOAConstants.ROOT_NODE + SOAConstants.LRN_NODE)){
		
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
										"Lrn Node should Not exist ");
			}
		
			errorMap.put(SOAConstants.LRN_NODE,
					parser.getValue(SOAConstants.ROOT_NODE + 
												SOAConstants.LRN_NODE));	
		
		}
	
		if( parser.exists( SOAConstants.ROOT_NODE + 
											SOAConstants.CLASSDPC_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"ClassDPC Node should Not exist ");
			}
		
			errorMap.put(SOAConstants.CLASSDPC_NODE, 
				parser.getValue(SOAConstants.ROOT_NODE+
											SOAConstants.CLASSDPC_NODE));		

		}
	
		if( parser.exists( SOAConstants.ROOT_NODE + 
											SOAConstants.CLASSSSN_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
										"ClassSSN Node should Not exist");
			}

			errorMap.put(SOAConstants.CLASSSSN_NODE, 
						parser.getValue(SOAConstants.ROOT_NODE + 
											SOAConstants.CLASSSSN_NODE));

		}
	
		if( parser.exists( SOAConstants.ROOT_NODE + 
											SOAConstants.LIDBDPC_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"LidbDPC Node should Not exist ");
			}
										
			errorMap.put(SOAConstants.LIDBDPC_NODE,
							parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.LIDBDPC_NODE));

		}
	
		if( parser.exists( SOAConstants.ROOT_NODE + 
											SOAConstants.LIDBSSN_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"LidbSSN Node should Not exist ");
			}

			errorMap.put(SOAConstants.LIDBSSN_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.LIDBSSN_NODE));		

		}
	
		if( parser.exists( SOAConstants.ROOT_NODE + 
											SOAConstants.ISVMDPC_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"IsvmDPC Node should Not exist ");
			}

			errorMap.put(SOAConstants.ISVMDPC_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.ISVMDPC_NODE));

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE +	
											SOAConstants.ISVMSSN_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"IsvmSSN Node should Not exist ");
			}

			errorMap.put(SOAConstants.ISVMSSN_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.ISVMSSN_NODE));

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE +
											 SOAConstants.CNAMDPC_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"CnamDPC Node should Not exist ");
			}

			errorMap.put(SOAConstants.CNAMDPC_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.CNAMDPC_NODE));		

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE +
											 SOAConstants.CNAMSSN_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"CnamSSN Node should Not exist ");
			}

			errorMap.put(SOAConstants.CNAMSSN_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.CNAMSSN_NODE));		

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE + 
											SOAConstants.WSMSCDPC_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"WsmscDPC Node should Not exist ");
			}

			errorMap.put(SOAConstants.WSMSCDPC_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.WSMSCDPC_NODE));		

		}

		if( parser.exists( SOAConstants.ROOT_NODE +
											SOAConstants.WSMSCSSN_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"WsmscSSN Node should Not exist ");
			}

			errorMap.put(SOAConstants.WSMSCSSN_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.WSMSCSSN_NODE));		

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE +
								 SOAConstants.ENDUSERLOCATIONVALUE_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
							"EndUserLocationValue Node should Not exist ");
			}

			errorMap.put(SOAConstants.ENDUSERLOCATIONVALUE_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.ENDUSERLOCATIONVALUE_NODE));		

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE + 
								SOAConstants.ENDUSERLOCATIONTYPE_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"EndUserLocationType Node should Not exist");
			}

			errorMap.put(SOAConstants.ENDUSERLOCATIONTYPE_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.ENDUSERLOCATIONTYPE_NODE));		

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE + 
											SOAConstants.BILLINGID_NODE)){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"BillingId Node should Not exist ");
			}

			errorMap.put(SOAConstants.BILLINGID_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.BILLINGID_NODE));		

		}
		
		if (spidValue.equals(oldSP))
		{
			if( parser.exists( SOAConstants.ROOT_NODE + 
									SOAConstants.NEWSPDUEDATE_NODE)){

				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
									"NewSPDueDate Node should Not exist ");
				}

				errorMap.put(SOAConstants.NEWSPDUEDATE_NODE, 
						parser.getValue(SOAConstants.ROOT_NODE +
									SOAConstants.NEWSPDUEDATE_NODE));

			}
		}
			
		if(errorMap.size() > 0){
		
			errorCol.add(errorMap);
		
		}	

	}

	/**
	 * This method, validate the request if spid is NewSP, status is pending, or
	 * active or conflict
	 * 
	 * @param XMLMessageParser parser
	 * 
	 */	
	private void validateDisconnectPending( XMLMessageParser parser ) 
									throws ProcessingException, MessageParserException{
		
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,"Inside validateDisconnectPending ");
		}
		
		TreeMap errorMap = new TreeMap();			
		
		if( parser.exists( SOAConstants.ROOT_NODE + SOAConstants.CUSTOMERDISCONNECTDATE_NODE) ){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"CustomerDisconnectDate Node should Not exist ");
			}
											
			errorMap.put(SOAConstants.CUSTOMERDISCONNECTDATE_NODE,
							parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.CUSTOMERDISCONNECTDATE_NODE));

		}
		
		if( parser.exists( SOAConstants.ROOT_NODE + SOAConstants.EFFECTIVERELEASEDATE_NODE) ){

			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"Error Node :  " +
								"EffectiveReleaseDate Node should Not exist ");
			}

			errorMap.put(SOAConstants.EFFECTIVERELEASEDATE_NODE, 
					parser.getValue(SOAConstants.ROOT_NODE +
								SOAConstants.EFFECTIVERELEASEDATE_NODE));		

		}
				
		if(errorMap.size() > 0){
			
			errorCol.add(errorMap);
			
		}	
		
	}


    /**
     * This method used to query DB and if there is not result set found,
     * it will generate Error in XML format.
     *
     * @return  String containing results.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private String generateErrors () throws ProcessingException  {
      
       String errorNode = null;
       
	   RuleError ruleError;
	   
	   TreeMap errorMap = null;
	   
	   String errorId = null;
	   
	   String errorValue = null;
	   
	   String errorMessage = null;
	   
	   String errorContext = null;
       
       try
        {
			
            ArrayList errorList = new ArrayList(errorCol);
            
            Iterator itrList = errorList.iterator();
            
			ErrorCollection errorCollection = new ErrorCollection();
			
            while(itrList.hasNext()){
            	
	            errorMap = (TreeMap)itrList.next();
	            
	            Iterator itrMap = errorMap.keySet().iterator();
			
				while(itrMap.hasNext())   
				{
					errorId = (String)itrMap.next();
					
					errorValue = (String)errorMap.get(errorId);
					
					if( portingToOriginal 
							&& (status.equals(SOAConstants.PENDING_STATUS) || 
								status.equals(SOAConstants.CONFLICT_STATUS))
							&& spidValue.equals(newSP) ){
						
						errorMessage = "[" + errorId + "]" 
							 + " can not be modified by SPID [" + spidValue
							 + "] when the status is ["
							 + status + "] and PortToOriginal is [TRUE]";
						
					}else {
						
						errorMessage =  "[" + errorId + "]"
							 + " can not be modified by SPID [" + spidValue
							 + "] when the status is ["
							 + status + "]";	
						
					}
					
	            	errorContext = SOAConstants.CONTEXT_ROOT_NODE + errorId;
	            	
					ruleError = new RuleError( errorId , errorMessage ,
												 errorContext, errorValue);
	
					errorCollection.addError(ruleError); 
	
	            }

            }
            
            errorCol.clear();

            return( errorCollection.toXML() );

        }
        
        catch ( Exception e )
        {

            throw new ProcessingException( "ERROR: Could not generate error" +
            											" :\n" + e.toString() );

        }
       
    }


	/**
	 * 
	 * @param locations
	 * @return
	 * @throws MessageException
	 * @throws ProcessingException
	 */
    private String getValue ( String locations )
                                throws MessageException, ProcessingException
    {
        StringTokenizer st = new StringTokenizer( locations,
								DBMessageProcessorBase.SEPARATOR );
		
		String tok = null;
		
        while ( st.hasMoreTokens() )
        {
            tok = st.nextToken( );

            if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
				Debug.log( Debug.MSG_STATUS, "Checking location ["
                                  + tok + "] for value..." );
			}

            if ( exists( tok, mpContext, inputObject ) )
            {
            
                return( ( String ) get( tok, mpContext, inputObject ) );
                
            }
        }

        return null;
    }



//	--------------------------For Testing---------------------------------//

	public static void main(String[] args) {

		Properties props = new Properties();

		props.put( "DEBUG_LOG_LEVELS", "all" );

		props.put( "LOG_FILE", "E:\\log.txt" );

		Debug.showLevels( );

		Debug.configureFromProperties( props );

		if (args.length != 3)
		{
			Debug.log (Debug.ALL_ERRORS, "ValidateModifySv: USAGE:  "+
			" jdbc:oracle:thin:@192.168.1.246:1521:soa jigar jigar ");

			return;

		}
		try
		{

			DBInterface.initialize(args[0], args[1], args[2]);


		}catch (DatabaseException e)
		{
			Debug.log(null, Debug.MAPPING_ERROR, ": " +
						"Database initialization failure: " + e.getMessage());
		}


		 ValidateModifySv validateModify = new ValidateModifySv();

		try
		{
			validateModify.initialize("FULL_NEUSTAR_SOA","ValidateModifySv");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("spidValue","1111");

			mob.set("npaValue","130");

			mob.set("nxxValue","200");

			mob.set("svIdValue","1");
			
			mob.set("regionIdValue","1");
			
			mob.set("inputMsg", "<SOAMessage>" +
					"<UpstreamToSOA>"+
						"<UpstreamToSOABody>"+
							"<SvModifyRequest>"+
							 "<DataToModify>"+
								"<NewSPDueDate value=\"123456789\"/>"+
								"<OldSPAuthorization value=\"1\"/>"+
								"<ClassDPC value=\"None\"/>"+
								"<Lrn value=\"None\"/>"+
							  "</DataToModify>"+
							"</SvModifyRequest>"+
						"</UpstreamToSOABody>"+
					"</UpstreamToSOA>"+
				"</SOAMessage>");

			validateModify.process(mpx,mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		}catch(ProcessingException pex)
		{

			  System.out.println(pex.getMessage());
		}catch(MessageException mex)
		{
			System.out.println(mex.getMessage());
		}

	} //end of main method

}

