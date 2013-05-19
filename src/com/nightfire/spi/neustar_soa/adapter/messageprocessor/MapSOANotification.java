/**
 * The purpose of this processor is to determine the exact SOA notification
 * to be mapped with the NPAC Notification received from NPAC based
 * on DB lookups and business logic
 *
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair
 * @see			com.nightfire.framework.util.StringUtils
 * @see			com.nightfire.framework.db.DBInterface
 * @see			com.nightfire.spi.common.driver.MessageProcessorBase
 * @see			com.nightfire.spi.common.driver.MessageObject
 * @see			com.nightfire.spi.common.driver.MessageProcessorContext
 *
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			06/01/2004			Created
	2			Ashok			06/04/2004			Review Comments incorporated
	3			Ashok			06/18/2004			Constants have been moved to
													SOAConstants class
	4			Ashok			06/19/04			Review comments from Jigar
													have been incorporated
	5			Ashok			07/08/2004			NewSP,OldSP putting in
													context and removing
													extra tag(s)
	6			Ashok			07/12/2004			Check added before deleting
													any node
	7			Ashok			07/29/2004			Formal review comments
													incorporated
	8			Ashok			09/21/2004			Modified to support
													non-contiguous SvId range
	9			Ashok			09/30/2004			RegionId added in DB Query ,If query's
													where clause contains ID.

 */


package com.nightfire.spi.neustar_soa.adapter.messageprocessor;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.RegexUtils;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;


public class MapSOANotification extends MessageProcessorBase {
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
	 * Value of input newSP location
	 */
    private String inputNewSPLoc = null;

	/**
	 * Value of input oldSp location
	 */
    private String inputOldSPLoc = null;

	/**
	 * Value of status location
	 */
    private String statusLoc = null;

	/**
	 * Value of Start Svid  location
	 */
    private String startSvidLoc = null;

	/**
	 * Value of End Svid location
	 */
    private String endSvidLoc = null;

	/**
	 * Value of Old Authorization flag location
	 */
	private String oldAuthorizationLoc = null;

	/**
	 * Value of Old Failed SP flag location
	 */
	private String failedSPLoc = null;

	/**
	 * Value of input tn location
	 */
	private String inputTnLoc = null;

	/**
	 * Value of output XML location
	 */
	private String outputXMLLoc = null;

	/**
	 * Value of output tn location
	 */
    private String outputTnLoc = null;

	/**
	 * Value of output endstation location
	 */
    private String outputEndStationLoc =  null;

	/**
	 * Value of output newSP location
	 */
	private String outputNewSPLoc = null;

	/**
	 * Value of output oldSp location
	 */
	private String outputOldSPLoc = null;

	/**
	 * Value of output cause code location
	 */
	private String causeCodeLoc = null;

	/**
	 * Value of output oldSp due date location
	 */
	private String dueDateLoc = null;

	/**
	 * Value of NPAC Notification location
	 */
    private String npacNotificationLoc = null;

	/**
	 * Value of spid
	 */
	private String spid = null;

	/**
	 * Value of newSP
	 */
	private String newSP = null;

	/**
	 * Value of oldSP
	 */
	private String oldSP = null;

	/**
	 * Value of status
	 */
	private String status = null;

	/**
	 * Value of Start Svid
	 */
	private String startSvid = null;

	/**
	 * Value of End Svid
	 */
	private String endSvid = null;

	/**
	 * Value of Old Authorization flag
	 */
	private int oldAuthorization = -1 ;

	/**
	 * Value of tn
	 */
	private String tn = null;

	/**
	 * Value of endstation
	 */
	private String endStation = null;

	/**
	 * Value of porting to original
	 */
	private int portingToOriginal = 0;

	/**
	 * Value of failed SP flag
	 */
	private int failedSP = 0;

	/**
	 * Value of cause code
	 */
	private String causeCode = null;

	/**
	 * Value of Old SP Due Date
	 */
	private String dueDate = null;

	/**
	 * Value of output date format
	 */
	private String dateFormat = null;

	/**
	 * Value of NPAC notification
	 */
	private String npacNotification = null;

	/**
	 * value of authorization flag from database
	 */
	private String dbAuthorization = null;

	/**
	 * Location of New SP Creation timestamp
	 */
	private String creationTimestampLoc = null;

	/**
	 * Value of New SP Creation time stamp
	 */
	private String creationTimestamp = null;

	/**
	 * Last request from database
	 */
	private String lastRequestType = null;
	
	/**
	 *  Request type from database
	 */
	private String requestType = null;

	/**
	 * Status from database
	 */
	private String dbStatus= null;

	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext context = null;

	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject object = null;

	/**
	 * This variable contains  child element count of TnSvIdList node
	 */
	private int childElement = 0;

	/**
	 * 	This variable used to get location of Region ID.
	 */
	private String regionIdLoc = null;

	/**
	 * 	This variable used to get value of Region ID.
	 */
	private String regionId = null;
	
	/**
	 * value of nnspduedate from database
	 */
	private String nnspduedate = null;

	/**
	 * value of onspduedate from database
	 */
	private String onspduedate = null;
	
	/**
	 * value of referencekey from database.
	 * Added in SOa 5.8.3 Release for CauseCode population
	 */
	private long referenceKey = -1;
	
	/**
	 * Select  query to fetch causeCode value from SOA_SUBSCRIPTION_VERSION table for SvReleaseInConflictRequest.
	 */
	private static final String SELECT_CAUSECODE_SQL = "SELECT /*+ index(SOA_SV_MESSAGE XPK_SOA_SV_MESSAGE) */ CAUSECODE FROM SOA_SV_MESSAGE WHERE  " +
	"MESSAGEKEY IN (SELECT MESSAGEKEY FROM SOA_MESSAGE_MAP WHERE REFERENCEKEY =  ?) " +
	"AND MESSAGETYPE = 'Request' AND MESSAGESUBTYPE = 'SvReleaseInConflictRequest' AND STATUS = 'Sent' ORDER BY DATETIME";

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
		  Debug.log( Debug.SYSTEM_CONFIG, "MapSOANotification: Initializing..." );
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

