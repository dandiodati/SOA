/**
 * This processor validates the request to check whether the request is
 * valid or not by look-up in SOA_VALID_REQUEST table. Also, query the DB
 * for the relevant data to validate the request data contents validity
 * and generate Error XML, if validation fails.
 *
 * @author Jaganmohan
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see		com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
 * @see		com.nightfire.common.ProcessingException;
 * @see		com.nightfire.framework.message.MessageException;
 * @see     com.nightfire.framework.resource.ResourceException;
 * @see		com.nightfire.framework.util.Debug;
 * @see		com.nightfire.framework.util.NVPair;
 * @see		com.nightfire.framework.util.StringUtils;
 * @see		com.nightfire.spi.common.driver.MessageObject;
 * @see		com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see		com.nightfire.framework.resource.ResourceException;
 * @see		com.nightfire.framework.db.DBConnectionPool;
 * @see		com.nightfire.framework.db.DatabaseException;
 * @see		com.nightfire.framework.db.DBInterface;
 * @see		com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
 * @see		com.nightfire.spi.neustar_soa.utils.SOAConstants;
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date			Reason
	-----       -----------     ----------		--------------------------
	1			Jaganmohan		05/21/2004		Created
	2  			Jaganmohan		05/21/2004		Review comments incorporated
	3  			Jaganmohan		05/28/2004		Query changed to handle
												intraport requests.
	4  			Jaganmohan		05/31/2004		Review comments incorporated
	5			Jaganmohan		06/17/2004		Modified code to handle if
												request type and last
												message are equal.
	6			Jaganmohan		06/18/2004		Review comments incorporated.

	7			Abhijit			07/29/2004		Formal review comments incorporated.

 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Properties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.rules.ErrorCollection;
import com.nightfire.framework.rules.RuleError;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class ValidateRequest extends DBMessageProcessorBase
{

	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
	private boolean usingContextConnection = true;

	/**
	 * Name of the oracle table requested
     */
	private String objectTableName = null;

	private String lookupTableName = null;

	/**
	 * The value of npa
	 */
	private String npaValue = null;

	/**
	 * The value of nxx
	 */
	private String nxxValue = null;

	/**
	 * The value of start TN
	 */
	private String startTNValue = null;

	/**
	 * The value of end TN
	 */
	private String endTNValue = null;

	/**
	 * The value of SPID
	 */
	private String spidValue = null;

	/**
	 * The value of REQUEST_TYPE
	 */
	private String requestType = null;

	/**
	 * The value of ERRORID
	 */
	private String errorId = null;

	/**
	 * The value of ERRORVALUE LOC
	 */
	private String errorValueLoc = null;

	/**
	 * The value of ERRORVALUE
	 */
	private String errorValue = null;

	/**
	 * The value of ERRORMESSAGE
	 */
	private String errorMessage = null;

	/**
	 * The value of ERRORCONTEXT
	 */
	private String errorContext = null;

	/**
	 * The value of ERRORLOCATION
	 */
	private String errorLocation = null;

	/**
	 * The value of isValidate to check the status of ResultSet
	 */
	private boolean isValidate = false;

	/**
	 * The value of isValidate to check the status of ResultSet
	 */
	private String dueDateCheck = "false";

	/**
	 * The value of dueDateCheck
	 */
	private String dueDateCheckLoc = null;	

	/**
     * Constructor.
     */
    public ValidateRequest( )
    {

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log( Debug.OBJECT_LIFECYCLE,
							"Creating ValidateRequest message-processor." );
		}
    }

	/**
     * Initializes this object via its persistent properties.
     *
     * @param key Property-key to use for locating initialization properties.
     * @param type Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type )
							throws ProcessingException
	{

        // Call base class method to load the properties.
        super.initialize( key, type );

        // Get configuration properties specific to this processor.
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "ValidateRequest: Initializing..." );
		}

        StringBuffer errorBuffer = new StringBuffer( );

        objectTableName = getRequiredPropertyValue(
        					SOAConstants.OBJECT_TABLE_NAME_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "OBJECT TABLE NAME is ["
										+ objectTableName + "]." );
		}

		lookupTableName = getRequiredPropertyValue(
				SOAConstants.LOOKUP_TABLE_NAME_PROP, errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "LOOKUP TABLE NAME is ["
										+ lookupTableName + "]." );
		}

		npaValue = getRequiredPropertyValue(
									SOAConstants.NPA_PROP, errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of NPA is ["
										+ npaValue + "]." );
		}

		nxxValue = getRequiredPropertyValue(
									SOAConstants.NXX_PROP, errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of NXX is ["
										+ nxxValue + "]." );
		}

		startTNValue = getRequiredPropertyValue(
								SOAConstants.START_TN_PROP, errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of start TN is ["
										+ startTNValue + "]." );
		}

		endTNValue = getRequiredPropertyValue(
								SOAConstants.END_TN_PROP, errorBuffer  );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of end TN is ["
												+ endTNValue + "]." );
		}

		spidValue = getRequiredPropertyValue(
									SOAConstants.SPID_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of SPID is ["
										+ spidValue + "]." );
		}

		requestType = getRequiredPropertyValue(
					SOAConstants.REQUEST_TYPE_PROP, errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of REQUEST TYPE is ["
										+ requestType + "]." );
		}

		errorLocation = getRequiredPropertyValue(
				SOAConstants.ERROR_RESULT_LOCATION_PROP,errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
			"Error Location to store result into [" + errorLocation + "]." );
		}

		errorId = getRequiredPropertyValue(
						SOAConstants.INPUT_LOC_ERROR_ID_PROP , errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Error ID is [" + errorId + "]." );
		}

		errorValueLoc = getRequiredPropertyValue(
					SOAConstants.INPUT_LOC_ERROR_VALUE_PROP ,errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,"Error Value is [" + errorValue + "]." );
		}

		errorMessage = getRequiredPropertyValue(
				SOAConstants.INPUT_LOC_ERROR_MESSAGE_PROP, errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Error Message is [" +
														errorMessage + "]." );
		}

		errorContext = getRequiredPropertyValue(
					SOAConstants.INPUT_LOC_ERROR_CONTEXT_PROP,errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Error Context is [" +
														errorContext + "]." );
		}

		dueDateCheckLoc = getRequiredPropertyValue(
					SOAConstants.DUE_DATE_CHECK_LOC_PROP,errorBuffer );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Due Date Check location is [" +
														dueDateCheckLoc + "]." );		
		}

        // If any of required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {

            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
					"ValidateRequest: Initialization done." );
		}
     }  //end of initialize method


	/**
	 * Extract data values from the context/input, and use them to
	 * retrive a row into the configured database table.
	 *
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The given input XML or Error XML.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	public NVPair[] process(MessageProcessorContext mpContext,
                          MessageObject inputObject)
                          throws MessageException, ProcessingException
    {
		ThreadMonitor.ThreadInfo tmti = null;
		if ( inputObject == null )
		{
            return null;
		}

        Connection dbConn = null;

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log( Debug.MSG_STATUS, "ValidateRequest: processing ... " );
		}
        try
        {
        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
        
        errorValue = (String) super.get(errorValueLoc, mpContext, inputObject);

		npaValue = (String) super.get( npaValue, mpContext, inputObject );

		nxxValue = (String) super.get( nxxValue, mpContext, inputObject );

		startTNValue = (String) super.get( startTNValue, mpContext,
												inputObject );

		endTNValue = (String) super.get( endTNValue, mpContext, inputObject );

		spidValue = (String) super.get( spidValue, mpContext, inputObject );

		requestType = (String) super.get( requestType, mpContext, inputObject );

		int startTN = 0;

		int endTN = 0;

		try
		{

			startTN = Integer.parseInt(startTNValue);

			endTN = Integer.parseInt(endTNValue);
		}
		catch(NumberFormatException nbrfex)
		{

			throw new MessageException("Invalid start TN & end TN: " + nbrfex);

		}

		StringBuffer queryMessage = new StringBuffer();

		queryMessage.append( "SELECT ");

		queryMessage.append( SOAConstants.STATUS_COL );

		queryMessage.append( " , ");
		
		queryMessage.append( SOAConstants.NNSP_COL);

		queryMessage.append( " , ");
		
		queryMessage.append( SOAConstants.NNSP_DUE_DATE_COL );

		queryMessage.append( " , ");
		
		queryMessage.append( SOAConstants.ONSP_DUE_DATE_COL );

		queryMessage.append( " , ");		

		queryMessage.append( SOAConstants.LASTMESSAGE_COL );

		queryMessage.append( " FROM " );

		queryMessage.append( objectTableName );

		queryMessage.append( " WHERE PORTINGTN = ?" );

		queryMessage.append( " AND SPID = ?" );

		queryMessage.append(" AND STATUS NOT IN ('");

		if ((requestType.equals(SOAConstants.SV_CREATE_REQUEST)) ||
					(requestType.equals(SOAConstants.SV_RELEASE_REQUEST))||
					(requestType.equals(
								SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)))
		{

			queryMessage.append(SOAConstants.ACTIVE_STATUS);

			queryMessage.append("','");

		}

		queryMessage.append(SOAConstants.CANCELED_STATUS);

		queryMessage.append("','");

		queryMessage.append(SOAConstants.OLD_STATUS);

		queryMessage.append("')");


		queryMessage.append( " ORDER BY CREATEDDATE DESC" );

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log( Debug.MSG_STATUS, "Database query is " + queryMessage );
		}

		PreparedStatement tnStatement = null;

		ResultSet rs = null;

		try
		{
            // Get a database connection from the appropriate location - based
			// on transaction characteristics.
            if ( usingContextConnection )
            {

                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database logging is "
					+ "transactional, so getting connection from context." );
				}

                dbConn = mpContext.getDBConnection( );
            }
            else
            {

                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database logging is not "
							+ "transactional, so getting connection "
							+ "from NightFire pool." );
				}

                dbConn = DBConnectionPool.getInstance( true )
							.acquireConnection( );
            }

			if (dbConn == null)
			{
				// throw the exception to the driver.
				throw new ProcessingException( "DB connection is not available" );
			}
		}
		catch ( FrameworkException e )
		{

			String errMsg = "ERROR: ValidateRequest: Attempt to get database "
									+ "connection failed with error: "
									+ e.getMessage( );

			Debug.log( Debug.ALL_ERRORS, errMsg );

			// Re-throw the exception to the driver.
			if(e instanceof MessageException )
			{

				throw ( MessageException )e;

			}else
			{

				throw new ProcessingException( errMsg );

			}
		}

		try
		{

			tnStatement = dbConn.prepareStatement( queryMessage.toString( ) );
			
			String nnsp = null;
	
			String nnspDueDate = null;
			
			String onspDueDate = null;
			
			// for each TN
			for (int i = startTN; i <= endTN; i++ )
			{

				//isValidate to come out if the second queryValidate fails.
				if( isValidate )
				{
					break;
				}

				StringBuffer tn = new StringBuffer( npaValue );

				tn.append( SOAConstants.DEFAULT_DELIMITER );

				tn.append( nxxValue );

				tn.append( SOAConstants.DEFAULT_DELIMITER );

				tn.append( StringUtils.padNumber( i, SOAConstants.TN_LINE,
															true, '0' ) );

				if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				{

					Debug.log(Debug.MSG_STATUS, "Querying TN: [" + tn + "]");

				}

				// set the TN in context

				tnStatement.setString( 1, tn.toString() );

				tnStatement.setString( 2, spidValue );

				rs = tnStatement.executeQuery();

				if (rs.next())
				{

					String status = rs.getString( SOAConstants.STATUS_COL );

					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS, "STATUS is " + status );
					}

					String lastMessage = rs.getString(
												SOAConstants.LASTMESSAGE_COL );

					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS,
											"LASTMESSAGE is " + lastMessage );
					}
					
					nnsp = rs.getString( SOAConstants.NNSP_COL );

					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS,
											"NNSP is " + nnsp );
					}
					
					nnspDueDate = rs.getString(
											SOAConstants.NNSP_DUE_DATE_COL );

					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS,
											"NNSP DUE DATE is " + nnspDueDate );
					}
					
					onspDueDate = rs.getString(
											SOAConstants.ONSP_DUE_DATE_COL );

					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log( Debug.MSG_STATUS,
											"ONSP DUE DATE is " + onspDueDate );
					}
					
					//	create a dynamic error message describing the state
					// of the subscription version
					errorMessage = "This request type ["+
				   	requestType+
				   	"] is not allowed when the subscription " +
				   	"version status is ["+
				   	status+
				   	"] and the last message received was ["+
				   	lastMessage+"]";

					//check whether the SV in pending status 
					if ( ( requestType.equals(SOAConstants.SV_RELEASE_REQUEST)
							|| requestType.equals(SOAConstants.SV_CREATE_REQUEST) )
							&& status.equals(SOAConstants.PENDING_STATUS) )
					{
						
						dueDateCheck = "false";
						
					}

					//Check for duplicate request
					if ( !(lastMessage.equalsIgnoreCase(requestType)) )
					{
						getValidateRequest( dbConn , status,
							lastMessage, requestType);
					}
					
					if( requestType.equals(SOAConstants.SV_MODIFY_REQUEST)){
						
						if(spidValue.equals(nnsp)){							
							
							if( ( onspDueDate != null ) ){

								dueDateCheck = "false";
							
							}else {
								
								dueDateCheck = "true";

								break;
									
							}
							
						}else {
														

							if( ( nnspDueDate != null ) ){
																
								dueDateCheck = "false";							
							
							}else {
								
								dueDateCheck = "true";

								break;
								
							}
						}
					}
				}
				else
				{

					if( !requestType.equals(SOAConstants.SV_CREATE_REQUEST) &&
						!requestType.equals(SOAConstants.SV_RELEASE_REQUEST)&&
						!requestType.equals(
									SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST))
					{
						//	create a dynamic error message describing the state
						// of the subscription version
						errorMessage = "This request type ["+
						requestType+
						"] is not allowed when the subscription " +
						"version does not exist.";

						isValidate = true;

						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							Debug.log( Debug.MSG_STATUS,
											"isValidate is.. " + isValidate );
						}
					}

					if ( requestType.equals(SOAConstants.SV_RELEASE_REQUEST) 
							|| requestType.equals(SOAConstants.SV_CREATE_REQUEST))
					{
						dueDateCheck = "true";

						break;
					}
				}
			}

	  }catch(SQLException ex)
	  {
			String errMsg = "ERROR: ValidateRequest: Attempt to query database" +
								  " failed with error: "+ ex.getMessage();

			Debug.log( Debug.ALL_ERRORS, errMsg );

			// Re-throw the exception to the driver.
			throw new ProcessingException( errMsg );


	  }
	  catch ( ProcessingException e )
	  {

		  String errMsg = "ERROR: ValidateRequest: Attempt to query database" +
						  " failed with error: "+ e.getMessage();

		  Debug.log( Debug.ALL_ERRORS, errMsg );

		  // Re-throw the exception to the driver.
		  throw new ProcessingException( errMsg );
	  }

	  finally
	  {		  
		  set( dueDateCheckLoc, mpContext, inputObject, dueDateCheck );				  
		  
		  if ( isValidate )
		  {

			  ErrorCollection errorCollection = new ErrorCollection();

			  RuleError ruleError = new RuleError( errorId, errorMessage,
			  						errorContext, errorValue );

			  errorCollection.addError(ruleError);

			  mpContext.set( errorLocation, errorCollection.toXML() );			  

	  	  }

		  if ( rs != null )
		  {

			  try
			  {

			    rs.close( );

				rs = null;

			  }

			  catch ( SQLException sqle )
			  {

				  Debug.log( Debug.ALL_ERRORS,
									DBInterface.getSQLErrorMessage(sqle) );

			  }
		   }


		  if ( tnStatement != null )
		  {

			  try
			  {

				  tnStatement.close( );

				  tnStatement = null;

			  }
			  catch ( SQLException sqle )
			  {

				  Debug.log( Debug.ALL_ERRORS,
									DBInterface.getSQLErrorMessage(sqle) );
			  }
		  }

		  // If the configuration indicates that this SQL operation
		  //isn't part of the overall driver transaction, return the
		  // connection previously acquired back to the resource pool.
		  if ( !usingContextConnection && (dbConn != null) )
		  {

			  try
			  {

				  DBConnectionPool.getInstance().releaseConnection( dbConn );

				  dbConn = null;

			  }

			  catch ( ResourceException e )
			  {

				  Debug.log( Debug.ALL_ERRORS, e.toString() );

			  }
		  }
	  }
        }
        finally
        {
        	ThreadMonitor.stop(tmti);
        }
	  // Pass the input on to the output.
	  return( formatNVPair( inputObject ) );
   }

  /**
   * This method used to query for rows from the database table using the
   * given connection and if no result set ,then generate errors
   *
   * @param  dbConn  The database connection to perform the SQL
   * SELECT operation against.
   * @param	status  The Status of the request.
   * @param	lastMessage  The LastMessage received.
   * @param	reqType  The Request Type.
   *
   * @exception  ProcessingException  Thrown if processing fails.
   */
  private void getValidateRequest ( Connection dbConn ,
  									String status,
  									String lastMessage,
  									String reqType)
  									throws ProcessingException
  {

	  StringBuffer queryValidate = new StringBuffer();

	  queryValidate.append( "SELECT ");

	  queryValidate.append( SOAConstants.STATUS_COL );

	  queryValidate.append( " FROM " );

	  queryValidate.append( lookupTableName );

	  queryValidate.append( " WHERE STATUS = ?" );

	  queryValidate.append( " AND LASTMESSAGE = ?" );

	  queryValidate.append( " AND REQUESTRECEIVED = ?" );

	  if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log( Debug.MSG_STATUS, "Database query to check status, lastmessage" +
	  						"and request received is.. " + queryValidate );
	  }

	  PreparedStatement pstmt = null;

	  ResultSet rs = null;

	  try
	  {

		  if( Debug.isLevelEnabled(Debug.DB_DATA) ){
			  Debug.log( Debug.DB_DATA, "Executing SQL:\n" + queryValidate );
		  }

		  // Get a prepared statement for the SQL statement.
		  pstmt = dbConn.prepareStatement( queryValidate.toString() );

		  pstmt.setString( 1, status );

		  pstmt.setString( 2, lastMessage );

		  pstmt.setString( 3, reqType );

		  if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			  Debug.log( Debug.MSG_STATUS, "status is: " + status );
			  Debug.log( Debug.MSG_STATUS, "lastMessage is: " + lastMessage );
			  Debug.log( Debug.MSG_STATUS, "reqType is: " + reqType );
		  }

		  // Execute the SQL SELECT operation.
		  rs = pstmt.executeQuery( );

		  if (!rs.next())
		  {

		  		isValidate = true;

		  }

	  }
	  catch ( SQLException sqle )
	  {

		  throw new ProcessingException( "ERROR: Could not select row from " +
									  "database table :\n" +
									  DBInterface.getSQLErrorMessage(sqle) );
	  }
	  finally
	  {

		  if ( rs != null )
		  {

			  try
			  {

				rs.close( );

				rs = null;

			  }

			  catch ( SQLException sqle )
			  {

				  Debug.log( Debug.ALL_ERRORS,
									  DBInterface.getSQLErrorMessage(sqle) );

			  }
		   }


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
									  DBInterface.getSQLErrorMessage(sqle) );
			  }
		  }
	  }
    }


	//--------------------------For Testing---------------------------------//

	public static void main(String[] args)
	{

		Properties props = new Properties();

		props.put( "DEBUG_LOG_LEVELS", "ALL" );

		props.put( "LOG_FILE", "E:\\logmap.txt" );

		Debug.showLevels( );

		Debug.configureFromProperties( props );

		if (args.length != 3)
		{

			 Debug.log (Debug.ALL_ERRORS, "ValidateRequest: USAGE:  "+
			 " jdbc:oracle:thin:@192.168.1.7:1521:cprod soa soa ");

			return;
		}
		try
		{
			DBInterface.initialize( args[0], args[1], args[2] );
		}
		catch (DatabaseException e)
		{
			 Debug.log( null, Debug.MAPPING_ERROR, ": " +
					  "Database initialization failure: " + e.getMessage() );
		}

		ValidateRequest vrequest = new ValidateRequest();

		try
		{
			vrequest.initialize("FULL_NEUSTAR_SOA","ValidateRequest");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("SPID","1234");

			mob.set("NPA","555");

			mob.set("NXX","123");

			mob.set("StartTN","4444");

			mob.set("EndTN","4444");

			mob.set("REQUESTTYPE","SvReleaseInConflictRequest");

			mob.set("errorValue","SvReleaseInConflictRequest");

			mob.set("ErrorLocation","<Errors>" +
									"<ruleerrorcontainer >" +
									"<ruleerror>" +
										"<RULE_ID value=\"1235656\" />" +
										"<MESSAGE value=\"No record found\" />"+
										"<CONTEXT value=\"spidcontext\" />" +
										"<CONTEXT_VALUE value=\"SPID\" />" +
									"</ruleerror>" +
									"</ruleerrorcontainer >" +
									"</Errors>");

			vrequest.process(mpx,mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		}
		catch(ProcessingException pex)
		{

			System.out.println(pex.getMessage());

		}
		catch(MessageException mex)
		{

			System.out.println(mex.getMessage());

		}
	} //end of main method
}
