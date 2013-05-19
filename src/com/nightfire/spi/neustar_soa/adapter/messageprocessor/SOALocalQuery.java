/**
 * The purpose of this program is to get the record(s) from configured table 
 * based on query request and generates the query reply XML.
 * 
 * @author Ashok Kumar
 * @version 1.1
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see		com.nightfire.framework.util.Debug
 * @see		com.nightfire.framework.util.NVPair
 * @see 	com.nightfire.framework.db.DBInterface
 * @see		com.nightfire.framework.util.StringUtils
 * @see		com.nightfire.spi.common.driver.MessageObject
 * @see		com.nightfire.spi.common.driver.MessageProcessorContext
 * @see		com.nightfire.spi.common.driver.MessageProcessorBase
 * @see		com.nightfire.framework.message.generator.xml.XMLMessageGenerator
 * @see		com.nightfire.framework.message.parser.xml.XMLMessageParser
 * @see		com.nightfire.spi.neustar_soa.utils.SOAUtility
 * @see		com.nightfire.spi.neustar_soa.utils.SOAConstants
 * @see		com.nightfire.framework.db.DBConnectionPool
 * @see 	com.nightfire.framework.db.PersistentProperty
 * 
 */

/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			07/27/2004			Created
	2			Ashok 			07/30/2004			Review Comments incorporated
	3			Ashok			09/02/2004			Support for NETWORK's query 
													in Phase II
	4			Ashok			09/10/2004			Review Comments incorporated
	5			Ashok			12/09/2004			Customer ID property added
		
 */


