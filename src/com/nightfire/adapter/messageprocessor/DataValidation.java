/**
 * The purpose of this processor is to perform Data content validations
 * and generate Error XML, if validation fails.
 *
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.adapter.messageprocessor.DatabaseLogger
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair
 * @see			com.nightfire.framework.rules.ErrorCollection
 * @see			com.nightfire.framework.rules.RuleError
 * @see			com.nightfire.framework.db.PersistentProperty
 * @see			com.nightfire.framework.util.StringUtils
 * @see			com.nightfire.framework.db.DBConnectionPool
 *
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			05/21/2004			Created
	2			Ashok			05/24/2004			Review comments incorporated
	3			Ashok			05/24/2004			ERRORS_CONTEXT_LOCATION_PROP
													property and it's value
													have been changed to
													ERROR_RESULT_LOCATION_PROP
													and ERROR_RESULT_LOCATION
													respectivily
	4			Ashok			05/31/2004			Now setting error result
													into context object
	5			Jigar			06/11/2004			NPA,NXX, Start_Tn and End_Tn 
													made it as getPropertyValue.
	6			Ashok			07/29/2004			Formal review comments
													incorporated
	7			Ashok			09/20/2004			Logic added for 
													negative Data validation
													
	8			Devaraj			04/01/2005			Changed Package name and 
													added constants.
 */


package com.nightfire.adapter.messageprocessor;


import java.util.ArrayList;
import java.util.Properties;
import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.rules.ErrorCollection;
import com.nightfire.framework.rules.RuleError;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;



public class DataValidation extends DBMessageProcessorBase {


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
	 * To store TNRange value
	 */ 
	private String rangeTNValue = null;

	/**
	 * Name of the oracle table requested
	 */ 
	private String tableName = null;

	/**
	 * Store SQL Statement
	 */ 
	private String sqlStmt = null;

	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */ 
	private boolean usingContextConnection = true;

	/**
	 * To indicate whether RANGE_TN_LOOPING true or false.
	 */ 
	private boolean useRangeLoop = true;

	/**
	 * To seperate column
	 */ 
	private String separator = null;

	/**
	 * To store error collection
	 */ 
	private String errorLocation = null;

	/**
	 * To store error Id
	 */ 
	private String errorId = null;

	/**
	 * To store error value Location
	 */ 
	private String errorValueLoc = null;

	/**
	 * To store error value
	 */	
	private String errorValue = null;

	/**
	 * To store error Message
	 */ 
	private String errorMessage = null;

	/**
	 * To store error context
	 */ 
	private String errorContext = null;

	/**
	 * To store column values
	 */ 
	private List columns = null;

	/**
	 * To store Data validation type
	 */ 
	private String validLoc = null;

	/**
	 * To store Data validation value (true or false) based on success or
	 * failure of Data validation
	 */ 
	 
	private String validValue = "true";

	/**
	 * This variable used to get value for tnDelimiter.
	 */	
	private String tnDelimiter = null;
	
	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;
	
	private boolean positiveValidation = true ;
	
	/**
	 * Property giving the SQL SELECT statement to execute.
	 */
	public static final String SQL_QUERY_STATEMENT_PROP
														= "SQL_QUERY_STATEMENT";
	
	/**
	 * The location in the context to put the data validation errors
	 */
	public static final String ERROR_RESULT_LOCATION_PROP = "ERROR_RESULT_LOCATION";
	
	    
    /**
     * Property indicating whether SQL operation should be part of overall
	 * driver transaction.
     */
    public static final String TRANSACTIONAL_LOGGING_PROP
										= "TRANSACTIONAL_LOGGING";

    /**
     * Property indicating separator token used to separate individual
	 * location alternatives.
     */
    public static final String LOCATION_SEPARATOR_PROP = "SEPARATOR";

    /**
     * Property indicating the location seperator for multiple values.
     */
	public static final String DEFAULT_LOCATION_SEPARATOR = "|";

	/**
     * Property prefix giving column data type.
     */
    public static final String COLUMN_TYPE_PREFIX_PROP = "COLUMN_TYPE";

