package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.RegexUtils;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;


public class MapNPBNotification extends MessageProcessorBase {
	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
	private boolean usingContextConnection = true;

	/**
	 * Value of input XML location
	 */
    private String inputXMLLoc = null;

	/**
	 * Value of spid location
	 */
    private String spidLoc = null;

	/**
	 * Value of status location
	 */
    private String statusLoc = null;

    /**
	 * Value of blockId (NPBID) location
	 */
    private String npbidLoc = null;
    
    /**
	 * Value of blockId (NPBID) location
	 */
	private String npanxxXLoc = null;	
	
    /**
	 * Value of output XML location
	 */
	private String outputXMLLoc = null;

	/**
	 * Value of NPAC Notification location
	 */
    private String npacNotificationLoc = null;

	/**
	 * Value of spid
	 */
	private String spid = null;

	/**
	 * Value of status
	 */
	private String status = null;

	/**
	 * Value of NPAC notification
	 */
	private String npacNotification = null;

	/**
	 * Last request from database
	 */
	private String lastRequestType ;
	
	/**
	 *  Request type from database
	 */
	private String lastResponse = null;

	private String npbId = null;
	
	private String npaNxxXValue = null;
	
	private String npa = null;
	
	private String nxx = null;
	
	private String dashX = null;
	
	private String dbSpid = null;
	
	private String outputNPBNotificationLoc = null;
	
	private String npbStatus = null;

	/**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties
     *
     * @exception ProcessingException when initialization fails
     */
	public void initialize ( String key, String type )throws ProcessingException
    {
		// Call base class method to load the properties.
		super.initialize( key, type );

		// Get configuration properties specific to this processor.
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log( Debug.SYSTEM_CONFIG, "MapNPBNotification: Initializing..." );
		}

		StringBuffer errorBuffer = new StringBuffer( );