package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.SQLBuilder;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class SOALocalQuery extends MessageProcessorBase
{
    
	
	/**
	 * Static variable for null_clause_fields
	 */
	public static final String NULL_CLAUSE_FIELDS = "NULL_CLAUSE_FIELDS";
	
	public static final String SQL_IS_NULL_CLAUSE = " IS NULL";
	
	public static final String SQL_AND_CLAUSE = " AND ";
	
	/**
	 * Static hashmap which contains the mapping between xml node and actual column name 
	 * of null_clause_fields.
	 */
	private static HashMap<String, String> null_clause_map = new HashMap<String, String>();
	
	/**
	 * Property indicating the location of ALLOW_NULL_CLAUSE_SEARCH
	 */
	public static final String ALLOW_NULL_CLAUSE_SEARCH_PROP = "ALLOW_NULL_CLAUSE_SEARCH";

	/**
	 * Variable contains value of Table name
	 */
   	private String tableName = null;
   	
	/**
	 * Variable contains value of Seperator
	 */
    private String separator = null;	
    
	/**
	 * Variable contains list of ColumnDatas which contains 
	 * all configured column data(Column type , Column Name ...)
	 */
    private List columns = null;
    
	/**
	 * Variable contains list of list(s) which contains String array(s) of
	 * ColumnName , Column Value and Node Name;
	 */
	private List list = null;    
    
	/**
	 * Variable contains value of Date format.
	 */
    private String dateFormat = null;	
    
	/**
	 * Variable contains location of query reply type.
	 */
    private String queryReplyTypeLoc = null;
    
	/**
	 * Variable contains value of query reply type.
	 */
    private String queryReplyType = null ;
    
	/**
	 * Variable contains location of spid value.
	 */
    private String spidLoc = null;
    
	/**
	 * Variable contains value of spid.
	 */
    private String spid = null;
    
	/**
	 * Variable contains value of allow_null_clause_search.
	 */
    private String allow_null_clause_search  = null;

    
	/**
	 * Value of output XML location
	 */
	private String outputXMLLoc = null;
	
	/**
	 * Variable contains error buffer string
	 */    
	private StringBuffer errorBuffer = null;
	
	/**
	 * Variable contains HashMap of Column Name and Node Name.
	 */
	private HashMap map = null;
	
	/**
	 * This flag is used to identify whether spid column should be added 
	 * or not in where clause.
	 */
	boolean spidFlag = false ;
	
	/**
	 * variable contains region id value 
	 */	
	private String regionId = null;
	
	/**
	 * The value of useCustomerId
	 */	
	private boolean useCustomerId = true;
	
	/**
     * Constructor.
     */
    public SOALocalQuery ( )
    {
    	if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log( Debug.OBJECT_LIFECYCLE, 
									"Creating SOALocalQuery message-processor." );
		}

		columns = new ArrayList( );
		
		list = new ArrayList( );
		
		map = new HashMap();				
		
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key  Property-key to use for locating initialization properties.
     * @param  type Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) 
    												throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "SOALocalQuery: Initializing..." );
		}

        errorBuffer = new StringBuffer( );
		
		// Get configuration properties specific to this processor.		
        tableName = getRequiredPropertyValue( 
        					SOAConstants.TABLE_NAME_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, 
							"Database table Name is [" + tableName + "]." );
		}
						
		spidLoc = getRequiredPropertyValue( 
		
									SOAConstants.SPID_PROP , errorBuffer );
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){														
			Debug.log( Debug.SYSTEM_CONFIG, "Location of SPID value : "
																+ spidLoc + "." );
		}
		
		allow_null_clause_search = getRequiredPropertyValue( 
				
				ALLOW_NULL_CLAUSE_SEARCH_PROP , errorBuffer );
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){														
					Debug.log( Debug.SYSTEM_CONFIG, "Value of allow_null_clause_search value : "
											+ allow_null_clause_search + "." );
			}
						
		dateFormat = getRequiredPropertyValue( 		
						SOAConstants.DATE_FORMAT_PREFIX_PROP , errorBuffer );
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){														
			Debug.log( Debug.SYSTEM_CONFIG, "value of output date format : "
															+ dateFormat + "." );
		}
																
		queryReplyTypeLoc = getRequiredPropertyValue( 
				SOAConstants.QUERY_REPLY_TYPE_PREFIX_PROP, errorBuffer );
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){													
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Query Request : "
													+ queryReplyTypeLoc + "." );
		}
												
		outputXMLLoc = getRequiredPropertyValue(
						SOAConstants.OUTPUT_XML_PREFIX_PROP, errorBuffer );
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){														
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Output XML : "
															+ outputXMLLoc + "." );
		}

        separator = getPropertyValue( SOAConstants.LOCATION_SEPARATOR_PROP );

        if ( !StringUtils.hasValue( separator ) )
        {
        
            separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;
            
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){    
			Debug.log( Debug.SYSTEM_CONFIG, 
								"Location separator token [" + separator + "]." );
		}							
		String strTemp = getPropertyValue(SOAConstants.USE_CUSTOMER_ID_PROP);

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
            
		try
		{
		
			populateColumnData(); 
						
			
		}catch( ProcessingException e )
		{
			
			Debug.log( Debug.ALL_ERRORS, e.toString() );
			
			throw new ProcessingException( 
						" Could not populate column data : "+e.toString());
			       
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "SOALocalQuery: Initialization done." );
		}
    }
    
	/**
	 * This method populates the list for columndata
	 *
	 * @exception ProcessingException when initialization fails
	 **/
	private void populateColumnData() throws ProcessingException
	{
		
		String columnName  = null;
		
		String colType = null;	
		
		String startLocation = null;
		
		String endLocation = null;
		
		String nodeName = null;
		
		ColumnData cd = null;
		
		for ( int Ix = 0;  true;  Ix ++ )
		{
			columnName = getPropertyValue( PersistentProperty
			 			.getPropNameIteration( 
			 			SOAConstants.COLUMN_NAME_PREFIX_PROP, Ix ));			
			 				
			// If we can't find next Column Name property we're done
			if ( !StringUtils.hasValue( columnName ) )
			{
			
				break;
				
			}
			
			colType = getPropertyValue( PersistentProperty
						.getPropNameIteration( 
						SOAConstants.COLUMN_TYPE_PREFIX_PROP, Ix ) );			
			
						
			startLocation = getPropertyValue( PersistentProperty
					.getPropNameIteration( 
					SOAConstants.START_LOCATION_PREFIX_PROP, Ix ) );
							
			endLocation = getPropertyValue( PersistentProperty
						.getPropNameIteration( 
						SOAConstants.END_LOCATION_PREFIX_PROP, Ix ) );
							
			nodeName = getPropertyValue( PersistentProperty
						.getPropNameIteration( 
						SOAConstants.NODE_NAME_PREFIX_PROP, Ix ) );
			
			// Create a new column data object and add it to the list.
			cd = new ColumnData( columnName, colType , startLocation , 
												endLocation, nodeName );		
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){							
			  Debug.log( Debug.SYSTEM_CONFIG, cd.describe() );
			}
			
			columns.add( cd );
			
			//	Add column name and Node Name as key value pair HashMap
			map.put( columnName , nodeName );				
			
		}
		
		//	If any of the required properties are absent, 
		// indicate error to caller.
		if ( errorBuffer.length() > 0 )
		{
			String errMsg = errorBuffer.toString( );
			 
			Debug.log( Debug.ALL_ERRORS, errMsg );
			 
			throw new ProcessingException( errMsg );
			 
		}
		
	}

    /**
     * Extract data values from the context/input, and use them to
     * select row(s) from the configured database table and generates
     * query reply XML.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( 	MessageProcessorContext mpContext, 
    						  	MessageObject inputObject )
                        		throws MessageException, ProcessingException
    {
    	ThreadMonitor.ThreadInfo tmti = null;
    	
        if ( inputObject == null )
        {
        
            return null;
            
        }
        if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){			
		   Debug.log( Debug.MSG_STATUS, "SOALocalQuery: processing ... " );
		}
		
		Connection dbConn = null;
		
		XMLMessageGenerator response = null;
		
		try{
			
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			// Extract the column values .
			extractMessageData( mpContext, inputObject );
			
			// Get spid value from context
			spid  = getString( spidLoc , mpContext , inputObject );
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){		
				Debug.log( Debug.SYSTEM_CONFIG, "Value of SPID : "
																+ spid + "." );
			}
			// get Query Request type
			queryReplyType 
					= getString( queryReplyTypeLoc , mpContext , inputObject );
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){			
				Debug.log( Debug.SYSTEM_CONFIG, "Value of Query Request : "
														+ queryReplyType + "." );
			}
													
			if( queryReplyType.equalsIgnoreCase( SOAConstants.SV_QUERY_REPLY ) )
			{
				
				spidFlag = true ;
			}
			
			try
			{
				
				// Get connection from Context.
				dbConn = mpContext.getDBConnection( );
					
			}
			catch ( Exception re )
			{
		
				Debug.log( Debug.ALL_ERRORS, re.getMessage() );
		
			}
			
			if( dbConn == null )
			{
		
				throw new ProcessingException("Connection is not acquired ," +
														"it is null ");
		
			}
	
			try
			{
				
				// select data from the database.
				selectData( dbConn,inputObject  );
				
				// generate query reply xml
				response = generateQueryReply( dbConn ) ;
				if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){	
					Debug.log( Debug.MSG_STATUS, 
									"Query reply XML  ... "+response.getMessage() );
				}
								
				//	Setting Output XML in to context		
				super.set( outputXMLLoc , mpContext , inputObject , 
														response.getDocument() );
				
			}
			catch ( FrameworkException e )
			{
				
				String errMsg = "ERROR: SOALocalQuery: Attempt to select records "+
									"from database failed with error: " + 
									e.getMessage();
	
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
		}finally
		{
			ThreadMonitor.stop(tmti);
		}
        
        return( formatNVPair( inputObject ) );
    }
    
	/**
	 * Query for row(s) from the database table using the given connection.
	 *
	 * @param  dbConn  The database connection to perform the SQL SELECT 
	 * 					operation against.
	 * @param inputObject 
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
   	private void selectData ( Connection dbConn, MessageObject inputObject ) throws ProcessingException, DatabaseException
   	{
	   	
	   	PreparedStatement pstmt = null;
	   	
	   	ResultSet rs = null;
	   
	   	String sqlSelectStmt = null;	
	   	
	   	StringBuffer sb = new StringBuffer();

	   	try
	   	{
	   		
	   		sqlSelectStmt = constructSelectSqlStatement();
	   		
	   		//append null clasue if allow_null_clause_search property is true. 
	   		
	   		if(allow_null_clause_search.equalsIgnoreCase("TRUE")){
	   			
	   			sb.append(sqlSelectStmt);
	   			sqlSelectStmt = appendNullClause(inputObject, sb);
	   			
	   		}
	   		
	   		if( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){	
				Debug.log( Debug.NORMAL_STATUS, 
									"Executing Select SQL:\n" + sqlSelectStmt );
			}

		   	// Get a prepared statement for the SQL statement.
		   	pstmt = dbConn.prepareStatement( sqlSelectStmt );

		   	// Populate the SQL statement using values obtained from 
		   	// the column data objects.
		   	populateSelectSqlStatement( pstmt );
			
		   	// Execute the SQL SELECT operation.
		   	rs = pstmt.executeQuery( );
		   
		   	ResultSetMetaData rsmd = rs.getMetaData( );

		   	int numCols = rsmd.getColumnCount( );
		   	
			SimpleDateFormat sdf = new SimpleDateFormat( dateFormat );
		   	
		   	List columnNodeValue = null;
		   	
			String colValue = null;				
				
			String colName = null;
   
			int colType =  -1 ;
		   	
		   	// Loop over rows returned by query ...
		   	while(  rs.next() )
		   	{				
				
				columnNodeValue = new ArrayList( );
		   		
			   	// Loop over columns that are available for each returned row .
			   	for ( int Jx = 1;  Jx <= numCols;  Jx ++ )
			   	{
				   	
				   	colName = rsmd.getColumnName( Jx );

				   	colType = rsmd.getColumnType( Jx );				   	

				   	if ( (colType == Types.DATE) 
				   			|| (colType == Types.TIME) 
				   			|| (colType == Types.TIMESTAMP) )
				   	{				   		

					   	java.sql.Timestamp d = rs.getTimestamp( colName );
					   	
					   	if(d != null)
					   	{
							colValue = sdf.format( d );
					   		
					   	}else
					   	{
					   		
							colValue = null;
					   		
					   	}					   	
					   	
				   	}
				   	else  // It's not a date, so extract as string.
				   	{
										   
					   	colValue = rs.getString( colName );

					}
                    if( Debug.isLevelEnabled( Debug.DB_DATA ) ){
						Debug.log( Debug.DB_DATA, "Extracting result-set data " +
										"for column [" + colName + "] " +
										" ,value [" + colValue + "] ..." );
					}
					
					
					
					if( colValue != null )
					{	
						// create String array to store column name 
						// column value and node name
						String columnNodeArr[] = new String[3];
						
						columnNodeArr[0] =  colName ;
						
						columnNodeArr[1] = colValue ;			   
						
						// Get node name from HashMap for a colName
						columnNodeArr[2] = ( String ) map.get( colName );						
						
						// if node name value is not equal to null
						if( columnNodeArr[2] != null )
						{
						
							// add this array to the list
							columnNodeValue.add( columnNodeArr );
						
						}
						
					}
				   
			   	}
			   	
			   	if( columnNodeValue.size() > 0 )
			   	{
			   		// add list which represents one record				
					list.add( columnNodeValue );					
					
			   	}						   	
			   	
		   	}		   
		   
	   	}
	   	catch ( SQLException sqle )
	   	{
	   	
		   	throw new ProcessingException( "ERROR: Could not select row from " +
		   						"database table [" + tableName + "]:\n" + 
		   						DBInterface.getSQLErrorMessage(sqle) );
		   						
	   	}
	   	catch ( ProcessingException e )
	   	{
	   	
		   	throw new ProcessingException( "ERROR: Could not select row " +
		   		"from database table [" + tableName  + "]:\n" + e.toString() );
		   		
	   	}
	   	finally
	   	{
		   	if ( pstmt != null )
		   	{
			   	try
			   	{
			   		if(rs != null)
			   		{
			   		
			   			rs.close();
			   			
			   			rs = null ;
			   			
			   		}
			   					   		
				   	pstmt.close( );
				   	
				   	pstmt = null ;
				   
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
	 * This method generates query reply XML.  
	 *
	 * @param  dbCon  The database connection to perform the SQL SELECT 
	 * 					operation against.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
   	private XMLMessageGenerator generateQueryReply( Connection dbCon  ) 
   													throws MessageException ,
   													ProcessingException{
   		
		//	Generate an XML response
		XMLMessageGenerator response 
				= new XMLMessageGenerator( SOAConstants.RESPONSE_ROOT );		
		
		// Create Simple date format for input date format
		SimpleDateFormat df = new SimpleDateFormat( dateFormat );
		
		// create Datesent node in query reply XML	
		response.setValue( SOAConstants.DATESENT, df.format( new Date() ) );
		
		String request_status_path 
				= SOAConstants.SOA_TO_UPSTREAM_BODY_PATH + "." + 
				  queryReplyType + 
				  SOAConstants.QUERY_STATUS +
				  SOAConstants.SOA_REQUEST_STATUS ;
		
		// set status node in the reply xml
		response.setValue( request_status_path , SOAConstants.SUCCESS );
		
		// if list contains at least one record
   		if( list.size() > 0 )
   		{
   											
			// if SV or Number Pool Block query reply
			if( queryReplyType.equalsIgnoreCase( SOAConstants.SV_QUERY_REPLY ) 
			|| queryReplyType.equalsIgnoreCase( SOAConstants.NBR_QUERY_REPLY ) )
			{
				
				generateSvNpbQueryReply( response , dbCon );
									
			// if NPA-NXX query Reply						
			}else if (	queryReplyType.equalsIgnoreCase( 
								SOAConstants.NPA_NXX_QUERY_REPLY ))
			{
				
				generateNpaNxxQueryReply( response );
											
			// if NPA-NXX_X query reply				
			}else if (	queryReplyType.equalsIgnoreCase( 
						SOAConstants.NPA_NXX_X_QUERY_REPLY ))
			{
				
				generateNpaNxxXQueryReply( response );
									
			// if LRN query reply						
			}else if (	queryReplyType.equalsIgnoreCase( 
								SOAConstants.LRN_QUERY_REPLY ) )
			{
				
				generateLRNQueryReply( response );								
							
			}							
							
   		}
		
		return response ;
   		
   	}
   	
	/**
	 * This method generates query reply XML for SV or number pool block query req.  
	 *
	 * @param  dbCon  The database connection to perform the SQL SELECT 
	 * 					operation against.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	private void generateSvNpbQueryReply(	XMLMessageGenerator response,
											Connection dbCon  ) 
											throws MessageException ,
											ProcessingException {

		Iterator iter = list.iterator( );			

		List arrayList = null;

		Iterator iterator = null;

		int i = 0 ;	

		

		// this is used to get Failed SP List from FAILEDSPLIST table
		String id = null ;			

		String failedSPList = null;	

		XMLMessageParser domMsg = null;	

		Node source = null;

		StringBuffer npaNxxX = null;
		
		String dataPath = null;
		
		if( queryReplyType.equalsIgnoreCase( SOAConstants.SV_QUERY_REPLY ) )
		{
			
			dataPath = SOAConstants.SV_DATA_PATH ;
			
		}else
		{
			
			dataPath = SOAConstants.NPB_DATA_PATH ;
			
		}

		// loop over the all list
		while ( iter.hasNext() )
		{

			arrayList =  ( ArrayList ) iter.next();

			iterator = arrayList.iterator();

			npaNxxX = new StringBuffer();
			
			if( queryReplyType.equalsIgnoreCase( 
											SOAConstants.NBR_QUERY_REPLY ) )
			{
				
				response.create( dataPath + "(" + i + ").BlockId" );
													
			}else
			{
				
				response.create( dataPath + "(" + i + ").SvId" );
				
			}

			// loop over all column node array
			while( iterator.hasNext() )
			{
						
				String columnNode[] = ( String [] )iterator.next();				
				
				if( columnNode[0].equals( SOAConstants.NPBID_COL ) ||  
								columnNode[0].equals( SOAConstants.SVID_COL ))
				{
					 // get npbid 
					 id = columnNode[1] ;
                     if( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					   Debug.log( Debug.DB_DATA,"ID value : " + id );
					 }

				}		
			
				if( columnNode[0].equals( SOAConstants.NPA_COL ) 
					|| columnNode[0].equals( SOAConstants.NXX_COL )
					|| columnNode[0].equals( SOAConstants.DASHX_COL ) )
				{
										
					//combine npa,nxx, dashx value	
					npaNxxX.append( columnNode[1] );					
					
			
				}else if( columnNode[0].equals( SOAConstants.SPID_COL ) )
				{
					
					if( queryReplyType.equalsIgnoreCase( 
											SOAConstants.NBR_QUERY_REPLY ) )
					{
					    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){	
							Debug.log( Debug.SYSTEM_CONFIG,
													"Creating Node : NpaNxxX..");
						}
	
						response.setValue( dataPath 
											+ "(" + i + ").NpaNxxX"  ,
											npaNxxX.toString() );
					}
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}

					response.setValue( dataPath 
												+ "(" + i + ")." + columnNode[2] , 
												columnNode[1] );					
						
				
				}else if( columnNode[0].equals( SOAConstants.STATUS_COL  ) )
				{
				 	
					if( queryReplyType.equalsIgnoreCase( 
									SOAConstants.SV_QUERY_REPLY ) )
					{
						if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
							Debug.log( Debug.SYSTEM_CONFIG, "Creating Node : " 
												+ SOAConstants.DOWNLOAD_REASON_NODE );
						}
						
						response.setValue( dataPath 
							+ "(" + i + ")" + SOAConstants.DOWNLOAD_REASON_NODE , 
							SOAConstants.DOWNLOAD_REASON_NEW );
					}
                    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}

					response.setValue( dataPath 
												+ "(" + i + ")." + columnNode[2] , 
												columnNode[1] );
												
				// if column name is 'ONSP'
				}else if( columnNode[0].equals( SOAConstants.ONSP_COL  ) )
				{
				 	if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}

					response.setValue( dataPath 
											+ "(" + i + ")." + columnNode[2] , 
											columnNode[1] );
												
					response.create( dataPath + "(" + i + ").NewSPDueDate" );
					
					response.create( 
								dataPath + "(" + i + ").NewSPCreateTimestamp" );
					
					response.create( dataPath + "(" + i + ").OldSPDueDate" );
					
					response.create( 
								dataPath + "(" + i + ").OldSPAuthorization" );
												
				// if column name is 'FAILEDSPFLAG'
				}else if( columnNode[0].equals( 
											SOAConstants.FAILED_SP_FLAG_COL  ) )
				{
					
					 // if FailedSpFlag is 'true'													
					 if( columnNode[1].equals("1") )
					 {
						
						if( queryReplyType.equalsIgnoreCase( 
												SOAConstants.SV_QUERY_REPLY ) )
						{
						 	// Get Failed SP List for this npbid
						 	failedSPList  = getFailedSPList( dbCon , id ,
												SOAConstants.SV_OBJECT_TYPE );
						}else
						{
							
							//	Get Failed SP List for this npbid
							failedSPList  = getFailedSPList( dbCon , id ,
												SOAConstants.NPB_OBJECT_TYPE );
							
						}
		
						 if( failedSPList != null )
						 {
										
							 domMsg  = new XMLMessageParser( failedSPList );									

							 source = domMsg.getDocument()
														.getDocumentElement();
				             if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
								 Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + failedSPList );
							 }

							 response.setValue( dataPath 
											+ "(" + i + ").", null, source  );	
				
						 }							

					 }		
	 
				// if any GTTDATA presents
				}else if ( 
					columnNode[0].equals( SOAConstants.CLASSDPC_COL )
					||columnNode[0].equals( SOAConstants.CLASSSSN_COL )
					||columnNode[0].equals( SOAConstants.CNAMDPC_COL )
					||columnNode[0].equals( SOAConstants.CNAMSSN_COL )
					||columnNode[0].equals( SOAConstants.ISVMDPC_COL )
					||columnNode[0].equals( SOAConstants.ISVMSSN_COL )
					||columnNode[0].equals( SOAConstants.LIDBDPC_COL )
					||columnNode[0].equals( SOAConstants.LIDBSSN_COL )
					||columnNode[0].equals( SOAConstants.WSMSCDPC_COL )
					||columnNode[0].equals( SOAConstants.WSMSCSSN_COL ) )
				{
		            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}
			
					response.setValue( dataPath 
								+ "(" + i + ")"+ SOAConstants.GTTDATA_NODE 
								+"."+ columnNode[2]  , columnNode[1] );
		
		
				}else if( columnNode[0].equals( SOAConstants.SOAORIGINATION_COL ) )
				{
	 				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){		
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}
			
					response.setValue( dataPath 
							+ "(" + i + ")." + columnNode[2] , columnNode[1] );
							
					response.create( dataPath + "(" + i + ").CreateTimestamp" );
							
							
							
				}else if( columnNode[0].equals( SOAConstants.PORTINGTN_COL ) )
				{
	 				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){		
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}
			
					response.setValue( dataPath 
							+ "(" + i + ")." + columnNode[2] , columnNode[1] );
							
					response.create( dataPath + "(" + i + ").LnpType" );
							
							
							
				}else
				{
	 			   if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){		
					   Debug.log( Debug.SYSTEM_CONFIG,
										   "Creating Node : " + columnNode[2] );
				   }
			
				   response.setValue( dataPath 
						   + "(" + i + ")." + columnNode[2] , columnNode[1] );
							
				}						  
	
			}

			// forward to add another record
			i++ ;	

		}		

	}
	
	/**
	 * This method generates query reply XML for NPA-NXX query request.  
	 *
	 * @param  dbCon  The database connection to perform the SQL SELECT 
	 * 					operation against.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	private void generateNpaNxxQueryReply(  XMLMessageGenerator response ) 
											throws MessageException ,
											ProcessingException {
				
		Iterator iter = list.iterator( );			
	
		List arrayList = null;
	
		Iterator iterator = null;
	
		int i = 0 ;	
	
		
		
		// loop over the all list
		while ( iter.hasNext() )
		{
		
			arrayList =  ( ArrayList ) iter.next();
		
			iterator = arrayList.iterator();
			
			response.create( 
					SOAConstants.NPA_NXX_DATA_PATH + "(" + i + ").NpaNxxId" );
		
			// loop over all column node array
			while( iterator.hasNext() )
			{
								
				String [] columnNode = ( String [] )iterator.next();			
			
				if( columnNode[0].equals( SOAConstants.NPA_COL )
					|| columnNode[0].equals( SOAConstants.NXX_COL ) )
				{
				    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log( Debug.SYSTEM_CONFIG,
										"Creating Node : " + columnNode[2] );
					}
				
					response.setValue( SOAConstants.NPA_NXX_DATA_PATH 
								+ "(" + i + ")"+ SOAConstants.NPANXXVALUE_NODE 
								+"."+ columnNode[2]  , columnNode[1] );
				
				
				}else if( columnNode[0].equals( 
											SOAConstants.EFFECTIVEDATE_COL ) )
				{
				    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}

					response.setValue( SOAConstants.NPA_NXX_DATA_PATH 
											+ "(" + i + ")." + columnNode[2] , 
											columnNode[1] );
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){	
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + 
											SOAConstants.DOWNLOAD_REASON_NODE );
					}

					response.setValue( SOAConstants.NPA_NXX_DATA_PATH 
								+ "(" + i + ")" + 
								SOAConstants.NPA_NXX_DOWNLOAD_REASON_NODE , 
								SOAConstants.DOWNLOAD_REASON_NEW );
					
					response.create( SOAConstants.NPA_NXX_DATA_PATH + 
										"(" + i + ").NpaNxxCreateTimestamp" );
				
					
	
				
				}
				
				else if(columnNode[0].equals( 
						SOAConstants.MODIFIEDDATE_COL )){
					
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}

					response.setValue( SOAConstants.NPA_NXX_DATA_PATH 
											+ "(" + i + ")." + columnNode[2] , 
											columnNode[1] );
					
				}
				else if( columnNode[0].equals(SOAConstants.STATUS_COL))
				{
					response.setValue( SOAConstants.NPA_NXX_DATA_PATH 
							+ "(" + i + ")." + columnNode[2] , 
							columnNode[1] );
				}
				
				
				else 
				{
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){				
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}
					
					response.setValue( SOAConstants.NPA_NXX_DATA_PATH 
							+ "(" + i + ")." + columnNode[2] , columnNode[1] );
				
				}	
											
			}
		
			// forward to add another record
			i++ ;	
		
		}		
	
	}
		
	/**
	 * This method generates query reply XML for NPA-NXX-X query request
	 *
	 * @param  dbCon  The database connection to perform the SQL SELECT 
	 * 					operation against.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	private void generateNpaNxxXQueryReply(	XMLMessageGenerator response ) 
											throws MessageException ,
											ProcessingException{
	
		Iterator iter = list.iterator( );			
	
		List arrayList = null;
	
		Iterator iterator = null;
	
		int i = 0 ;	
	
		StringBuffer npaNxxX = null;
	
		// loop over the all list
		while ( iter.hasNext() )
		{
		
			arrayList =  ( ArrayList ) iter.next();
		
			iterator = arrayList.iterator();
		
			npaNxxX = new StringBuffer();
			
			response.create( SOAConstants.NPA_NXX_X_DATA_PATH 
											+ "(" + i + ").SPID" );							
		
			// loop over all column node array
			while( iterator.hasNext() )
			{
								
				String [] columnNode = ( String [] )iterator.next();
			
				if( columnNode[0].equals( SOAConstants.NPA_COL ) 
					|| columnNode[0].equals( SOAConstants.NXX_COL )
					|| columnNode[0].equals( SOAConstants.DASHX_COL ) )
				{
				
					// combine npa,nxx ,dashx value
					npaNxxX.append( columnNode[1] );
				
				}else if( columnNode[0].equals( 
										SOAConstants.EFFECTIVEDATE_COL ) )
				{
				    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log( Debug.SYSTEM_CONFIG,
										"Creating Node : NpaNxxXValue" );
					}
				
					response.setValue( SOAConstants.NPA_NXX_X_DATA_PATH 
											+ "(" + i + ").NpaNxxXValue"  , 
											npaNxxX.toString() );
				    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
										"Creating Node : " + columnNode[2] );
					}
				
					response.setValue( SOAConstants.NPA_NXX_X_DATA_PATH 
										+ "(" + i + ")." + columnNode[2] , 
										columnNode[1] );
				
				}else if( columnNode[0].equals( 
											SOAConstants.OBJECT_CREATION_DATE_COL ) )
				{
				    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
									"Creating Node : " + columnNode[2] );
					}
								
					response.setValue( SOAConstants.NPA_NXX_X_DATA_PATH 
							+ "(" + i + ")." + columnNode[2] , columnNode[1] );
							
					response.create( SOAConstants.NPA_NXX_X_DATA_PATH 
									+ "(" + i + ").NpaNxxXModifiedTimestamp" );
				    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
												"Creating Node : " + 
												SOAConstants.DOWNLOAD_REASON_NODE );
					}

					response.setValue( SOAConstants.NPA_NXX_X_DATA_PATH 
								+ "(" + i + ")" + 
								SOAConstants.NPA_NXX_X_DOWNLOAD_REASON_NODE , 
								SOAConstants.DOWNLOAD_REASON_NEW );
								
				}
				else if( columnNode[0].equals(SOAConstants.STATUS_COL))
				{
					response.setValue( SOAConstants.NPA_NXX_X_DATA_PATH 
							+ "(" + i + ")." + columnNode[2] , 
							columnNode[1] );
				}
				else
				{
			
			        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}
					
					response.setValue( SOAConstants.NPA_NXX_X_DATA_PATH 
							+ "(" + i + ")." + columnNode[2] , columnNode[1] );
				
				}			  
			
			}
		
			// forward to add another record
			i++ ;	
		
		}	
	
	}
		
	/**
	 * This method generates query reply XML for LRN query request.  
	 *
	 * @param  dbCon  The database connection to perform the SQL SELECT 
	 * 					operation against.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	private void generateLRNQueryReply(	XMLMessageGenerator response ) 
										throws MessageException ,
										ProcessingException {
	
		Iterator iter = list.iterator( );			
	
		List arrayList = null;
	
		Iterator iterator = null;
	
		int i = 0 ;	
	
		// loop over the all list
		while ( iter.hasNext() )
		{
		
			arrayList =  ( ArrayList ) iter.next();
		
			iterator = arrayList.iterator();
			
			response.create( SOAConstants.LRN_DATA_PATH + "(" + i + ").LrnId");
		
			// loop over all column node array
			while( iterator.hasNext() )
			{
								
				String [] columnNode = ( String [] )iterator.next();				
							
				if( columnNode[0].equals( SOAConstants.SPID_COL ) )
				{
                    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}
		
					response.setValue( SOAConstants.LRN_DATA_PATH 
							+ "(" + i + ")." + columnNode[2] , columnNode[1] );
                    
					if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + 
											SOAConstants.DOWNLOAD_REASON_NODE );
					}

					response.setValue( SOAConstants.LRN_DATA_PATH 
								+ "(" + i + ")" + 
								SOAConstants.LRN_DOWNLOAD_REASON_NODE , 
								SOAConstants.DOWNLOAD_REASON_NEW );
								
					response.create( SOAConstants.LRN_DATA_PATH + 
											"(" + i + ").LrnCreateTimestamp");
					
		
				}
				else if( columnNode[0].equals(SOAConstants.STATUS_COL))
				{
					response.setValue( SOAConstants.LRN_DATA_PATH 
							+ "(" + i + ")." + columnNode[2] , 
							columnNode[1] );
				}
				else
				{
			        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG,
											"Creating Node : " + columnNode[2] );
					}
					
					response.setValue( SOAConstants.LRN_DATA_PATH 
							+ "(" + i + ")." + columnNode[2] , columnNode[1] );	
				
				}			
							
			}
		
			// forward to add another record
			i++ ;	
		
		}		
	
	}
   	
	
   	
	/**
	 * This method used to get FailedSP List from table .  
	 *
	 * @param  dbCon  The database connection to perform the SQL SELECT 
	 * 					operation against.
	 * @param  id     String as 'SVID' or 'NBRID'
	 * @param  objectType String i.e 'SV' or 'NBR'
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
   	private String getFailedSPList( Connection dbCon , 
   									String id , 
   									String objectType )
   									throws ProcessingException 
   	{
   		   				
		PreparedStatement pstmt = null;
	   	
		ResultSet rs = null;
		
		StringBuffer sqlQuery = new StringBuffer();
   		
		sqlQuery.append(" SELECT ");

		sqlQuery.append( SOAConstants.FAILED_SP_LIST_COL );

		sqlQuery.append(" FROM ");

		sqlQuery.append( SOAConstants.SOA_SV_FAILEDSPLIST_TABLE );

		sqlQuery.append( " WHERE ");

		sqlQuery.append( SOAConstants.ID_COL );

		sqlQuery.append( " = '");

		sqlQuery.append( id );

		sqlQuery.append( "' AND ");
		
		sqlQuery.append( SOAConstants.REGION_COL );

		sqlQuery.append( " = ");

		sqlQuery.append( regionId );

		sqlQuery.append( " AND ");

		sqlQuery.append( SOAConstants.OBJECTTYPE_COL );	
		
		sqlQuery.append( " = '" );
		
		sqlQuery.append( objectType );

		sqlQuery.append( "'");	
		
		String failedSPList = null;			

		try
		{
			if( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){   		
				Debug.log( Debug.NORMAL_STATUS, 
							"Executing Select SQL for FailedSP List:\n" 
							+ sqlQuery.toString() );
			}

			// Get a prepared statement for the SQL statement.
			pstmt = dbCon.prepareStatement( sqlQuery.toString() );

			// Execute the SQL SELECT operation.
			rs = pstmt.executeQuery( );					

			// Loop over row returned by query ...
			if(  rs.next() )
			{
				
				failedSPList = rs.getString( SOAConstants.FAILED_SP_LIST_COL );
				if( Debug.isLevelEnabled( Debug.DB_DATA ) ){ 
					Debug.log( Debug.DB_DATA, 
										"Failed SP List value:\n" + failedSPList );
				}
					   	
			}		   
   
		}
		catch ( SQLException sqle )
		{

			throw new ProcessingException( "ERROR: Could not select row from " +
				"database table [" + SOAConstants.SOA_SV_FAILEDSPLIST_TABLE + 
				"]:\n" + DBInterface.getSQLErrorMessage(sqle) );
   						
		}
		finally
		{
			if ( pstmt != null )
			{
				try
				{
					if(rs != null)
					{
	   		
						rs.close();
						
						rs = null ;
	   			
					}
	   					   		
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
   		
   		return failedSPList ;
   	}
   	
   	
   	/**
	 * Construct the SQL Select statement from the column data.
	 *
	 * @return SQL Select statement.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
   	private String constructSelectSqlStatement()throws ProcessingException, DatabaseException
   	{
	    if( Debug.isLevelEnabled( Debug.DB_DATA ) ){ 
		  Debug.log( Debug.DB_DATA, "Preparing SQL SELECT statement ..." );
		}
		
		Iterator iter = columns.iterator( );
		
		StringBuffer sqlSelect = new StringBuffer();
		
		StringBuffer whereClause = new StringBuffer();

		boolean whereClauseFlag = false;
		
		if( spidFlag )
		{
				
			whereClause.append( SOAConstants.SPID_COL  );
			
			whereClause.append( " = ? " );
			
			whereClauseFlag = true ;			
		
		}
		
		if ( useCustomerId ) 
		{
			
			if ( whereClauseFlag )
			{
	
				whereClause.append( " AND " );
	
			}else
			{
	
				whereClauseFlag = true;
	
			}
			
			try {
				
				SQLBuilder.insertCustomerID( whereClause );
				
				whereClause.append( " = ?" );
				
			} catch (DatabaseException e) {
				
				throw new ProcessingException( "ERROR: Could not add customerID.." );
				
			}
	
			
		}
		
		boolean flag = true;		
		
		ColumnData cd = null;		
		
		sqlSelect.append("SELECT ");
		
		while( iter.hasNext() )
		{
			
			cd = (ColumnData)iter.next( );			
			
			//	if start value is not null 		
			if( ! SOAUtility.isNull( cd.startValue ) )
			{
				
				// Get the Region Id value which is required to query Failed 
				// SP list table to get failed SP list
				if( ( cd.columnName ).equalsIgnoreCase( SOAConstants.REGION_COL ) )
				{
					
					regionId = ( String )cd.startValue ;
					if( Debug.isLevelEnabled( Debug.DB_DATA ) ){ 
					  Debug.log( Debug.DB_DATA, "Region ID value [ "+ regionId +" ]" ); 
					}
					
				}
				// if end value is not null then prepare where clause
				// for less than and greater than
				if ( ! SOAUtility.isNull( cd.endValue )  )
				{
					if ( whereClauseFlag )
					{
						
						whereClause.append( " AND " );
						
					}else
					{
						
						whereClauseFlag = true;
						
					}
								    
					
					whereClause.append( " ( " );
					
					whereClause.append( cd.columnName );			
		
					whereClause.append( " >=  ? AND  " );
					
					whereClause.append( cd.columnName );			
		
					whereClause.append( " <=  ?   " );
					
					whereClause.append( " ) " );
					
				}else  
				{
	
					if ( whereClauseFlag )
					{
						
						whereClause.append( " AND " );
						
					}else
					{
						
						whereClauseFlag = true;
						
					}			    
			
				    whereClause.append( cd.columnName );			
		
				    whereClause.append( " = ? " );
	
			   }
			  
			   
			}		
			
			if ( flag )
			{
					
				flag = false;
				 
			} 
			else
			{
				
				sqlSelect.append( ", " );
				 
			} 
				
			sqlSelect.append(cd.columnName);			
						
		}

		String customerId = null;

		try
        {
            customerId = CustomerContext.getInstance().getCustomerID( );
        }
        catch ( Exception e )
        {
            Debug.error( e.getMessage() );

            throw new DatabaseException( e.getMessage() );
        }

		if ( tableName.equalsIgnoreCase("SOA_LRN") || tableName.equalsIgnoreCase("SOA_NPANXX") || 
			tableName.equalsIgnoreCase("SOA_SPID") || tableName.equalsIgnoreCase("SOA_NPANXX_X") || 
			tableName.equalsIgnoreCase("SOA_NBRPOOL_BLOCK") )
		{
			if ( whereClauseFlag )
			{
				
				whereClause.append( " AND " );
				
			}
	 
			whereClause.append("exists (select * from TRADINGPARTNERRELATIONSHIP where transaction = 'SOARequest' and customerid = '");

			whereClause.append(customerId);

			whereClause.append("')");
		}

		
		sqlSelect.append(" FROM " + tableName );
		
		sqlSelect.append(" WHERE " + whereClause );		
		if( Debug.isLevelEnabled( Debug.DB_DATA ) ){ 
		  Debug.log( Debug.DB_DATA, "Done constructing SQL SELECT statement." );
		}
	
	   	return ( sqlSelect.toString() );
	   	
   }
   
   	/**
     * Populate the SQL SELECT statement from the column data.
     *
     * @param  pstmt  The prepared statement object to populate.
     *
     * @exception ProcessingException  thrown if population fails.
     */
  	private void populateSelectSqlStatement ( PreparedStatement pstmt ) 
  												throws ProcessingException
  	{
		if( Debug.isLevelEnabled( Debug.DB_DATA ) ){ 
	  	  Debug.log( Debug.DB_DATA, "Populating SQL SELECT statement ..." );
		}
	  	
	  	try
	  	{
	  		  	
		  	Iterator iter = columns.iterator( );
		  	
			SimpleDateFormat sdf = new SimpleDateFormat( dateFormat );
	
		  	int x = 0;		
		  	
			if( spidFlag )
			{
				
				pstmt.setString( ++x , spid );
				
			}
			
			if ( useCustomerId ) 
			{
				
				try { 
					
					SQLBuilder.populateCustomerID( pstmt , ++x );
					
				} catch (DatabaseException e) {
					
					throw new ProcessingException( "ERROR: Could not populate customerID value.." );
					
				}
				
			}
		  	
			ColumnData cd = null;
			
			String startVal = null ;
			
			String endVal = null;
			
			java.util.Date startDate = null;
			
			java.util.Date endDate = null;
				  
		  	for ( int Ix = 1;  iter.hasNext();  Ix ++ )
		  	{
			  	cd = (ColumnData)iter.next( );			        
			  	
			  	// populate only where clause values
			  	if(! SOAUtility.isNull( cd.startValue ) )
			  	{
			  		
			  		// Default is no column type specified.
			  		if ( !StringUtils.hasValue(cd.columnType) )
			  		{
						startVal = (String)( cd.startValue );
				  		
						endVal = (String)( cd.endValue );
	                    if( Debug.isLevelEnabled( Debug.MSG_DATA ) ){ 
							Debug.log( Debug.MSG_DATA, 
											"Start Value for column [" 
											+ cd.columnName+" ] is [" 
											+ startVal + "]." );
							Debug.log( Debug.MSG_DATA, 
										"End Value for column [" 
										+ cd.columnName+" ] is [" 
										+ endVal + "]." );
						}
										
						
	            
				  		pstmt.setObject( ++x, startVal );
				  		
				  		if( endVal != null )
				  		{
				  			
							pstmt.setObject( ++x, endVal );
				  			
				  		}
				  		
			  		}else if ( cd.columnType.equalsIgnoreCase(
			  									SOAConstants.COLUMN_TYPE_DATE) )
			  		{
				  		startVal = (String)( cd.startValue );
				  		
						endVal = (String)( cd.endValue );
	                    if( Debug.isLevelEnabled( Debug.MSG_DATA ) ){ 
							Debug.log( Debug.MSG_DATA, 
											"Start Value for date column is [" 
											+ startVal + "]." );
											
							Debug.log( Debug.MSG_DATA, 
											"End Value for date column is [" 
											+ endVal + "]." );
						}
						
											
						try
						{

							startDate = sdf.parse( startVal );

						}catch(ParseException parseException )
						{
	
							throw new ProcessingException( 
									"ERROR: could not parse start date" +
									"\for [" + cd.describe() + "] ." 
									+ parseException.toString() );
	
						}				  		
	
				  		pstmt.setTimestamp( ++x, 
				  			new java.sql.Timestamp( startDate.getTime()) );
				  		
				  		if ( endVal != null )
				  		{
				  								
							try
							{
	
								endDate = sdf.parse( endVal );
	
							}catch(ParseException parseException )
							{
		
								throw new ProcessingException( 
										"ERROR: could not parse end date" +
										"\for [" + cd.describe() + "] ." 
										+ parseException.toString() );
		
							}
		
							pstmt.setTimestamp( ++x, 
								new java.sql.Timestamp( endDate.getTime() ) );
					  			
					  			
					  	}
				  		
			  		}
			  		else
			  		{
			  			
				  		throw new ProcessingException( "ERROR: Invalid " +
				  			"column-type property value given in " +
				  			"configuration for [" + cd.describe() + "] ." );
				  								
			  		}
			  		
			  	}
			  	
		  	}
		  
		}catch( SQLException sqlException )
		{
			throw new ProcessingException( 
							"ERROR:  could not populate sql statement ."
							+ sqlException.toString() );
	
		}
        if( Debug.isLevelEnabled( Debug.MSG_DATA ) ){ 
	  	  Debug.log( Debug.MSG_DATA, "Done populating SQL SELECT statement." );
		}
	  
  	}
    

    /**
	 * Extract data for each column from the input message/context.
	 *
	 * @param context The message context.
	 * @param inputObject The input object.
	 *
	 * @exception MessageException  thrown if required value can't be found.
	 * @exception ProcessingException thrown if any other processing error .
	 */
	private void extractMessageData ( MessageProcessorContext context, 
									  MessageObject inputObject )
									  throws ProcessingException, 
									  		 MessageException
	{
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log (Debug.MSG_STATUS, "Extracting message data ..." );
		}

		Iterator iter = columns.iterator( );
		
		ColumnData cd = null;

		// While columns are available ...
		while ( iter.hasNext() )
		{
			cd = (ColumnData)iter.next( );

			// If end location was given, try to get a value from it.
			if ( StringUtils.hasValue( cd.startLocation ) )
			{
				// Location contains one or more alternative locations that 
				// could contain the column's value.
				StringTokenizer st 
						= new StringTokenizer( cd.startLocation, separator );
							
				String loc = null;
				
				// While location alternatives are available ...
				while ( st.hasMoreTokens() )
				{
					// Extract a location alternative.
					loc = st.nextToken().trim( );				

					// Attempt to get the value from the context 
					// or input object.
					if ( exists( loc, context, inputObject ) )
					{
					
						cd.startValue = get( loc, context, inputObject );
						
					} 

					// If we found a value, we're done with this column.
					if ( !SOAUtility.isNull(cd.startValue ))
					{
						if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
						Debug.log( Debug.MSG_DATA, 
								"Found value for column [" + cd.describe() + 
								"] at location [" + loc + "]." );
						}

						break;
						
					}
				}
			}

			// If no start value can be obtained ...
			if ( SOAUtility.isNull(cd.startValue ) )
			{
				
				cd.startValue = null;					
				 
			}
			
			//	if end location was given, try to get a value from it.
			 if ( StringUtils.hasValue( cd.endLocation ) )
			 {
				 // Location contains one or more alternative locations that 
				 // could contain the column's value.
				 StringTokenizer st 
							 = new StringTokenizer( cd.endLocation, separator );
					
				 String loc = null;
		
				 // While location alternatives are available ...
				 while ( st.hasMoreTokens() )
				 {
					 // Extract a location alternative.
					 loc = st.nextToken().trim( );				

					 // Attempt to get the value from the context 
					 // or input object.
					 if ( exists( loc, context, inputObject ) )
					 {
			
						 cd.endValue = get( loc, context, inputObject );
				
					 } 

					 // If we found a value, we're done with this column.
					 if ( !SOAUtility.isNull(cd.endValue ))
					 {
				         if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							 Debug.log( Debug.MSG_DATA, 
									 "Found value for column [" + cd.describe() + 
									 "] at location [" + loc + "]." );
						 }

						 break;
				
					 }
				 }
			 }

			 // If no value can be obtained ...
			 if ( SOAUtility.isNull( cd.endValue ) )
			 {
		
				 cd.endValue = null;					
		 
			 }

		}
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log (Debug.MSG_STATUS, "Done extracting message data ." );
		}
		
	}

    
    /**
     * Class ColumnData is used to encapsulate a description of a 
     * single column and its associated value.
     */
    private static class ColumnData
    {
		//	Property used to store Column Name for a column
        public final String columnName;
        
		//	Property used to store Column Type for a column
        public final String columnType;
        
		//	Property used to store location for a column's value
        public final String startLocation;
        
		//	Property used to store location for a column's value
		public final String endLocation;
        
		//	Property used to store optional flag
        public final String nodeName;
		
		//	Property used to store value for a column
        public Object startValue = null;
        
		//	Property used to store value for a column
		public Object endValue = null;

        public ColumnData ( String columnName, String columnType, 
        					String startLocation,String endLocation  , 
        					String nodeName ) 
        {
            this.columnName   		= columnName;
            
            this.columnType   		= columnType;  
                      
            this.startLocation     	= startLocation;
            
			this.endLocation     	= endLocation;
			
			this.nodeName   		= nodeName;
            
        }

		/**
		 * This method used to describe a column
		 */
        public String describe ( )
        {
            
            StringBuffer sb = new StringBuffer( );

            sb.append( "Column description: Name [" );
            
            sb.append( columnName );

            if ( StringUtils.hasValue( columnType ) )
            {
                sb.append( "], type [" );
                
                sb.append( columnType );
            }

            
            if ( StringUtils.hasValue( startLocation ) )
            {
                sb.append( "],Start location [" );
                
                sb.append( startLocation );
            }
            
			if ( StringUtils.hasValue( endLocation ) )
			{
				sb.append( "], End location [" );
				
				sb.append( endLocation );
			}

            sb.append( "], Node Name [" );
            
            sb.append( nodeName );

            if ( startValue != null )
            {
                sb.append( "], Start value [" );
                
                sb.append( startValue );
            }
            
			if ( endValue != null )
			{
				sb.append( "], End value [" );
				
				sb.append( endValue );
			}

            sb.append( "]." );

            return( sb.toString() );
        }
    }
    /**
     * Append null clause fields if present in message xml
     * @param inputObject
     * @param sb
     * @return modified sql statement
     * @throws ProcessingException
     */
    private String appendNullClause(MessageObject inputObject , StringBuffer sb) throws ProcessingException{
    	
    	BufferedReader input = null;
    	Document doc = null;
	   	Element elem = null;
	   	NodeList nodelist = null;
	   	String null_clause_field = null;
	   	HashMap<String, String> null_map = null;
	   	
	   	String path = "./gateways/neustar-soa/maps/null_clause_feild_mapping.properties";
	   	File mappingFile = new File(path);
		try{
		input = new BufferedReader(new FileReader(mappingFile));
		null_map = populateNullClauseMap(input);	
			
		doc = inputObject.getDOM();
   		elem = doc.getDocumentElement();
   	
   		nodelist = elem.getElementsByTagName(NULL_CLAUSE_FIELDS);
   		if(nodelist != null && nodelist.getLength() > 0){
   			
   			Element temp = (Element) nodelist.item(0); //node
   			
   			if(temp != null ){
   				
   				null_clause_field = temp.getAttribute("value");
   				if( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) ){	
   					Debug.log( Debug.NORMAL_STATUS, 
   										"NULL_CLAUSE_FEILDS :\n" + null_clause_field);
   				}
   				
   				StringTokenizer st = new StringTokenizer( null_clause_field, SEPARATOR );
   				for (int Ix = 0;  st.hasMoreTokens();  Ix ++ ){
   					String subVal = st.nextToken( );
   					if(null_map.containsKey(subVal)){
   				    	
   						sb.append(SQL_AND_CLAUSE);
   				        		        
				        sb.append(null_map.get(subVal));
				        
				        sb.append( SQL_IS_NULL_CLAUSE );
   				    }
   				}
   			}
        }
   		}catch ( MessageException e ){
	   	
		   	throw new ProcessingException( "ERROR: Could not select row " +
		   		"from database table [" + tableName  + "]:\n" + e.toString() );
		   		
	   	}catch (FileNotFoundException fnf) {
		
			throw new ProcessingException( ": Could not find the file on given path [" + path + "]" +
						"\n" + fnf.toString() );
			
	   	}catch (IOException io) {

			throw new ProcessingException( ": IoException occured " + "\n" + io.toString() );
			
	   	}	
		
    	return sb.toString();
    }
    /**
     * Populate the static hashmap with given property file.
     * @param input
     * @return
     * @throws IOException
     */
    private static HashMap<String, String> populateNullClauseMap(BufferedReader input) throws IOException{
    	String line = null;
		while ((line = input.readLine()) != null) {
			if (line.startsWith("#") || line.equals("")) {
				continue;
			} else {
				/*
				 * Check for '=' operand is present in mapping string.
				 * If '=' is present then, 
				 * Check for mapping value, and it must be present.
				 * 
				 * Throws exception if mapping file is wrong.
				 */
				if (line.contains("=")) {
					if (line.charAt(line.length() - 1) != '=') {
						String[] lineArray = line.split("=");
						if (!((lineArray[0].equals("") || lineArray[0] == null) && (lineArray[1].equals("") || lineArray[1] == null))) {
								null_clause_map.put(lineArray[0].trim(), lineArray[1].trim());
							}
						} else {
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
								Debug.log(Debug.MSG_STATUS, " : Mapping string " + line.toString()
										+ " does not contain any mapping value after '=' operand");
						}
					}
				} else {
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
						Debug.log(Debug.MSG_STATUS, " : Mapping string " + line.toString()
								+ " does not contain '=' operand, Invalid Mapping line in mapping configuration file.");
					}
						
				}
			}
		}
		Debug.log(Debug.MSG_STATUS, " : Hash map loading is done...");

		return null_clause_map;
    	
    }
    
    