		inputNewSPLoc = getPropertyValue( SOAConstants.INPUT_NEWSP_LOC_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of input newSP : "
															+ inputNewSPLoc + "." );
		}

		inputOldSPLoc = getPropertyValue( SOAConstants.INPUT_OLDSP_LOC_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of input oldSP : "
															+ inputOldSPLoc + "." );
		}

		statusLoc = getPropertyValue( SOAConstants.STATUS_LOC_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Status : "
																+ statusLoc + "." );
		}

		startSvidLoc = getRequiredPropertyValue( SOAConstants.START_SVID_PROP ,
																errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Start Svid : "
														+ startSvidLoc + "." );
		}

		dateFormat = getRequiredPropertyValue(
						SOAConstants.OUTPUT_DATE_FORMAT_LOC_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "value of output date format : "
															+ dateFormat + "." );
		}

		endSvidLoc = getPropertyValue( SOAConstants.END_SVID_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of End Svid : "
														+ endSvidLoc + "." );
		}

		oldAuthorizationLoc = getPropertyValue(
						SOAConstants.OLD_AUTHORIZATION_FLAG_LOC_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Old Authorization : "
													+ oldAuthorizationLoc + "." );
		}

		failedSPLoc = getPropertyValue( SOAConstants.FAILED_SP_FLAG_LOC_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Failed SP : "
															+ failedSPLoc + "." );
		}

		inputTnLoc = getPropertyValue( SOAConstants.INPUT_TN_LOC_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Input TN : "
														+ inputTnLoc + "." );
		}

		outputTnLoc = getRequiredPropertyValue( SOAConstants.OUTPUT_TN_LOC_PROP ,
																errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of Output TN :"
															+ outputTnLoc + "." );
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

		outputEndStationLoc = getRequiredPropertyValue(
				SOAConstants.OUTPUT_ENDSTATION_LOC_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Location of Output End Station :"
												+ outputEndStationLoc + "." );
		}

		outputNewSPLoc = getPropertyValue( SOAConstants.OUTPUT_NEWSP_LOC_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Location of output newSP : "
													+ outputNewSPLoc + "." );
		}

		outputOldSPLoc = getPropertyValue( SOAConstants.OUTPUT_OLDSP_LOC_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Location of output oldSP : "
													+ outputOldSPLoc + "." );
		}

		causeCodeLoc = getPropertyValue( SOAConstants.OUTPUT_CAUSECODE_LOC_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of cause code : "
														+ causeCodeLoc + "." );
		}

		dueDateLoc = getPropertyValue( SOAConstants.OUTPUT_DUEDATE_LOC_PROP );
        
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of output due date : "
														+ dueDateLoc + "." );
		}

		creationTimestampLoc = getPropertyValue(
									SOAConstants.CREATION_TS_LOC_PROP );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location of creation timestamp  : "
													+ creationTimestampLoc + "." );
		}

		regionIdLoc =
					getRequiredPropertyValue(
									SOAConstants.REGION_ID_PROP, errorBuffer);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
								"Location of region ID value [" + regionIdLoc + "].");
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
									"MapSOANotification: Initialization done." );
		}

    }


    /**
     * This method will extract the data values from the context/input,
     * and perform DB lookups and apply business logic with the input values
     * to determine the exact SOA Notification.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext context,
    						  MessageObject object )
                        					throws MessageException,
                        						   ProcessingException
    {
    	ThreadMonitor.ThreadInfo tmti = null;

		if ( object == null )
		{

            return null;

		}

		this.context = context ;

		this.object = object ;

		Connection dbConn = null;

		String lastMessage = null;

		String inputXML = null;

		String soaNotification = null;

		String outputXML = null;

		XMLMessageParser parser = null;

		try
		{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	        // get input XML from context
			Document doc
				= (Document) super.getDOM( inputXMLLoc , context , object );

			XMLMessageGenerator generator = new XMLMessageGenerator( doc );

			inputXML = generator.getMessage();
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Input XML value :\n\t"
															+ inputXML + ".\n" );
			}

	        // Get SPID value from context
	        spid = getValue( spidLoc );
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, "SPID Value : [ " + spid + " ]." );
			}

			//	Get NewSP value from context
			newSP = getValue( inputNewSPLoc );
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, " New SP value :[ " + newSP + " ]." );
			}

			//	Get OldSP value from context
			oldSP = getValue( inputOldSPLoc );

            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, "Old SP value :[ " + oldSP + " ]." );
			}

			//	Get Status value from context
			status = getValue( statusLoc );
            
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log( Debug.SYSTEM_CONFIG, "Status value :[ " + status + " ]." );
			}

			//	Get Start Svid value from context
			startSvid = getValue( startSvidLoc );
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Start Svid value :[ "
																+ startSvid+ " ]." );
			}

			regionId = getValue( regionIdLoc );

            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Region ID value :[ "
																+ regionId+ " ]." );
			}
			//	Get End Svid value from context
			endSvid = getValue( endSvidLoc );

			// Get OldAuthorization value from context
			String oldAuthorizationStr = getValue( oldAuthorizationLoc  );

			oldAuthorization = -1 ;

			if( !SOAUtility.isNull( oldAuthorizationStr ) )
			{

				oldAuthorization = Integer.parseInt( oldAuthorizationStr );

			}
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, " Old authorization value :[ "
														+ oldAuthorization + " ]." );
			}

			//	Get OldAuthorization value from context
			 String failedSPStr  =  getValue( failedSPLoc );

			 if( !SOAUtility.isNull( failedSPStr ) )
			 {

				 failedSP = Integer.parseInt( failedSPStr );

			 }
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, " Failed SP value :[ "
														+ failedSP+ " ]." );
			}
			//	Get TN value from context
			tn = getValue( inputTnLoc );
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			 Debug.log( Debug.SYSTEM_CONFIG, " TN value :[ " + tn + " ]." );
			}

			//	Get Creation timestamp value from context
			creationTimestamp = getValue( creationTimestampLoc );
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,
					" Creation time stamp value :[ " + creationTimestamp + " ]." );
			}

			//	Get NPAC Notification value from context
			npacNotification = getValue( npacNotificationLoc );
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
				dbConn = context.getDBConnection( );

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

				}catch ( ResourceException e )
				{

					String errMsg
						= "ERROR: MapSOANotification: Attempt to get database"
											+ " connection failed with error: "
											+ e.getMessage( );

					Debug.log( Debug.ALL_ERRORS, errMsg );

				}

			}

			if( dbConn == null )
			{

				throw new ProcessingException( "Connection is not acquired ," +
														"it is null ");

			}

			//	Getting last message from Database by quering SV object table
			lastMessage = getDBData( dbConn );

			//	Getting SOA Notification
			soaNotification
					= getSOANotification( npacNotification ,lastMessage);
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
			outputXML
					= inputXML.replaceAll( npacNotification,soaNotification );

			// XML String after removing extra nodes
			outputXML
				= removeNodes( outputXML , npacNotification , soaNotification );

			if( soaNotification.equals(
									SOAConstants.SV_ATR_CHANGE_NOTIFICATION ) )
			{

				// creating stringbuffer to insert ModifiedData tag
				StringBuffer sb = new StringBuffer( outputXML );

				sb.insert( sb.indexOf( "<SPID/>" ) + "<SPID/>".length() ,
														"\n<ModifiedData>" );

				sb.insert( sb.indexOf( "</SvAttributeChangeNotification>" ) ,
														"</ModifiedData>\n" );

				parser = new XMLMessageParser( sb.toString() );

			}else
			{

				parser = new XMLMessageParser( outputXML.toString() );

			}

			childElement = 0 ;

			parser = populateTN( parser , soaNotification , npacNotification );
			
			
			//
		      /* SOA 5.8.3, if SOA notification type is SvReleaseInConflictAckNotification, map Cause Code from SvReleaseInConflict Request to this notification */
			if ( soaNotification.equals(SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION ) )
			{						
				String causeCode = getCauseCodefromSvRelInConflictRequest (dbConn,referenceKey);
				if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					Debug.log( Debug.DB_DATA, " CauseCode From SOA_SV_MESSAGE table :[ "
																+ causeCode+"]" );
				}
				if(causeCode!=null)
				{
					Debug.log( Debug.DB_DATA, " Inside causeCode is not null");
					parser = populateCauseCode(parser, causeCode);
												
				}	
			}
			CharSequence msg = (CharSequence)parser.getMessage();
			msg  = SOAUtility.removeWhitespace(msg);
			msg = RegexUtils.replaceAll("> <", msg.toString(), ">\n<");
			
			parser = new XMLMessageParser( msg.toString() );
			
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Output SOA Notification XML: \n\t"
													+ parser.getMessage() + ".\n" );
			}
            
		}
		catch ( ProcessingException e )
		{

			Debug.log( Debug.ALL_ERRORS, e.toString() );

			// Re-throw the exception to the driver.
			throw new ProcessingException( e.toString() );


		}catch (FrameworkException fe){
			Debug.log( Debug.ALL_ERRORS, fe.toString() );

			// Re-throw the exception to the driver.
			throw new ProcessingException( fe.toString() );
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
		super.set( outputXMLLoc , context , object , parser.getDocument() );

		if( tn == null ){

         // if the TN was not available, this is probably because no SV exists
         // in the SOA DB for the SVID given in the response/notification.
         throw new ProcessingException("No TN was available for the given SVID.");

        }

		//	Setting TN in to context
		super.set( outputTnLoc , context , object , tn );

		// Setting EndStation in to context
		super.set( outputEndStationLoc ,
                 context ,
                 object ,
                 getEndStation() );

		// following attributes	need to be populate in XML in next processor
		// setting NewSp in to context
		if( newSP != null )
		{

			super.set( outputNewSPLoc , context , object , newSP );

		}

		//	setting OldSp in to context
		if( oldSP != null )
		{

			super.set( outputOldSPLoc , context , object , oldSP );

		}

		//	setting cause code in to context
		if( causeCode != null )
		{

			super.set( causeCodeLoc , context , object , causeCode );

		}

		//	setting old sp due date in to context
		if( dueDate != null )
		{

			super.set( dueDateLoc , context , object , dueDate );

		}
		
		
		// Pass the input on to the output.
        return( formatNVPair( object ) );

    }
    
    /**
     * 
     * Populate the CauseCode node in Message XML
     * @param parser
     * @param causeCode
     * @return
     * @throws MessageException
     */
    
    private XMLMessageParser populateCauseCode(XMLMessageParser parser, String causeCode) throws MessageException
    {
    	
    	XMLMessageGenerator generator = ( XMLMessageGenerator )parser.getGenerator();
    	
    	String rootNode =  	"SOAToUpstream.SOAToUpstreamBody.SvReleaseInConflictAckNotification";
    	generator.setValue( rootNode + ".CauseCode" , causeCode );
    	
    	return ( XMLMessageParser ) generator.getParser();
    }

	/**
	 * This method is used to populate tn value in the input message if
	 * input message comes with TnSvidList node
	 *
	 * @param  parser as XMLMessageParser
	 * @param  soaNotification as String
	 * @param  npacNotification as String
	 *
	 * @return  XMLMessageParser.
	 *
	 * @exception MessageException
	 *
	 */
    private XMLMessageParser populateTN( XMLMessageParser parser ,
    									 String soaNotification ,
    									 String npacNotification )
    									 throws MessageException
    {

    	XMLMessageGenerator generator
    						= ( XMLMessageGenerator )parser.getGenerator();

    	String rootNode =  	"SOAToUpstream.SOAToUpstreamBody." +
    						soaNotification +
							".Subscription.TnSvIdList";

    	if( parser.exists( rootNode ))
    	{

    		childElement = parser.getChildCount( rootNode );
            
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG ,
						" No of child Elements [ SvId ] = " + childElement );
			}

    		if( childElement > 1 )
    		{

				if( (npacNotification.equals(
							SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION )
					|| npacNotification.equals(
							SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION ))&& tn != null)
				{

	    			StringTokenizer st = new StringTokenizer( tn ,
	    										SOAConstants.DEFAULT_DELIMITER );

	    			String npa = st.nextToken();

	    			String nxx = st.nextToken();

	    			String line = st.nextToken();

	    			int lineNumber = Integer.parseInt( line );

	    			for( int i =0 ; i< childElement ; i++ )
	    			{

	    				StringBuffer telephoneNumber = new StringBuffer( npa );

						telephoneNumber.append( SOAConstants.DEFAULT_DELIMITER );

						telephoneNumber.append( nxx );

						telephoneNumber.append( SOAConstants.DEFAULT_DELIMITER );

						telephoneNumber.append( StringUtils.padNumber(
								lineNumber++, SOAConstants.TN_LINE, true, '0' ) );

                        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
							Debug.log( Debug.SYSTEM_CONFIG ,
										" Telephone Number to be populated  = " +
										telephoneNumber.toString() );
						}

						generator.setValue( rootNode + ".TnSvId(" + i + ").Tn" ,
											telephoneNumber.toString() );


	    			}
				}

    		}else
    		{

				if( (npacNotification.equals(
							SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION )
					|| npacNotification.equals(
							SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION ))&& tn != null )
				{
                    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
						Debug.log( Debug.SYSTEM_CONFIG ,
									" Telephone Number to be populated  = " + tn );
					}

					generator.setValue( rootNode + ".TnSvId.Tn" , tn );

				}


    		}



    	}

    	 return ( XMLMessageParser ) generator.getParser();

    }

	/**
	 * This method will give SOA notification based on NPAC notification,
	 * status and last Message
	 *
	 * @param  npacNotification as String
	 * @param  lastmessage as String
	 *
	 * @return  SOA Notification, or null.
	 *
	 */
    private String getSOANotification(String npacNotification ,
    								  String lastMessage)
    {

    	String notification = null;

		// Routing based on NPAC Notification to get SOA Notification

		// if NPAC Notification is VersionObjectCreateNotification
		if( npacNotification.equals(SOAConstants
									.VERSION_OBJECT_CREATION_NOTIFICATION) )
		{

			notification = getMapVerObject( lastMessage );

	   //if NPAC Notification is 'VersionStatusAttributeValueChangeNotification'
		}else if( npacNotification.equals(SOAConstants
									.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION) )
		{

			notification = getMapVerStatus( lastMessage );

		//	if NPAC Notification is 'VersionAttributeValueChangeNotification'
		}else if( npacNotification.equals(SOAConstants
										.VERSION_ATR_VAL_CHAN_NOTIFICATION) )
		{

			notification = getMapVerAttribute( lastMessage );

		}

		return notification;

    }

	/**
	 * This method will Map VersionObjectCreationNotification into
	 * SOA notification based on last Message
	 *
	 * @param  lastmessage as String
	 *
	 * @return  String as SOA Notification, or null.
	 *
	 */
	private String getMapVerObject( String lastMessage )
	{

		String notification = null;

		//	If InitSPID equals to NewSP
		if( spid.equals(newSP) )
		{
			//	if creationtimestamp is coming with 'VersionObjectCreationNotification'
			if( creationTimestamp != null )
			{

				notification = SOAConstants.SV_CREATE_ACK_NOTIFICATION ;

			//	Old Authorization is 'true'
			}else if( oldAuthorization == 1  )
			{

				notification = SOAConstants.SV_RELEASE_NOTIFICATION ;

			//	Old Authorization is 'false'
			}else if( oldAuthorization == 0  )
			{

				notification = SOAConstants.SV_RELEASE_CONF_NOTIFICATION ;

			}

		// if InitSPId equal to OldSP
		}else if( spid.equals(oldSP) )
		{
			// if creationtimestamp is coming with 'VersionObjectCreationNotification'
			if( creationTimestamp != null )
			{

				notification = SOAConstants.SV_CREATE_NOTIFICATION ;

			// Old Authorization is 'true'
			}else if( oldAuthorization == 1  )
			{

				notification = SOAConstants.SV_RELEASE_ACK_NOTIFICATION ;

			//	Old Authorization is 'false'
			}else if( oldAuthorization == 0  )
			{

				notification = SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION ;

			}

		}

		return notification ;

	}

	/**
	 * This method will Map VersionStatusAttributeValueChangeNotification into
	 * SOA notification based on last Message , status .
	 *
	 * @param  lastmessage as String
	 *
	 * @return  String as SOA Notification .
	 *
	 */
	private String getMapVerStatus( String lastMessage )
	{

		String notification = SOAConstants.SV_STS_CHANGE_NOTIFICATION ;

		// if status is 'old'
		if( status.equals(SOAConstants.OLD_STATUS) )
		{
			if(failedSP == 1)
			{

				notification = SOAConstants.SV_STS_CHANGE_NOTIFICATION ;

			}else

			{
				//if PortingToOriginal is 'true'
				if (lastRequestType != null && (portingToOriginal == 1 && !lastRequestType.equals(SOAConstants.SV_DISCONNECT_REQUEST))) {
					notification = SOAConstants.SV_PTO_NOTIFICATION;
				} else 
				{
					notification = SOAConstants.SV_DISCONNECT_NOTIFICATION;
				}
				

			}

		//	if status is 'active'
		}else if( status.equals(SOAConstants.ACTIVE_STATUS) )
		{
			if(requestType != null && requestType.equals("Port-Out"))
			{
				if(failedSP == 1){
					notification = SOAConstants.SV_STS_CHANGE_NOTIFICATION ;
				}
				else{
					notification = SOAConstants.SV_ACTIVATE_NOTIFICATION ;
				}
			}
			else
			{
				if( failedSP == 1 || (lastMessage != null && lastMessage.equals(SOAConstants.SV_MODIFY_REQUEST))
						          || (dbStatus != null && dbStatus.equals(SOAConstants.ACTIVE_STATUS))
					              || (lastMessage != null && lastMessage.equals(SOAConstants.SV_CANCEL_REQUEST )))
				{
					notification = SOAConstants.SV_STS_CHANGE_NOTIFICATION ;
				}else
				{
					// if PortingToOriginal is 'true'
					if( portingToOriginal == 1 )
					{
						notification = SOAConstants.SV_PTO_NOTIFICATION ;
					}else
					{
						notification = SOAConstants.SV_ACTIVATE_NOTIFICATION ;
					}
				}
			}
		//	if status is 'canceled'
		}else if( status.equals(SOAConstants.CANCELED_STATUS) )
		{
			// if DB status is other than 'conflict'
			if(dbStatus != null && ! dbStatus.equals( SOAConstants.CONFLICT_STATUS ) )
			{


				notification = SOAConstants.SV_CANCEL_NOTIFICATION ;

			}

		}

		return notification ;

	}

	/**
	 * This method will Map VersionAttributeValueChangeNotification into
	 * SOA notification based on last Message
	 *
	 * @param  lastmessage as String
	 *
	 * @return  String as SOA Notification.
	 *
	 */
	private String getMapVerAttribute( String lastMessage )
	{

		String notification = SOAConstants.SV_ATR_CHANGE_NOTIFICATION;

		// if InitSPID equal to NewSP
		if( spid.equals(newSP) )
		{

			//	if LastMessage is 'SvCreateRequest'
			if( lastMessage != null && lastMessage.equals(SOAConstants.SV_CREATE_REQUEST) )
			{
				if( creationTimestamp != null )
				{
					notification = SOAConstants.SV_CREATE_ACK_NOTIFICATION ;

				}

			// if lastMessage is 'SvCreateAckNotification'
			}else if( lastMessage != null && lastMessage.equals(SOAConstants
											.SV_CREATE_ACK_NOTIFICATION) )
			{
				//if OldAuthorizationFlag is 'true'
				if( ( onspduedate == null && oldAuthorization == 1 ) )
				{

					notification = SOAConstants.SV_RELEASE_NOTIFICATION ;

				}else if( onspduedate == null &&  oldAuthorization == 0 ){
					
					notification = SOAConstants.SV_RELEASE_CONF_NOTIFICATION ;
				}

			}
			else if ((creationTimestamp != null)&&
					(lastRequestType == null || 
							(lastRequestType.equals(SOAConstants.SV_CREATE_REQUEST))))
			{
				
					notification = SOAConstants.SV_CREATE_ACK_NOTIFICATION ;
				
			}
			// if lastMessage exists
			else if(lastMessage != null)
			{
				// ONSPDUEDATE is null for existing SV
				if( dueDate == null)
				{
					if( onspduedate == null && oldAuthorization == 1 ){
						notification = SOAConstants.SV_RELEASE_NOTIFICATION ;	
					}
					else if( onspduedate == null &&  oldAuthorization == 0 ){
						notification = SOAConstants.SV_RELEASE_CONF_NOTIFICATION ;
					}
				}
				
			}

		// if IntiSPID equal to OldSp
		}else if( spid.equals(oldSP) )
		{

			//if last message is 'SvReleaseAckNotification'
			if( lastMessage != null && (lastMessage.equals(
								SOAConstants.SV_RELEASE_ACK_NOTIFICATION)
				|| lastMessage.equals(
								SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION)) )
			{
				if( creationTimestamp != null )
				{

					notification = SOAConstants.SV_CREATE_NOTIFICATION ;

				}

			//	if last message is 'SvReleaseRequest'
			}else if( lastMessage != null && lastMessage.equals(SOAConstants
												.SV_RELEASE_REQUEST ) )
			{

				// autorization flag will not come when NewSp sends modify
				// request , this will come when Oldsp sends request
				if ( oldAuthorization == 1 )
				{

					notification = SOAConstants.SV_RELEASE_ACK_NOTIFICATION ;

				}

			}else if ( lastMessage != null && (lastMessage.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST ) || 
					  lastMessage.equals(SOAConstants.SV_STS_CHANGE_NOTIFICATION )))
			{
			
                // autorization flag will not come when NewSp sends modify
				// request , this will come when Oldsp sends request
				
				if( dueDate == null && oldAuthorization == 0  )
				{
					notification
						= SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION ;

				}
		     }
			else if ((oldAuthorization == 1 || oldAuthorization == 0)
					&& (lastRequestType == null || ((lastRequestType
							.equals(SOAConstants.SV_RELEASE_REQUEST) || lastRequestType
							.equals(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST)))))
			{

				if (dbAuthorization == null && oldAuthorization == 1) {
					notification = SOAConstants.SV_RELEASE_ACK_NOTIFICATION;
				} else if (dbAuthorization == null && oldAuthorization == 0) {
					notification = SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION;
				}
			}
			// if lastMessage exists
			else if(lastMessage != null)
			{
				// nnspduedate is null for existing SV
				if( nnspduedate == null)
				{
					if( creationTimestamp != null )
					{

						notification = SOAConstants.SV_CREATE_NOTIFICATION ;

					}
				}
				
			}

		}

		return notification ;

	}

	/**
	 * This method will retrieve Last Message , PTO , NNSP , ONSP
	 *  and TN from Database
	 *
	 * @param  dbConn  The database connection to perform the SQL
   	 * SELECT operation against.
	 *
	 * @return  Last Message , or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private String getDBData( Connection dbConn )
											throws ProcessingException
	{

		String comma = " , ";

		StringBuffer queryMessage = new StringBuffer();

		queryMessage.append( "SELECT ");

		if( !npacNotification.equals(SOAConstants
										.VERSION_OBJECT_CREATION_NOTIFICATION) )
		{

			queryMessage.append( SOAConstants.PORTINGTN_COL );

			queryMessage.append( comma );

			queryMessage.append( SOAConstants.NNSP_COL );

			queryMessage.append( comma );

			queryMessage.append( SOAConstants.ONSP_COL );

			queryMessage.append( comma );

		}

		queryMessage.append( SOAConstants.LASTMESSAGE_COL );

		queryMessage.append( comma );
		
		queryMessage.append( SOAConstants.REQUESTTYPE_COL );

		queryMessage.append( comma );

		queryMessage.append( SOAConstants.STATUS_COL );
		
		queryMessage.append( comma );

		queryMessage.append( SOAConstants.LASTREQUESTTYPE_COL );

		if( npacNotification.equals(SOAConstants
									.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION) )
		{

			queryMessage.append( comma );

			queryMessage.append( SOAConstants.PORTINGTOORIGINAL_COL );

			queryMessage.append( comma );

			queryMessage.append( SOAConstants.ONSPDUEDATE_COL );

			

		}

		if( npacNotification.equals(SOAConstants
											.VERSION_ATR_VAL_CHAN_NOTIFICATION) )
		{

			queryMessage.append( comma );

			queryMessage.append( SOAConstants.CAUSECODE_COL );

			queryMessage.append(  comma );

			queryMessage.append( SOAConstants.AUTHORIZATIONFLAG_COL );
			
			queryMessage.append(  comma );

			queryMessage.append( SOAConstants.NNSPDUEDATE_COL );
			
			queryMessage.append(  comma );

			queryMessage.append( SOAConstants.ONSPDUEDATE_COL );
			
			queryMessage.append(  comma );

			queryMessage.append( SOAConstants.REFERENCEKEY_COL );

		}

		queryMessage.append( " FROM " );

		queryMessage.append( SOAConstants.SV_TABLE );

		queryMessage.append( " WHERE "+SOAConstants.SPID_COL + " = ?" );

		// if NPAC notification is 'VersionObjectCreateNotification'
		if( npacNotification.equals(SOAConstants
								.VERSION_OBJECT_CREATION_NOTIFICATION) )
		{

			queryMessage.append( " AND "+SOAConstants.PORTINGTN_COL + " = ?" );

			queryMessage.append(" AND "+ SOAConstants.STATUS_COL + " IN (");
			
			queryMessage.append("'"+SOAConstants.SENDING_STATUS+"'");
			
			queryMessage.append( comma );
			
			queryMessage.append("'"+SOAConstants.CONFLICT_STATUS +"'");
			
			queryMessage.append( comma );
			
			queryMessage.append("'"+SOAConstants.PENDING_STATUS+"'");
			
			queryMessage.append( comma );
			
			queryMessage.append("'"+SOAConstants.CANCEL_PENDING_STATUS+"'");
			
			queryMessage.append( comma );
			
			queryMessage.append("'"+SOAConstants.DOWNLOAD_FAILED_STATUS+"'");
			
			queryMessage.append( comma );
			
			queryMessage.append("'"+SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS+"'");
			
			queryMessage.append( comma );
			
			queryMessage.append("'"+SOAConstants.DISCONNECT_PENDING_STATUS+"'");
			
			queryMessage.append( comma );
			
			queryMessage.append("'"+SOAConstants.CREATING_STATUS+"'");
			
			queryMessage.append( comma );
			
			queryMessage.append("'"+SOAConstants.NPAC_CREATE_FAILURE+"'");			

			queryMessage.append(")");

			queryMessage.append( " ORDER BY CREATEDDATE DESC" );
			
			queryMessage.insert(7, "/*+ INDEX(" + SOAConstants.SV_TABLE + " SOA_SV_INDEX_2 ) */ ");
			
		}else
		{

			queryMessage.append( " AND " + SOAConstants.SVID_COL + " = ?" );

			queryMessage.append( " AND " + SOAConstants.REGION_COL + " = ?" );
			
			queryMessage.insert(7, "/*+ INDEX(" + SOAConstants.SV_TABLE + " SOA_SV_INDEX_1) */ ");
		}
        if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){
		  Debug.log( Debug.NORMAL_STATUS, "Database quary is : \n" + queryMessage );
		}

		PreparedStatement pstmt = null;

	  	ResultSet rs = null;

	  	String lastMessage = null;

	  	try
	  	{

		  	// Get a prepared statement for the SQL statement.
		  	pstmt = dbConn.prepareStatement( queryMessage.toString() );

		  	pstmt.setString( 1, spid );

		  	// if NPAC notification is 'VersionObjectCreateNotification'
		  	if( npacNotification.equals(SOAConstants
		  						.VERSION_OBJECT_CREATION_NOTIFICATION) )
		  	{

		  		pstmt.setString( 2, tn );

		  	}else
		  	{

				pstmt.setString( 2, startSvid );

				pstmt.setString( 3, regionId );

		  	}

		   	// Execute the SQL SELECT operation.
		  	rs = pstmt.executeQuery( );

		  	if ( rs.next() )
		  	{

				if( !npacNotification.equals(SOAConstants
										.VERSION_OBJECT_CREATION_NOTIFICATION) )
				{
			  		tn = rs.getString( SOAConstants.PORTINGTN_COL );
                    
					if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					 Debug.log( Debug.DB_DATA, " TN From database : " + tn );
					}

					newSP = rs.getString( SOAConstants.NNSP_COL );
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    					Debug.log( Debug.DB_DATA, " New SP From database : "
																	+ newSP );
					}

					oldSP = rs.getString( SOAConstants.ONSP_COL );
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    					Debug.log( Debug.DB_DATA, " Old SP From database : "
																	+ oldSP );
					}
				}

				if( npacNotification.equals(SOAConstants
									.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION) )
				{

					portingToOriginal
								= rs.getInt( SOAConstants.PORTINGTOORIGINAL_COL );
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    					Debug.log( Debug.DB_DATA,
										"Porting to original From database : "
														+ portingToOriginal );
					}

					java.sql.Timestamp d = rs.getTimestamp(
												SOAConstants.ONSPDUEDATE_COL );

					if( d != null )
					{

						SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

						dueDate = sdf.format( d );

					}
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    					Debug.log( Debug.DB_DATA,
											"Old SP due date From database : "
																+ dueDate );
					}

					

				}

				if( npacNotification.equals( SOAConstants
										.VERSION_ATR_VAL_CHAN_NOTIFICATION ) )
				{

					causeCode = rs.getString( SOAConstants.CAUSECODE_COL );
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    					Debug.log( Debug.DB_DATA, "Cause Code From database : "
															+ causeCode );
					}

					dbAuthorization
						= rs.getString( SOAConstants.AUTHORIZATIONFLAG_COL );
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
						Debug.log( Debug.DB_DATA,
											"Authorization flag From database : "
											+ dbAuthorization );
					}
					nnspduedate
					     = rs.getString( SOAConstants.NNSPDUEDATE_COL );
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    					Debug.log( Debug.DB_DATA,
										"nnspduedate From database : "
										+ nnspduedate );
					}
					onspduedate
				     = rs.getString( SOAConstants.ONSPDUEDATE_COL );
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    				    Debug.log( Debug.DB_DATA,
									"onspduedate From database : "
									+ onspduedate );
					}
                    
                    referenceKey 
                     = rs.getLong(SOAConstants.REFERENCEKEY_COL);
                    if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
    				    Debug.log( Debug.DB_DATA,
									"referenceKey From database : "
									+ referenceKey );
					}	
				}

		  		lastMessage = rs.getString( SOAConstants.LASTMESSAGE_COL );
                if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					Debug.log( Debug.DB_DATA, "Last Message From database : "
																+ lastMessage );
				}
				
				requestType = rs.getString( SOAConstants.REQUESTTYPE_COL );
				if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					Debug.log( Debug.DB_DATA, "Request Type From database : "
							+ requestType );
				}
				
				dbStatus = rs.getString( SOAConstants.STATUS_COL );
                if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					Debug.log( Debug.DB_DATA, "Status From database : "
																+ dbStatus );
				}
                
                lastRequestType	= rs.getString( SOAConstants.LASTREQUESTTYPE_COL );
		        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					Debug.log( Debug.DB_DATA,
						"Last Request type From database : "
														+ lastRequestType );
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

		return lastMessage;
	}
	
	
	/**
	 * Method to fetch CauseCode from SOA_SV_MESSAGE table SvReleaseInConflict Request Message.
	 */
	private String getCauseCodefromSvRelInConflictRequest(Connection con, Long refKey)
	{
		String causeCODE = null;
		PreparedStatement psmt = null;
		ResultSet rset = null;
		try
		{
			psmt = con.prepareStatement(SELECT_CAUSECODE_SQL);
			psmt.setLong(1, refKey);						
			rset = psmt.executeQuery();		
			while (rset.next())	
			{
				causeCODE = rset.getString(SOAConstants.CAUSECODE_COL);
			}
				
		}catch (Exception e)
		{
			if( Debug.isLevelEnabled(Debug.ALL_ERRORS))
				Debug.log(Debug.ALL_ERRORS, "Error Ocurred while fetching Cause Code from SOA_SV_MESSAGE table for SvReleaseinConfictAck Notification" + e.getMessage());
		}
		finally{
			try {				
				if (psmt != null) {					
					psmt.close();					
					psmt = null;
				}
				if (rset != null) {					
					rset.close();					
					rset = null;
				}
			}catch (SQLException sqle) {
				
				if( Debug.isLevelEnabled( Debug.ALL_ERRORS ) )
					Debug.log(Debug.ALL_ERRORS, "Error Occured while closing the PreparedStatement & ResultSet. "); 
			}
			
		}			
		return causeCODE;		
	}

	/**
	 * This method process svids and  gives EndStation
	 *
	 * @return  EndStation .
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */

	private String getEndStation()throws ProcessingException
	{

		String startTN = tn.substring(tn.lastIndexOf(
								SOAConstants.DEFAULT_DELIMITER )+ 1 );

		int endStation = -1 ;

		try
		{

			long startSvidVal = Long.parseLong(startSvid);

			int diff = 0;

			if( endSvid != null )
			{
				diff = (int)(Long.parseLong(endSvid) - startSvidVal) ;

			}else if( childElement > 1 )
			{

				diff = childElement - 1 ;

			}

			endStation = Integer.parseInt(startTN) + diff ;


		}catch( NumberFormatException nbrfex ){

			throw new ProcessingException("Invalid StartSvid and/or EndSvid: "
																	 + nbrfex);

		}

      // this makes sure that the end station is always 4 digits and
      // pads the String with zeros if the end station has less than three
      // digits (i.e. is less than 1000).
      String endStationValue = StringUtils.padNumber(endStation, 4, true, '0');
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, " Value of EndStation : " +
														endStationValue+ "." );
		}

     return endStationValue;


	}

	/**
	 * This method used to remove some extra nodes from output XML.
	 *
	 * @param  xml as a string
	 * @param  npacNotification as a string
	 * @param  soaNotification  as a string
	 *
	 * @return  XMLMessageParser
	 *
	 * @exception  MessageException  Thrown if node name not found.
	 *
	 */
	private String removeNodes( String xml ,
								String npacNotification,
								String soaNotification )
								throws MessageException
	{

		// create XML parser for xml string
		XMLMessageParser parser = new XMLMessageParser( xml );

		String rootNode = "SOAToUpstream.SOAToUpstreamBody." + soaNotification ;

		// if notification is 'VersionStatusAttributeValueChangeNotification'
		if( npacNotification.equals(
						SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION ) )
		{

			removeVerStatusNodes( parser , rootNode , soaNotification  );


		}else if( npacNotification.equals(
							SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION ) )
		{

			removeVerAttributeNodes( parser , rootNode , soaNotification  );

		}

		return parser.getMessage();

	}


	/**
	 * This method used to remove some extra nodes from output XML
	 * If NPAC notification is VersionStatusAttributeValueChangeNotification.
	 *
	 * @param  parser as a XMLMessageParser
	 * @param  rootNode as a string
	 * @param  soaNotification  as a string
	 *
	 * @exception  MessageException  Thrown if node name not found.
	 *
	 */
	private void removeVerStatusNodes( 	XMLMessageParser parser,
							  			String rootNode ,
							  			String soaNotification  )
							  			throws MessageException
	{

		//	if notification is other than
		// 'SvReleaseInConflictAckNotification'
		if( ! ( soaNotification.equals(
						SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION )
			||  soaNotification.equals(
						SOAConstants.SV_RELEASE_CONF_NOTIFICATION ) ) )
		{
			if( parser.exists( rootNode + ".DueDate" ) )
			{

				// remove DueDate node
				parser.removeNode( rootNode + ".DueDate" );

			}

		}

		//	if notification is 'SvStatusChangeNotification'
		if( soaNotification.equals(
								SOAConstants.SV_STS_CHANGE_NOTIFICATION ) )
		{
			if( parser.exists( rootNode + ".CauseCode " ) )
			{

				// remove DueDate node
				parser.removeNode( rootNode + ".CauseCode " );

			}

		}else
		{
			if( parser.exists( rootNode + ".SvStatus" ) )
			{

				// remove DueDate node
				parser.removeNode( rootNode + ".SvStatus" );

			}



		}

		// if notification is 'SvDisconnectNotification' or
		// 'SvPortToOriginalNotification'
		if( soaNotification.equals( SOAConstants.SV_DISCONNECT_NOTIFICATION )
			|| soaNotification.equals( SOAConstants.SV_PTO_NOTIFICATION ) )
		{
			if( parser.exists( rootNode + ".NewSP" ) )
			{

				// remove NewSP node
				parser.removeNode( rootNode + ".NewSP" );

			}

			if( parser.exists( rootNode + ".OldSP" ) )
			{

				// remove OldSP node
				parser.removeNode( rootNode + ".OldSP" );

			}


		}else
		{

			if( parser.exists( rootNode + ".SPID" ) )
			{
				// remove SPID node
				parser.removeNode( rootNode + ".SPID" );

			}

		}

	}


	/**
	 * This method used to remove some extra nodes from output XML
	 * If NPAC notification is VersionAttributeValueChangeNotification.
	 *
	 * @param  parser as a XMLMessageParser
	 * @param  rootNode as a string
	 * @param  soaNotification  as a string
	 *
	 * @exception  MessageException  Thrown if node name not found.
	 *
	 */
	private void removeVerAttributeNodes( 	XMLMessageParser parser,
							  				String rootNode ,
							  				String soaNotification  )
							  				throws MessageException
	{

		// if notification is other than 'SvReleaseInConflictNotification'
		if( !soaNotification.equals(
							SOAConstants.SV_RELEASE_CONF_NOTIFICATION ) )
		{
			if( parser.exists( rootNode + ".CauseCode" ) )
			{

				// remove CauseCode node
				parser.removeNode( rootNode + ".CauseCode" );

			}

		}

		//	if notification is other than 'SvAttributeChangeNotification'
		if( soaNotification.equals(
								SOAConstants.SV_ATR_CHANGE_NOTIFICATION ) )
		{

			if( parser.exists( rootNode + ".NewSP" ) )
			{

				// remove NewSP node
				parser.removeNode( rootNode + ".NewSP" );

			}

			if( parser.exists( rootNode + ".OldSP" ) )
			{

				// remove OldSP node
				parser.removeNode( rootNode + ".OldSP" );

			}

			if( parser.exists( rootNode + ".DueDate" ) )
			{

				// remove DueDate node
				parser.removeNode( rootNode + ".DueDate" );

			}

			if( parser.exists( rootNode + ".AuthorizationCreateTimestamp" ) )
			{

				// remove AuthorizationCreateTimestamp node
				parser.removeNode(
							rootNode + ".AuthorizationCreateTimestamp" );

			}

		}else
		{

			if( parser.exists( rootNode + ".SPID" ) )
			{
				// remove SPID node
				parser.removeNode( rootNode + ".SPID" );

			}

			if( parser.exists( rootNode + ".NewSPDueDate" ) )
			{
				// remove NewSPDueDate node
				parser.removeNode( rootNode + ".NewSPDueDate" );

			}

			if( parser.exists( rootNode + ".NewSPCreateTimestamp" ) )
			{

				// remove NewSPCreateTimestamp node
				parser.removeNode( rootNode + ".NewSPCreateTimestamp" );

			}

			if( parser.exists( rootNode + ".OldSPDueDate" ) )
			{

				// remove OldSPDueDate node
				parser.removeNode( rootNode + ".OldSPDueDate" );

			}

			if( parser.exists( rootNode + ".OldSPAuthorizationTimestamp" ) )
			{
				// remove OldSPAuthorizationTimestamp node
				parser.removeNode(
							rootNode + ".OldSPAuthorizationTimestamp" );

			}

			if( parser.exists( rootNode + ".OldSPAuthorization" ) )
			{

				// remove OldSPAuthorization node
				parser.removeNode( rootNode + ".OldSPAuthorization" );

			}

		}

	}

	/**
	 * This method tokenizes the input string and return an
	 * object for exsisting value in context or messageobject.
	 *
	 * @param  locations as a string
	 *
	 * @return  String
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.

	 */
	private String getValue ( String locations )
								throws MessageException, ProcessingException
	{
		StringTokenizer st = new StringTokenizer( locations,
								MessageProcessorBase.SEPARATOR );

		String tok = null;

		while ( st.hasMoreTokens() )
		{
			tok = st.nextToken( );
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log( Debug.MSG_STATUS, "Checking location ["
									  + tok + "] for value..." );
			}

			if ( exists( tok, context, object ) )
			{

				return( ( String ) get( tok, context, object ) );

			}
		}

		return null;
	}