	/**
     * Property prefix giving date format for date types.
     */
    public static final String DATE_FORMAT_PREFIX_PROP = "DATE_FORMAT";

	/**
     * Property prefix giving location of column value.
     */
    public static final String LOCATION_PREFIX_PROP = "LOCATION";


    /**
     * Property prefix giving default value for column.
     */
    public static final String DEFAULT_PREFIX_PROP = "DEFAULT";

	
	/**
	 * Property indicating whether to go for TN Range Looping or not.
	 */
	public static final String RANGE_TN_LOOPING_PROP = "RANGE_TN_LOOPING";

	/**
 	 * Property indicating the location of NPA
	 */
	public static final String NPA_PROP = "NPA";

	/**
	 * Property indicating the location of NXX
	 */
	public static final String NXX_PROP = "NXX";

	/**
	 * Property indicating the location start TN
	 */
	public static final String START_TN_PROP = "START_TN";

	/**
	 * Property indicating the location of end TN
	 */
	public static final String END_TN_PROP = "END_TN";

	/**
	 * Property prefix giving location of input Error Id.
	 */
	public static final String INPUT_LOC_ERROR_ID_PROP = "ERROR_ID";

	/**
	 * Property prefix giving location of input Error Value.
	 */
	public static final String INPUT_LOC_ERROR_VALUE_PROP = "ERROR_VALUE";

	/**
	 * Property prefix giving location of input Error Message.
	 */
	public static final String INPUT_LOC_ERROR_MESSAGE_PROP = "ERROR_MESSAGE";

	/**
	 * Property prefix giving location of input Error Context.
	 */
	public static final String INPUT_LOC_ERROR_CONTEXT_PROP = "ERROR_CONTEXT";

	/**
	 * The location in the context to put success or failure of Data Validation
	 */
	public static String IS_VALID_PROP = "IS_VALID";

	/**
	 * Property prefix giving location of Delimiter.
	 */
	public static final String TN_DELIMITER_PROP = "TN_DELIMITER";

	/**
	 * Default TN delimeter.
	 */
	public static final String DEFAULT_DELIMITER = "-";

	/**
	 * Property indicating current validation is positive or negative
	 */
	public static final String POSITIVE_VALIDATION_PROP
												= "POSITIVE_VALIDATION";	


	public static final int TN_LINE = 4;

	/**
	 * Property indicating the location of TN
	 */
	public static final String TN_LOCATION = "@context.CurrentTN";

	/**
	 * Types of supported columns that require special processing.
	 */
    public static final String COLUMN_TYPE_DATE        = "DATE";

	/**
	 * Token value for LOCATION indicating that the entire contents of the
     * message-processor's input message-object should be used as the
	 * column value.
	 */
    public static final String PROCESSOR_INPUT = "PROCESSOR_INPUT";
    