//	--------------------------For Testing---------------------------------//

  	public static void main(String[] args) {

	  	Properties props = new Properties();
	
	  	props.put( "DEBUG_LOG_LEVELS", "all" );
	
	  	props.put( "LOG_FILE", "e:\\SOAlog.txt" );
	
	  	Debug.showLevels( );
	
	  	Debug.configureFromProperties( props );
	
	  	if (args.length != 3)
	  	{
			Debug.log (Debug.ALL_ERRORS, "SOALocalQuery: USAGE:  "+
			" jdbc:oracle:thin:@192.168.1.240:1521:soa ashok ashok ");
			return;
			
	  	}
	  	try
	  	{

		  	DBInterface.initialize(args[0], args[1], args[2]);
		
	  	}
	  	catch (DatabaseException e)
	  	{
		   	Debug.log(null, Debug.MAPPING_ERROR, ": " +
					"Database initialization failure: " + e.getMessage());
	  	}


		SOALocalQuery soaLocalQuery = new SOALocalQuery();

	  	try
	  	{
			soaLocalQuery.initialize("FULL_NEUSTAR_SOA","SOALocalQuery");
		
		  	MessageProcessorContext mpx = new MessageProcessorContext();
		
		  	MessageObject mob = new MessageObject();			
		
		  	mob.set("SPID","1111");	
		  	
			//mob.set("STARTDATE","10-20-2003-073200PM");
			
			//mob.set("ENDDATE","10-21-2003-073200PM");
		  	
			mob.set("ReplyType","NumberPoolBlockQueryReply");
			
			mob.set("REGIONID","3");
			
			//mob.set("STARTSVID","1");							
		
			soaLocalQuery.process(mpx,mob);
		
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