		inputXMLLoc = getRequiredPropertyValue(
						SOAConstants.INPUT_XML_MESSAGE_LOC_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Location of Input XML : "
													+ inputXMLLoc + "." );
		}

		spidLoc = getRequiredPropertyValue( SOAConstants.SPID_PROP , errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log( Debug.SYSTEM_CONFIG, "Location of SPID : " + spidLoc + "." );
		}


		npbidLoc = getPropertyValue(SOAConstants.BLOCKID_PROP);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log( Debug.SYSTEM_CONFIG, "Location of BLOCKID : " + npbidLoc + "." );
		}

		
		npanxxXLoc = getRequiredPropertyValue( SOAConstants.NPANXX_X_PROP , errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log( Debug.SYSTEM_CONFIG, "Location of NPA_NXX_DASHX : " + npanxxXLoc + "." );
		}

		
		statusLoc = getPropertyValue( SOAConstants.BLOCK_STATUS );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Status : "
																+ statusLoc + "." );
		}

		outputXMLLoc = getRequiredPropertyValue(
						SOAConstants.OUTPUT_XML_MESSAGE_LOC_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Output XML : "
														+ outputXMLLoc + "." );
		}

		npacNotificationLoc = getRequiredPropertyValue(
				SOAConstants.NPAC_NOTIFICATION_LOC_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of NPAC Notification : "
													+ npacNotificationLoc + "." );
		}
		outputNPBNotificationLoc = getRequiredPropertyValue( SOAConstants.OUTPUT_NPBSOA_NOTIFICATION ,errorBuffer );
        
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log( Debug.SYSTEM_CONFIG, "Location of Output TN :"+ outputNPBNotificationLoc + "." );
		}

		String strTemp = getPropertyValue( SOAConstants.TRANSACTIONAL_LOGGING_PROP );
				
		if ( StringUtils.hasValue( strTemp ) )
		{
			try {

				usingContextConnection = getBoolean( strTemp );

			}
			catch ( FrameworkException e )
			{

				errorBuffer.append ( "Property value for "
								+ SOAConstants.TRANSACTIONAL_LOGGING_PROP
								+ " is invalid. " + e.getMessage ( ) + "\n" );

			}
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
						"Logger will participate in overall driver transaction? ["
							   + usingContextConnection + "]." );
		}

		//	If any of the required properties are absent,indicate error to caller
		 if ( errorBuffer.length() > 0 )
		 {

			 String errMsg = errorBuffer.toString( );

			 Debug.log( Debug.ALL_ERRORS, errMsg );

			 throw new ProcessingException( errMsg );

		 }
         if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			 Debug.log( Debug.SYSTEM_CONFIG,
									"MapNPBNotification: Initialization done." );
		 }
    }

    /**
     * This method will extract the data values from the context/input,
     * and perform DB lookups and apply business logic with the input values
     * to determine the exact NPB Notifications.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext ctx,
    						  MessageObject obj )
                        					throws MessageException,
                        						   ProcessingException
    {
    	ThreadMonitor.ThreadInfo tmti = null;
		if ( obj == null )
		{
            return null;
		}

		Connection dbConn = null;

		String lastNPBRequest = null;

		String inputXML = null;

		String soaNotification = null;

		String outputXML = null;

		XMLMessageParser parser = null;

		try
		{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	        // get input XML from context
			Document doc
				= (Document) super.getDOM( inputXMLLoc , ctx , obj );

			XMLMessageGenerator generator = new XMLMessageGenerator( doc );

			inputXML = generator.getMessage();
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Input XML value :\n\t"
															+ inputXML + ".\n" );
			}
	        // Get SPID value from context			
	        spid = getString(spidLoc, ctx, obj);
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, "SPID Value : [ " + spid + " ]." );
			}

			//	Get Status value from context
			status = getString(statusLoc, ctx, obj );
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, "Status value :[ " + status + " ]." );
			}

			// Get NPBID value from context 
			npbId = getString(npbidLoc, ctx, obj);
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, "Block ID value :[ " + npbId + " ]." );
			}
			
			// Get npanxxX value from context
			npaNxxXValue = getString(npanxxXLoc, ctx, obj);
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, "NPA XXX X value :[ " + npaNxxXValue + " ]." );
			}
			
			//	Get NPAC Notification value from context
			npacNotification = getString(npacNotificationLoc, ctx, obj );

            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, " Value of NPAC Notification :[ "
														+ npacNotification + " ]." );
			}

			// Get a database connection from the appropriate location - based
			// on transaction characteristics.
			if ( usingContextConnection )
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database logging is "
						+ "transactional, so getting connection from context." );
				}

				dbConn = ctx.getDBConnection();
			}
			else
			{
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS, "Database logging is not "
								+ "transactional, so getting connection "
								+ "from NightFire pool." );
				}

				try
				{
					dbConn = DBConnectionPool.getInstance( true )
							.acquireConnection( );
					
				}catch (ResourceException e)
				{
					String errMsg
						= "ERROR: MapSOANotification: Attempt to get database"
											+ " connection failed with error: "
											+ e.getMessage( );

					Debug.log(Debug.ALL_ERRORS, errMsg);
				}
			}

			if( dbConn == null )
			{
				throw new ProcessingException( "Connection is not acquired ," +
														"it is null ");
			}

			//	Getting last response from Database by Quering on NPB object table
			lastNPBRequest = getDBData(dbConn);

			//	Getting SOA Notification		
			soaNotification = getMapNpbStatus(lastNPBRequest);
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, " Value of SOA Notification :[ "
														+ soaNotification + " ]." );
			}

			if( soaNotification == null )
			{
				// Release the connection before throw the exception
				if ( !usingContextConnection )
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
				throw new ProcessingException( "The SOA notification type " +
											"could not be found for the NPAC "+
										   	"notification type ["+
										   	npacNotification+"]." );
			}
			
			//	Replace NPAC notification with SOA notification
			outputXML = inputXML.replaceAll( npacNotification,soaNotification );
			
			// Remove all nodes except BlockId node if NPB Activate Notification is received.
			if(soaNotification == SOAConstants.NBR_POOLBLK_ACTIVATE_NOTIFICATION){
				
				outputXML = removeNodes(outputXML, soaNotification);
			}
			
			CharSequence message = (CharSequence)outputXML;
			message = SOAUtility.removeWhitespace(message).toString();
			message = RegexUtils.replaceAll("> <", message.toString(), ">\n<");
			
			outputXML = message.toString();
			parser = new XMLMessageParser( outputXML.toString() );
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Output SOA Notification XML: \n\n"
													+ parser.getMessage() + "\n\n" );
			}
		}
		catch ( ProcessingException e )
		{

			Debug.log( Debug.ALL_ERRORS, e.toString() );

			// Re-throw the exception to the driver.
			throw new ProcessingException( e.toString() );

		}
		catch ( FrameworkException e )
		{

			Debug.log( Debug.ALL_ERRORS, e.toString() );

			// Re-throw the exception to the driver.
			throw new ProcessingException( e.toString() );

		}

		finally
		{
			ThreadMonitor.stop(tmti);
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

		// Setting Output XML in to context
		super.set( outputXMLLoc , ctx , obj , parser.getDocument() );

		// Setting NPB SOA Notification in to context
		super.set(outputNPBNotificationLoc, ctx, obj, soaNotification);
		
		// Pass the input on to the output.
        return( formatNVPair( obj ) );

    }

	/**
	 * This method will Map NumberPoolBlockStatusAttributeValueChangeNotification into
	 * SOA notification based on last Response, and status.
	 *
	 * @param  lastmessage as String
	 *
	 * @return  String as SOA Notification .
	 *
	 */
	private String getMapNpbStatus( String lastNPBRequestType )
	{

		String notification = SOAConstants.NBR_POOLBLK_STATUS_CH_NOTIFICATION;

		//	if status is 'active'
		if( status.equals(SOAConstants.ACTIVE_STATUS) )
		{
			if ((lastNPBRequestType != null && lastNPBRequestType.equals(SOAConstants.NPB_MODIFY_REQUEST)) || npbStatus.equals("active"))
			{
				notification = SOAConstants.NBR_POOLBLK_STATUS_CH_NOTIFICATION;

				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

					Debug.log( Debug.MSG_STATUS, "Last Request Type on this record in DB is : ["
							+ lastNPBRequestType + "]" );

                    Debug.log( Debug.MSG_STATUS, "Status in incoming XML is : ["
						+ status + "]" );
				}
				
				
			}
			else
			{
				notification = SOAConstants.NBR_POOLBLK_ACTIVATE_NOTIFICATION ;
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log( Debug.MSG_STATUS, "Last Request Type on this record in DB is NOT "
							+ SOAConstants.NPB_MODIFY_REQUEST + "but, [" + lastNPBRequestType + "]" );
					
					Debug.log( Debug.MSG_STATUS, "Status in incoming XML is : ["
							+ status + " status]" );
				}
			}
		}
		
		return notification;
		
	}


	/**
	 * This method will retrieve Last Response , last Request Type, SPID from Database
	 *
	 * @param  dbConn  The database connection to perform the SQL
   	 * SELECT operation against.
	 *
	 * @return  Last Response , or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private String getDBData( Connection dbConn )
											throws ProcessingException
	{
		boolean flag = false;

		String comma = " , ";

		StringBuffer queryMessage = new StringBuffer();

		queryMessage.append( "SELECT ");
		
		queryMessage.append( SOAConstants.SPID_COL );
		
		queryMessage.append( comma );
			
		queryMessage.append( SOAConstants.NPB_LASTREQUESTTYPE );

		queryMessage.append( comma );
		
		queryMessage.append( SOAConstants.NPB_LASTRESPONSE );
		
		queryMessage.append( comma );
		
		queryMessage.append( SOAConstants.STATUS_COL );
		
		queryMessage.append( " FROM " );

		queryMessage.append( SOAConstants.NBRPOOL_BLOCK_TABLE );

		queryMessage.append( " WHERE "+SOAConstants.SPID_COL + " = ?" );		
		
		if (!npaNxxXValue.equals("null") && !npbId.equals("null")){
			
			queryMessage.append( " AND " + SOAConstants.NPA_COL + " = ?");
			
			queryMessage.append( " AND " + SOAConstants.NXX_COL + " = ?");
			
			queryMessage.append( " AND " + SOAConstants.DASHX_COL + " = ?");
			
			queryMessage.append( " AND " + SOAConstants.NPBID_COL + " = ?");
			
			flag = true;
			
		}
		if (!npaNxxXValue.equals("null") && !flag){
			
			queryMessage.append( " AND " + SOAConstants.NPA_COL + " = ?");
			
			queryMessage.append( " AND " + SOAConstants.NXX_COL + " = ?");
			
			queryMessage.append( " AND " + SOAConstants.DASHX_COL + " = ?");
		}
		
		if (!npbId.equals("null") && !flag){
			
			queryMessage.append( " AND " + SOAConstants.NPBID_COL + " = ?");
			
		}
		//SOA_NBRPOOL_BLOCK_INDEX_1
		//XPK_SOA_NBRPOOL_BLOCK		
		//queryMessage.insert(7, "/*+ INDEX(" + SOAConstants.SV_TABLE + " SOA_SV_INDEX_2 ) */ ");
		if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		  Debug.log( Debug.NORMAL_STATUS, "Database query is :[ \n" + queryMessage +" ]");
		}

		PreparedStatement pstmt = null;

	  	ResultSet rs = null;

	  	String lastNPBRequestType = null;

	  	try
	  	{

		  	// Get a prepared statement for the SQL statement.
		  	pstmt = dbConn.prepareStatement( queryMessage.toString() );

		  	pstmt.setString( 1, spid );

		  	if (!npaNxxXValue.equals("null") && !npbId.equals("null")){
		  		
		  		npa = npaNxxXValue.substring(0, 3);
				nxx = npaNxxXValue.substring(3, 6);
				dashX = npaNxxXValue.substring(6);
				
				pstmt.setString( 2, npa );
				
				pstmt.setString( 3, nxx );
				
				pstmt.setString( 4, dashX );
				
				pstmt.setString( 5, npbId );
		  	}
		  	
		  	if (!npaNxxXValue.equals("null") && !flag){
		  		
		  		pstmt.setString( 2, npa );
				
				pstmt.setString( 3, nxx );
				
				pstmt.setString( 4, dashX );
		  	}
		  	
		  	if (!npbId.equals("null") && !flag){
		  		
		  		pstmt.setString( 2, npbId );
		  	}
		  	
		   	// Execute the SQL SELECT operation.
		  	rs = pstmt.executeQuery( );

		  	if ( rs.next() )
		  	{

		  		dbSpid = rs.getString( SOAConstants.SPID_COL );
		  		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
		  		 Debug.log( Debug.DB_DATA, " SPID From database : " + dbSpid );
				}
		  		
		  		lastNPBRequestType = rs.getString( SOAConstants.NPB_LASTREQUESTTYPE );
                if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				  Debug.log( Debug.DB_DATA, " LASTREQUESTTYPE From database : " + lastRequestType );
				}

				lastResponse = rs.getString( SOAConstants.NPB_LASTRESPONSE );
                
				if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				 Debug.log( Debug.DB_DATA, " LASTRESPONSE From database : " + lastResponse );
				}
				
				npbStatus = rs.getString( SOAConstants.STATUS_COL );

                if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				  Debug.log( Debug.DB_DATA, " STATUS From database : " + npbStatus );
				}
				
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

		return lastNPBRequestType;
	}
	
	/**
	 * This method used to remove some extra nodes from output XML 
	 * if NPBActivateNotification is mapped.
	 *
	 * @param  xml as a string
	 * @param  soaNotification  as a string
	 *
	 * @return  XMLMessageParser
	 *
	 * @exception  MessageException  Thrown if node name not found.
	 *
	 */
	public String removeNodes (String xml, String soaNotification) throws MessageException
	{
		
		XMLMessageParser parser = new XMLMessageParser(xml);
			
		String rootNode = "SOAToUpstream.SOAToUpstreamBody." + soaNotification ;
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
		 Debug.log( Debug.MSG_STATUS, " The mapped SOA Notification is NumberPoolBlockActiveNotification");
		}
		
		if(soaNotification.equals(SOAConstants.NBR_POOLBLK_ACTIVATE_NOTIFICATION)){
			
			if(parser.exists(rootNode + ".NpaNxxX")){
				
				parser.removeNode(rootNode + ".NpaNxxX");

				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				  Debug.log( Debug.MSG_STATUS, " The node NpaNxxX is present in XMl so removing it.");
				}
			
			}
			if(parser.exists(rootNode + ".BlockStatus")){
			
				parser.removeNode(rootNode + ".BlockStatus");
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				  Debug.log( Debug.MSG_STATUS, " The node BlockStatus is present in XMl so removing it.");
				}
				
			}
			if(parser.exists(rootNode + ".FailedServiceProviderList")){
		
				parser.removeNode(rootNode + ".FailedServiceProviderList");
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				 Debug.log( Debug.MSG_STATUS, " The node FailedServiceProviderList is present in XMl so removing it.");
				}
				
			}
		}
		
		return parser.getMessage();
	}
	
}