    /**
     * Constructor.
     */
    public DataValidation ( )
    {

        Debug.log( Debug.OBJECT_LIFECYCLE,
							"Creating Data Validation message-processor." );

        columns = new ArrayList( );

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
        Debug.log( Debug.SYSTEM_CONFIG, "DataValidation: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );

        sqlStmt = getRequiredPropertyValue( 
        				SQL_QUERY_STATEMENT_PROP, errorBuffer );

        Debug.log( Debug.SYSTEM_CONFIG, "SQL query is [" + sqlStmt + "]." );

        errorLocation = getRequiredPropertyValue( 
        			ERROR_RESULT_LOCATION_PROP, errorBuffer );

        Debug.log( Debug.SYSTEM_CONFIG,
			"Error Location to store result into [" + errorLocation + "]." );

        String strTemp = getPropertyValue( 
        						TRANSACTIONAL_LOGGING_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {

            try {

                usingContextConnection = getBoolean( strTemp );

            } catch ( FrameworkException e ) {

                errorBuffer.append ( "Property value for " +
                TRANSACTIONAL_LOGGING_PROP +
                " is invalid. " + e.getMessage ( ) + "\n" );

            }
        }

        Debug.log( Debug.SYSTEM_CONFIG,"Database-select-query will participate"+
        							   " in overall driver transaction? ["
                   					   + usingContextConnection + "]." );

        separator = getPropertyValue( LOCATION_SEPARATOR_PROP );

        if ( !StringUtils.hasValue( separator ) )
            separator = DEFAULT_LOCATION_SEPARATOR;

        Debug.log( Debug.SYSTEM_CONFIG,
							"Location separator token [" + separator + "]." );
							
		String colType = null;
		
		String dateFormat = null;
		
		String location = null;
		
		String defaultValue = null;
		
		ColumnData cd = null;
		
        // Loop until all column configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            colType = getPropertyValue( PersistentProperty.getPropNameIteration
            					( COLUMN_TYPE_PREFIX_PROP, Ix ) );

            dateFormat
            		= getPropertyValue( PersistentProperty.getPropNameIteration
            					( DATE_FORMAT_PREFIX_PROP, Ix ) );

            location
            		= getPropertyValue( PersistentProperty.getPropNameIteration
            					( LOCATION_PREFIX_PROP, Ix ) );

            defaultValue
            		= getPropertyValue( PersistentProperty.getPropNameIteration
            						( DEFAULT_PREFIX_PROP, Ix ) );

            if ( !StringUtils.hasValue(colType)
            	 && !StringUtils.hasValue(dateFormat)
                 && !StringUtils.hasValue(location)
                 && !StringUtils.hasValue(defaultValue) )
            {
            
                break;
                
            }

            // Create a new column data object and add it to the list.
            cd = new ColumnData( colType, dateFormat,
            								location, defaultValue );

            if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
            {
				Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
            }
                
            columns.add( cd );
            
        }

        Debug.log( Debug.SYSTEM_CONFIG, "Number of columns participating in " +
        				"dynamic query criteria [" + columns.size() + "]." );

		strTemp = getRequiredPropertyValue( 
						RANGE_TN_LOOPING_PROP , errorBuffer );

        if ( StringUtils.hasValue( strTemp ) )
        {

            try {

                useRangeLoop = getBoolean( strTemp );

            } catch ( FrameworkException e ) {

                errorBuffer.append ( "Property value for " 
					+ RANGE_TN_LOOPING_PROP + " is invalid. " 
					+ e.getMessage ( ) + "\n" );

            }
        }

        Debug.log( Debug.SYSTEM_CONFIG,"Use Range for Looping? ["
                   					   + useRangeLoop + "]." );

		npaValue = getPropertyValue( NPA_PROP );

		Debug.log( Debug.SYSTEM_CONFIG, "Value of NPA is ["	+ npaValue + "]." );

		nxxValue = getPropertyValue( NXX_PROP );

		Debug.log( Debug.SYSTEM_CONFIG, "Value of NXX is ["	+ nxxValue + "]." );

		startTNValue = getPropertyValue( START_TN_PROP );

		Debug.log( Debug.SYSTEM_CONFIG, "Value of start TN is ["
													  + startTNValue + "]." );

		endTNValue = getPropertyValue( END_TN_PROP );

		Debug.log( Debug.SYSTEM_CONFIG, "Value of end TN is ["
													   + endTNValue + "]." );

		errorId = getRequiredPropertyValue( 
					INPUT_LOC_ERROR_ID_PROP ,errorBuffer );

		Debug.log( Debug.SYSTEM_CONFIG, "Error ID is [" + errorId + "]." );

		errorValueLoc = getRequiredPropertyValue( 
					INPUT_LOC_ERROR_VALUE_PROP ,errorBuffer );

		Debug.log( Debug.SYSTEM_CONFIG,"Error Value Loc is [" +
													 errorValueLoc + "]." );

		errorMessage = getRequiredPropertyValue( 
				INPUT_LOC_ERROR_MESSAGE_PROP, errorBuffer );

		Debug.log( Debug.SYSTEM_CONFIG, "Error Message is [" +
														errorMessage + "]." );

		errorContext = getRequiredPropertyValue( 
					INPUT_LOC_ERROR_CONTEXT_PROP, errorBuffer );

		Debug.log( Debug.SYSTEM_CONFIG, "Error Context is [" +
														errorContext + "]." );

		validLoc = getRequiredPropertyValue( 
									IS_VALID_PROP,errorBuffer );

		Debug.log( Debug.SYSTEM_CONFIG, "location for valid is [" +
															validLoc + "]." );

		tnDelimiter = getPropertyValue( TN_DELIMITER_PROP );

		if (!StringUtils.hasValue(tnDelimiter))
		{
		
			tnDelimiter = DEFAULT_DELIMITER;
			
		}
		
		String positiveValProp = getRequiredPropertyValue( 
								POSITIVE_VALIDATION_PROP , 
								errorBuffer );

		if ( StringUtils.hasValue( positiveValProp ) )
		{

			try {

				positiveValidation = getBoolean( positiveValProp );

			} catch ( FrameworkException e ) {

				errorBuffer.append ( "Property value for " +
				POSITIVE_VALIDATION_PROP +
				" is invalid. " + e.getMessage ( ) + "\n" );

			}
		}

        // If any of the required properties are absent,indicate error to caller
        if ( errorBuffer.length() > 0 )
        {

            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );

        }