//	--------------------------For Testing---------------------------------//

	public static void main(String[] args) {

		Properties props = new Properties();

		props.put( "DEBUG_LOG_LEVELS", "all" );

		props.put( "LOG_FILE", "d:\\log.txt" );

		Debug.showLevels( );

		Debug.configureFromProperties( props );

		if (args.length != 3)
		{
			Debug.log (Debug.ALL_ERRORS, "MapSOANotification: USAGE:  "+
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


		MapSOANotification mapSOANotification = new MapSOANotification();

		try
		{
			mapSOANotification.initialize("FULL_NEUSTAR_SOA","MapSoaNotification");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("SPID","2222");

			mob.set("NewSP","2222");

			mob.set("OldSP","A222");

			mob.set("Status","active");

			//mob.set("InputTN","555-123-4444");

			mob.set("StartSVID","129475");

			//mob.set("EndSVID","2009");

			//mob.set("OldAuthorization","true");

			mob.set("format","MM-dd-yyyy-hhmmssa");

			mob.set("NpacNotification","VersionStatusAttributeValueChangeNotification");

			mob.set("InputXML","<SOAMessage>"+
									"<SOAToUpstream> <SOAToUpstreamHeader>"+
									"<DateSent value=\"09-17-2004-121617PM\" />" +
									"<RegionId value=\"0000\" />"+
									 "</SOAToUpstreamHeader>"+
									"<SOAToUpstreamBody>"+
									"<VersionStatusAttributeValueChangeNotification>"+
									"<Subscription>"+
									"<TnSvIdRange><SvIdRange>"+
									"<TnSvId>"+
									"<Tn />"+
									"<SvId value=\"129475\" />"+
									 "</TnSvId>"+
//									"<TnSvId>"+
//									"<Tn />"+
//									"<SvId value=\"129476\" />"+
//									"</TnSvId>"+
//									"<TnSvId>"+
//									"<Tn />"+
//									"<SvId value=\"129477\" />"+
//									 "</TnSvId>"+
									"</SvIdRange></TnSvIdRange>"+
									"</Subscription>"+
									"<NewSP value=\"2222\" />"+
									"<OldSP value=\"A222\" />"+
									"</VersionStatusAttributeValueChangeNotification>"+
									"</SOAToUpstreamBody>"+
									"</SOAToUpstream>"+
									"</SOAMessage>");

			mapSOANotification.process(mpx,mob);

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