        Debug.log( Debug.SYSTEM_CONFIG,"DataValidation: Initialization done." );

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

		if ( inputObject == null )
		{
		
            return null;
            
		}
		
		Debug.log( Debug.MSG_STATUS, "DataValidation: processing ... " );
		
		this.mpContext = mpContext ;
		
		this.inputObject = inputObject ;        

		String result = null;

		Connection dbConn = null;

		errorValue =  getValue( errorValueLoc );

		Debug.log( Debug.SYSTEM_CONFIG,"Error Value is [" +errorValue + "]." );

		if (npaValue != null)
		{
			npaValue =  getValue( npaValue );
		}

		if (nxxValue != null)
		{
			nxxValue =  getValue( nxxValue );
		}
		
		if (startTNValue != null)
		{
			startTNValue =  getValue( startTNValue );
		}
		
		if (endTNValue != null)
		{
			endTNValue =  getValue( endTNValue );
		}
		

		try
        {
			// Get a database connection from the appropriate location -
			//based on transaction characteristics.
			if ( usingContextConnection )
			{
				Debug.log( Debug.MSG_STATUS,
								"Database logging is transactional, " +
								"so getting connection from context." );

				dbConn = mpContext.getDBConnection( );

			}
			else
			{

				Debug.log( Debug.MSG_STATUS,
								"Database logging is not transactional, " +
								"so getting connection from pool." );

				try
				{
				
					dbConn = DBConnectionPool.getInstance( true )
							.acquireConnection( );
							
				}catch ( ResourceException e )
				{
					
					String errMsg 
						= "ERROR: DataValidation: Attempt to get database"
											+ " connection failed with error: " 
											+ e.getMessage( );
        	
					Debug.log( Debug.ALL_ERRORS, errMsg );
            
				}

			}
			
			if( dbConn == null )
			{
				
				throw new ProcessingException("Connection is not acquired ," +
														"it is null ");
				
			}

			if ( useRangeLoop )
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
					StringBuffer tn = new StringBuffer();
					
					tn.append( npaValue );
					
					tn.append( tnDelimiter );
					
					tn.append( nxxValue );
					
					tn.append( tnDelimiter );
					
					tn.append( StringUtils.padNumber( i, TN_LINE, true, '0' ) );
					
					
					if (Debug.isLevelEnabled(Debug.MSG_STATUS))
					{

						Debug.log(Debug.MSG_STATUS, 
										"Logging TN: [" + tn.toString() + "]");

					}
					
					//	set the TN in context
					super.set( TN_LOCATION, mpContext, 
														inputObject, tn.toString() );			

					// Reset the values in cases where we're querying again.
					resetColumnValues( );

					// Extract the column values from the arguments.
					extractQueryCriteriaValues( mpContext, inputObject );

					//Check whether exist or not
					if(mpContext.exists( errorLocation ))
					{
						String errorMessage = (String)mpContext.get(errorLocation);

						Debug.log( Debug.SYSTEM_CONFIG,
											"Error Message from previous data " +
											"validation component. "+errorMessage);
						// Get the XML String
						result = generateErrors( dbConn ,
								ErrorCollection.toErrorCollection(errorMessage));

					}else
					{

						result = generateErrors( dbConn, null);

					}

					if (validValue.equals("false"))
					{
					
						break;
						
					}

				} // END OF FOR LOOP
				
			}
			else
			{
				// Reset the values in cases where we're querying again.
				resetColumnValues( );

				// Extract the column values from the arguments.
				extractQueryCriteriaValues( mpContext, inputObject );

				//Check whether exist or not
				if( mpContext.exists( errorLocation ) )
				{
					String errorMessage = ( String )mpContext.get(errorLocation);

					Debug.log( Debug.SYSTEM_CONFIG,
										"Error Message from previous data " +
										"validation component. "+errorMessage);

					// Get the XML String
					result = generateErrors( dbConn ,
							ErrorCollection.toErrorCollection(errorMessage));

				}else
				{

					result = generateErrors( dbConn , null );

				}
			}	
		}

		catch ( FrameworkException e )
		{
			String errMsg = "ERROR: DataValidation: Attempt to query database" +
						" failed with error: "+ e.getMessage();

			Debug.log( Debug.ALL_ERRORS, errMsg );

			if(e instanceof MessageException )
			{
				
				throw ( MessageException )e;
				
			}else
			{
				
				throw new ProcessingException( errMsg );
				
			}
			
		}
		finally
		{
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

		Debug.log( Debug.MSG_DATA, "Data Validation success : "
														+ validValue );

		if (validValue.equals("false"))
		{
			if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
			{

				Debug.log( Debug.MSG_DATA, "Result:\n" + result );

			}

			//	Place the result in the configured location.
			mpContext.set( errorLocation, result );

		}

		// Place the valid result(true or false)
		set( validLoc,mpContext,inputObject, validValue );

        // Pass the input on to the output.
        return( formatNVPair( inputObject ) );

    }


    /**
     * This method used to query DB and if there is not result set found,
     * it will generate Error in XML format.
     *
     * @param  dbConn  The database connection to perform the SQL
     * SELECT operation against.
     * @param errorCollection as ErrorCollection
     *
     * @return  String containing results.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private String generateErrors ( Connection dbConn ,
    								ErrorCollection errorCollection )
    								throws ProcessingException
    {
        
        PreparedStatement pstmt = null;

		ResultSet rs = null;

        try
        {
			validValue = "true";

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log( Debug.NORMAL_STATUS, "DataValidation : Executing SQL:\n" + sqlStmt );

            // Get a prepared statement for the SQL statement.
            pstmt = dbConn.prepareStatement( sqlStmt );

            // Populate the SQL statement using values obtained from
            // the column data objects.
            populateSqlStatement( pstmt );

            // Execute the SQL SELECT operation.
            rs = pstmt.executeQuery( );
            
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            	Debug.log( Debug.NORMAL_STATUS, "DataValidation : Finished Executing SQL...");
            
            if( errorCollection == null )
			{

            	errorCollection = new ErrorCollection();

            }

			
            if( rs.next() )
            {
            	// check if the current data validation is poitive or negative means
            	// if record is exist but negative validation , we need to generate
            	// the error xml
            	if( ! positiveValidation )
            	{
            	
	            	validValue = "false";
	
					//Create error
	            	RuleError ruleError
	            				= new RuleError( errorId , errorMessage ,
	            								 errorContext,errorValue );
	
	            	errorCollection.addError( ruleError );
	            	
            	}

            }else
            {
            	
				//	check if the current data validation is poitive or negative means
				 // if record doesn,t exist but positive validation , we need to generate
				 // the error xml
				if( positiveValidation )
				{
					
					validValue = "false";
	
					//Create error
					RuleError ruleError
								= new RuleError( errorId , errorMessage ,
												 errorContext,errorValue );
	
					errorCollection.addError( ruleError );
					
				}            	
            	
            }

            return( errorCollection.toXML() );

        }
        catch ( SQLException sqle )
        {

            throw new ProcessingException( "ERROR: Could not select row from " +
            							"database table :\n" +
            							DBInterface.getSQLErrorMessage(sqle) );

        }
        catch ( ProcessingException e )
        {

            throw new ProcessingException( "ERROR: Could not select row from " +
            							"database table :\n" + e.toString() );

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


    /**
     * Populate the SQL SELECT statement from the column data.
     *
     * @param  pstmt  The prepared statement object to populate.
     *
     * @exception ProcessingException  thrown if population fails.
     */
    private void populateSqlStatement ( PreparedStatement pstmt )
    										throws ProcessingException
    {
        
        Debug.log( Debug.DB_DATA, "Populating SQL SELECT statement ..." ); 
		
		try
		{
			
			Iterator iter = columns.iterator( );

			//ColumnData cd = null;

	        for ( int Ix = 1;  iter.hasNext();  Ix ++ )
	        {
				ColumnData cd = (ColumnData)iter.next( );
	
	            Debug.log( Debug.DB_DATA,
							"Populating prepared-statement slot [" + Ix + "]." );
	
	            // Default is no column type specified.
	            if ( !StringUtils.hasValue(cd.columnType) )
	            {
	                	
	                pstmt.setObject( Ix, cd.value );
	                
					Debug.log( Debug.MSG_DATA,
						"Value for column is [" + cd.value.toString() + "]." );
	
	            }else if ( cd.columnType.equalsIgnoreCase(COLUMN_TYPE_DATE) )
	            {
	                String val = (String)(cd.value);	                
	
	                if ( !StringUtils.hasValue( cd.dateFormat ) )
	                {
	
	                    throw new ProcessingException( "ERROR: Configuration for " +
	                    					"date column [" + cd.describe() +
											"] does not specify date format." );
	
	                }
	
	                SimpleDateFormat sdf = new SimpleDateFormat( cd.dateFormat );
	                
					java.util.Date d = null;
					
					try
					{
					
	                	 d = sdf.parse( val );
	                
					}catch(ParseException parseException )
					{
						
						throw new ProcessingException( 
								"ERROR: could not parse date" +
								"\for [" + cd.describe() + "] ." 
								+ parseException.toString() );
						
					}
	
	                pstmt.setTimestamp( Ix, new java.sql.Timestamp(d.getTime()) );
	                
					Debug.log( Debug.MSG_DATA,
										"Value for date column is [" + val + "]." );
	
	            }else
	            {
	                throw new ProcessingException( "ERROR: Invalid column-type " +
	                			"property value given in configuration " +
	                			"\for [" + cd.describe() + "] ." );
	
	            }
	        }
	        
		}catch( SQLException sqlException )
		{
			throw new ProcessingException( 
							"ERROR:  could not populate sql statement ."
							+ sqlException.toString() );
			
		}

        Debug.log( Debug.DB_DATA, "Done populating SQL SELECT statement." );
    }


    /**
     * Extract data for each column from the input message/context.
     *
     * @param mpcontext The message context.
     * @param intputObject The input object.
     *
     * @exception MessageException thrown if required value can't be found.
     * @exception ProcessingException thrown if any other processing error occurs.
     */
    private void extractQueryCriteriaValues ( MessageProcessorContext context,
    										  MessageObject inputObject )
    										throws ProcessingException,
    											   MessageException
    {
        Debug.log (Debug.MSG_STATUS,
					"Extracting message data to use in query criteria ..." );

        Iterator iter = columns.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {
            ColumnData cd = (ColumnData)iter.next( );

            // If location was given, try to get a value from it.
            if ( StringUtils.hasValue( cd.location ) )
            {
                // Location contains one or more alternative locations that
                //could contain the column's value.
                StringTokenizer st = new StringTokenizer(cd.location,separator);

                // While location alternatives are available ...
                while ( st.hasMoreTokens() )
                {
                    // Extract a location alternative.
                    String loc = st.nextToken().trim( );

                    // If the value of location indicates that the input
                    // message-object's  entire value should be used as
                    // the column's value, extract it.
                    if ( loc.equalsIgnoreCase( PROCESSOR_INPUT ) )
                    {
                        Debug.log( Debug.MSG_DATA,
									"Using message-object's contents as the " +
									"column's value." );

                        cd.value = inputObject.get( );

                        break;
                    }

                    // Attempt to get the value from the context or input object.
                    if ( exists( loc, context, inputObject ) )
                    {
                        cd.value = get( loc, context, inputObject );

                        // If we found a value, we're done with this column.
                        if ( cd.value != null )
                        {
                            Debug.log( Debug.MSG_DATA,
										"Found value for column at location [" +
										 loc + "]." );

                            break;

                        }
                    }
                }
            }

            // If no value was obtained from location in context/input,
            // try to set it from default value (if available).
            if ( cd.value == null )
            {
                cd.value = cd.defaultValue;

                if ( cd.value != null )
                    Debug.log( Debug.MSG_DATA, "Using default value for column." );

                else
                {
                    // Signal error to caller.
                    throw new ProcessingException( "ERROR: Could not locate " +
                    			"required value for column [" + cd.describe()
                                + "] used in query " );

                }
            }
        }

        Debug.log (Debug.MSG_STATUS,
					"Done extracting message data to use in query criteria." );
    }


    /**
     * Reset the column values in the list.
     */
    private void resetColumnValues ( )
    {
        Debug.log (Debug.MSG_STATUS, "Resetting column values ..." );

        Iterator iter = columns.iterator( );

        // While columns are available ...
        while ( iter.hasNext() )
        {

            ColumnData cd = (ColumnData)iter.next( );

            cd.value = null;

        }
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
    private String getValue ( String locations )
                                throws MessageException, ProcessingException
    {
        StringTokenizer st = new StringTokenizer( locations,
								DBMessageProcessorBase.SEPARATOR );
		
		String tok = null;
		
        while ( st.hasMoreTokens() )
        {
            tok = st.nextToken( );

            Debug.log( Debug.MSG_STATUS, "Checking location ["
                                  + tok + "] for value..." );

            if ( exists( tok, mpContext, inputObject ) )
            {
            
                return( ( String ) get( tok, mpContext, inputObject ) );
                
            }
        }

        return null;
    }


    /**
     * Class ColumnData is used to encapsulate a description of a single column
     * and its associated value.
     */
    private static class ColumnData
    {
    	//Property used to store Column Type for a column
        public final String columnType;

		//Property used to store Date format for a column if
		// column type is "DATE"
        public final String dateFormat;

		//Property used to store location for a column from where
		// value to be fetched
        public final String location;

		//Property used to store default value for a column
        public final String defaultValue;

 		//Property used to store value for a column
        public Object value = null;

		/**
		 * Constructor
		 */
        public ColumnData ( String columnType, String dateFormat,
                            String location, String defaultValue )
                            
        {
            this.columnType   = columnType;
            this.dateFormat   = dateFormat;
            this.location     = location;
            this.defaultValue = defaultValue;
        }


		/**
		 * This method used to describe a column
		 */
        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Column description: " );

            if ( StringUtils.hasValue( columnType ) )
            {
                sb.append( "type [" );
                sb.append( columnType );
            }

            if ( StringUtils.hasValue( dateFormat ) )
            {
                sb.append( "], date-format [" );
                sb.append( dateFormat );
            }

            if ( StringUtils.hasValue( location ) )
            {
                sb.append( "], location [" );
                sb.append( location );
            }

            if ( StringUtils.hasValue( defaultValue ) )
            {
                sb.append( "], default [" );
                sb.append( defaultValue );
            }

            if ( value != null )
            {
                sb.append( "], value [" );
                sb.append( value.toString() );
            }

            sb.append( "]." );

            return( sb.toString() );
        }
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
			Debug.log (Debug.ALL_ERRORS, "DataValidation: USAGE:  "+
			" jdbc:oracle:thin:@192.168.1.7:1521:cprod soa soa ");

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


		DataValidation dataValidation = new DataValidation();

		try
		{
			dataValidation.initialize("Full Soa Testing","NeuStar");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("SPID","1234");

			mob.set("NPA","123");

			mob.set("NXX","567");

			mob.set("DUEDATE","05-21-2004-0900PM");

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

			dataValidation.process(mpx,mob);

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

