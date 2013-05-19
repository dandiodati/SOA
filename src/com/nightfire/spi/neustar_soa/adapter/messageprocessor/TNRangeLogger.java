/**
 * The purpose of this processor is to insert or update  
 * record(s) into SOA_SUBSCRIPTION_VERSION table and generate the XML output contains 
 * ReferenceKey values.
 *
 * @author Abhijit Talukdar
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
 * @see com.nightfire.framework.db.SQLBuilder;
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.framework.db.PersistentSequence;
 * @see com.nightfire.spi.common.driver.Converter;
 * @see com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
 * @see	com.nightfire.spi.neustar_soa.utils.SOAConstants;
 * @see com.nightfire.framework.message.parser.xml.XMLMessageParser;
 * @see com.nightfire.spi.neustar_soa.utils.SOAUtility;
 */

/**

	Revision History
	---------------
	
	Rev#	Modified By			Date			Reason
	----- ----------- ---------- --------------------------
	1		Abhijit			05/13/2004			Created
	2		Abhijit			05/17/2004			Review comments incorporated.
	3		Abhijit			05/21/2004			Modified, IS_REQUEST property
												deleted from configuration
												property.
	4		Abhijit			05/24/2004			Added condition for status
												column in where clause of the
												select query.
	5		Abhijit			06/07/2004			Added TN_DELIMITER property.
	
	6		Jaganmohan		06/17/2004			Modifyed code to incorporate
												Database changes and Matt
												comments.
	7		Jaganmohan		05/18/2004			Review comments incorporated.
	
	8		Abhijit			07/29/2004			Formal review comments incorporated.

	9		Abhijit			09/21/2004			Modify to support non-contiguoes
												SV Id range.

	10		Shan			05/03/2007			Comments incorporated
	
	11		Peeyush M		05/22/2007			Subdomain Requirement Changes
*/

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.DBLOBUtils;
import com.nightfire.framework.db.SQLBuilder;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.PersistentSequence;
import com.nightfire.spi.common.driver.Converter;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.spi.neustar_soa.rules.SOACustomDBFunctions.CreateTnAltSpidData;
import com.nightfire.spi.neustar_soa.rules.SOACustomDBFunctions.CreateTnSubdomainData;

public class TNRangeLogger extends DBMessageProcessorBase {	

	/**
	 * Name of the oracle table requested
     */
	private String tableName = null;

    private String separator = null;

	/**
	 * To indicate whether TRANSACTION_LOGGING true or false.
	 */
    private boolean usingContextConnection = true;

	/**
	 * To indicate whether Customer Id to include or not.
	 */
    private boolean useCustomerId = true;

    /**
	 * To indicate whether Customer Id to include or not.
	 */
    private boolean useSubDomain = false;

	/**
	 * The value of start SVID
	 */
	private String startSVIDValue = null;

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
	 * The value of ONSP
	 */
	private String onspValue = null;

	/**
	 * The value of NNSP
	 */
	private String nnspValue = null;

	/**
	 * The value of SVID
	 */
	private String svidValue = null;

	/**
	 * The value of REGIONID
	 */
	private String regionIdValue = null;

	/**
	 * The value of RANGEID
	 */
	private String rangeid = "";

	/**
	 * The value of SUBDOAMAIN
	 */
	private String currentSubDomain = "";
		
	/**
	 * This variable used to get value for tnDelimiter.
	 */
	private String tnDelimiter = null;

	private List columns = null;

	/**
     * Name of the oracle sequence requested
     */
	private String seqName =  null;

	/**
     * Context location of REFERENCE key XML
     */
	private String keyXMLLocation =  null;

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
	private String inputMessage =  null;

	/**
     * Context location of notification
     */
	private String notification =  null;
	
	
	private HashSet<String> oidSet = new HashSet<String>();
	
	private HashSet<String> transOidSet = new HashSet<String>();
	
	private  List <TnOIDTransOID> oIDTransOidTnlst = new ArrayList<TnOIDTransOID>();

	/**
     *  listDataFlag
     */
	private boolean listDataFlag =  false;

	/**
	 * ArrayList which contains all TN list
	 */
		private ArrayList tn_Subdomain = new ArrayList();
	
	/**
	 * ArrayList which contains Subdomain value of TNs
	 */		
		private ArrayList subdomain_Tn = new ArrayList();
	/**
	 * Delete a row from SOA_SUBSCRIPTION_VERSION table for given referencekey value.
	 */
		public static final String deletestmt = "DELETE FROM SOA_SUBSCRIPTION_VERSION WHERE REFERENCEKEY = ?";
	
	/**
	 * Update the referencekey column of SOA_SUBSCRIPTION_VERSION table for given referencekey value.
	 */
		public static final String updatestmt = "UPDATE /* + index ( SOA_MESSAGE_MAP SOA_MSGMAP_INDEX_2 ) */ SOA_MESSAGE_MAP SET REFERENCEKEY = ? WHERE REFERENCEKEY = ?";
		
	private String LISP = "lisp"; 
	
	/**
     * Constructor.
     */
    public TNRangeLogger ( ) {

		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
           Debug.log( Debug.OBJECT_LIFECYCLE,
							"Creating TN range-logger message-processor." );
		}

        columns = new ArrayList( );
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
					throws ProcessingException {

        // Call base class method to load the properties.
        super.initialize( key, type );

        // Get configuration properties specific to this processor.

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "TNRangeLogger: Initializing..." );
		}

        StringBuffer errorBuffer = new StringBuffer( );

        tableName = getRequiredPropertyValue( 
        						SOAConstants.TABLE_NAME_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Database table to log to is ["
											+ tableName + "]." );
		}

        String strTemp = getPropertyValue( 
        							SOAConstants.TRANSACTIONAL_LOGGING_PROP );

		// If TRANSACTIONAL_LOGGING is configured get the same
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

        separator = getPropertyValue( SOAConstants.LOCATION_SEPARATOR_PROP );

		// If the LOCATION_SEPARATOR is not configured use the default separator
        if ( !StringUtils.hasValue( separator ) ) {

            separator = SOAConstants.DEFAULT_LOCATION_SEPARATOR;

		}

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Location separator token ["
							+ separator + "]." );
		}

		seqName = getRequiredPropertyValue( 
						SOAConstants.REFERENCEKEY_SEQ_NAME_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
				"Sequence name to to generate REFERENCE key is [" + 
				seqName + "]." );
		}

		startSVIDValue = getPropertyValue( SOAConstants.START_SVID_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of Start SVID is ["
										+ startSVIDValue + "]." );
		}

		npaValue = getRequiredPropertyValue( 
									SOAConstants.NPA_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of NPA is ["
										+ npaValue + "]." );
		}

		nxxValue = getRequiredPropertyValue( 
									SOAConstants.NXX_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of NXX is ["
										+ nxxValue + "]." );
		}

		startTNValue = getRequiredPropertyValue( 
								SOAConstants.START_TN_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of start TN is ["
										+ startTNValue + "]." );
		}

		endTNValue = getRequiredPropertyValue( 
								SOAConstants.END_TN_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of end TN is ["
										+ endTNValue + "]." );
		}

		spidValue = getRequiredPropertyValue( 
								SOAConstants.SPID_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of SPID is ["
										+ spidValue + "]." );
		}
		
		onspValue = getPropertyValue( SOAConstants.ONSP_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of ONSP is ["
										+ onspValue + "]." );
		}

		nnspValue = getPropertyValue( SOAConstants.NNSP_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of NNSP is ["
										+ nnspValue + "]." );
		}
		svidValue = getPropertyValue( SOAConstants.SVID_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of SVID is ["
										+ svidValue + "]." );
		}

		regionIdValue = getPropertyValue( SOAConstants.REGION_ID_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of REGIONID is ["
										+ regionIdValue + "]." );
		}
		
		keyXMLLocation = getRequiredPropertyValue( 
						SOAConstants.REFERENCEKEY_OUT_LOC_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of end TN is ["
										+ keyXMLLocation + "]." );
		}

		tnDelimiter = getPropertyValue(SOAConstants.TN_DELIMITER_PROP);

		// If tnDelimiter is not specified use the Default one
		if (!StringUtils.hasValue(tnDelimiter)) {

			tnDelimiter = SOAConstants.DEFAULT_DELIMITER;

		}		

		inputMessage = getRequiredPropertyValue( 
					SOAConstants.INPUT_XML_MESSAGE_LOC_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of inputMessage is ["
											+ inputMessage + "]." );
		}
		
		notification = getPropertyValue( 
							SOAConstants.NOTIFICATION_PROP );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Configured value of notification is ["
											+ notification + "]." );
		}

		String colName = null ;
		
		String colType = null;
		
		String dateFormat = null;
		
		String defaultValue = null;
		
		String optional = null;
		
		String update = null;
		
		String location = null;
		
		ColumnData cd = null;

        // Loop until all column configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            colName = getPropertyValue( PersistentProperty
							.getPropNameIteration( 
							SOAConstants.COLUMN_PREFIX_PROP, Ix ) );

            // If we can't find a column name, we're done.
            if ( !StringUtils.hasValue( colName ) ) {

                break;

			}

            colType = getPropertyValue( PersistentProperty
						.getPropNameIteration( 
						SOAConstants.COLUMN_TYPE_PREFIX_PROP, Ix ) );

            dateFormat = getPropertyValue( PersistentProperty
					.getPropNameIteration( 
					SOAConstants.DATE_FORMAT_PREFIX_PROP, Ix ) );

            location = getPropertyValue( PersistentProperty
					.getPropNameIteration( 
					SOAConstants.LOCATION_PREFIX_PROP, Ix ) );

            defaultValue = getPropertyValue( PersistentProperty
					.getPropNameIteration( 
					SOAConstants.DEFAULT_PREFIX_PROP, Ix ) );

            optional = getRequiredPropertyValue( PersistentProperty
					.getPropNameIteration( 
					SOAConstants.OPTIONAL_PREFIX_PROP, Ix ),
					errorBuffer );

			update = getRequiredPropertyValue( PersistentProperty
				.getPropNameIteration(SOAConstants.UPDATE_PREFIX_PROP, Ix ),
				errorBuffer );

            try
            {
                // Create a new column data object and add it to the list.
                cd = new ColumnData( colName, colType, dateFormat,
									location, defaultValue, optional, update );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ) {

                    Debug.log( Debug.SYSTEM_CONFIG, cd.describe( ) );
    			}
                columns.add( cd );
            }
            catch ( FrameworkException e )
            {
                throw new ProcessingException(
						"ERROR: Could not create column data description:\n"
						+ e.toString( ) );
            }
        }

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Maximum number of columns to insert ["
					+ columns.size() + "]." );
		}

        strTemp = getPropertyValue( SOAConstants.USE_CUSTOMER_ID_PROP );

		// If USE_CUSTOMER_ID is configured
        if ( StringUtils.hasValue( strTemp ) )
        {
            try {
                
                useCustomerId = getBoolean( strTemp );
                
            }
            catch ( FrameworkException e )
            {
                errorBuffer.append ( "Property value for "
					+ SOAConstants.USE_CUSTOMER_ID_PROP
					+ " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "To use customer id in SQL statement?["
					+ useCustomerId + "]." );
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
					"TNRangeLogger: Initialization done." );
		}
    }  //end of initialize method


	/**
	 * Extract data values from the context/input, and use them to
	 * insert/update record(s) into the configured database table
	 * and geenerate the XML containing ReferenceKey
	 *	<ReferenceKey>
	 *		<keycontainer>
	 *			<key value="9480" />
	 *		</keycontainer>
	 *	</ReferenceKey>
	 *
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	public NVPair[] process(MessageProcessorContext mpContext,
                          MessageObject inputObject)
                          throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;
		// If MessageObject is null then return from here
		if ( inputObject == null ) 
		{
			return null;
		}
		
		this.mpContext = mpContext;

		this.inputObject = inputObject;

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log( Debug.MSG_STATUS, "TNRangeLogger: processing ... " );
		}

		Connection dbConn = null;
		try
		{
		tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
		
		if ( startSVIDValue != null )
		{
			// Get start SVID value from context
			startSVIDValue = getValue( startSVIDValue );
		}

		// Get the NPA value from context
		npaValue = getValue( npaValue );

		if( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of NPA is ["
										+ npaValue + "]." );
		}

		// Get the NXX value from context
		nxxValue = getValue( nxxValue );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		Debug.log( Debug.SYSTEM_CONFIG, "Value of NXX is ["
										+ nxxValue + "]." );
		}

		// Get startTN value from context
		startTNValue = getValue( startTNValue );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of Start TN is ["
										+ startTNValue + "]." );
		}

		// get endStation value from context
		endTNValue = getValue( endTNValue );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of end TN is ["
										+ endTNValue + "]." );
		}

		// Get SPID value from context
		spidValue = getValue( spidValue );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of SPID is ["
										+ spidValue + "]." );
		}

		// Get OldSP value from context
		onspValue = getValue( onspValue );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of ONSP is ["
										+ onspValue + "]." );
		}

		// Get NewSP value from context 
		nnspValue = getValue( nnspValue );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of NNSP is ["
										+ nnspValue + "]." );
		}

		// Get RegionId value from context
		regionIdValue = getValue( regionIdValue );

		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Value of REGIONID is ["
										+ regionIdValue + "]." );
		}

		
		// if NOTIFICATION_TYPE is configured
		if ( notification != null )
		{
			
			// Get the notification value from context
			notification = getValue( notification );
			
		}
		
	    // set the useSubDomain flag true if Object Create Notification is for ONSP
        if( notification.equals(SOAConstants.SV_CREATE_NOTIFICATION)
        		|| notification.equals(SOAConstants.SV_RELEASE_ACK_NOTIFICATION)
        		|| notification.equals(SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION))
        {
        	useSubDomain = true;
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "To use useSubDomain in SQL statement?["
				+ useSubDomain + "]." );
		}
		
        int startTN = 0;

		int endTN = 0;

		long startSVID = -1;
		
		// Get the startTN, endTN and startSVID
		try
		{

			startTN = Integer.parseInt(startTNValue);

			endTN = Integer.parseInt(endTNValue);
			
			//Added for identifying whether its a true range or not
			if (startTN < endTN && (notification.equals(SOAConstants.SV_RELEASE_NOTIFICATION) || 
					notification.equals(SOAConstants.SV_CREATE_NOTIFICATION) || 
					notification.equals(SOAConstants.SV_RELEASE_CONF_NOTIFICATION)|| 
					notification.equals(SOAConstants.SV_RELEASE_ACK_NOTIFICATION)
					|| notification.equals(SOAConstants.SV_CREATE_ACK_NOTIFICATION) || 
					notification.equals(SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION)))
			{
				// Get the rangekey value from object table
				rangeid = Integer.toString(PersistentSequence.getNextSequenceValue( SOAConstants.NEUSTAR_FULL_SOA_RANGEID_KEY, false,dbConn ));	
			}

			if ( startSVIDValue != null )
			{

				startSVID = Long.parseLong( startSVIDValue );

			}

		}
		catch( NumberFormatException nbrfex ){

			throw new MessageException("Invalid start TN & end TN: " + nbrfex);

		}
		catch( DatabaseException sqlexp ){
			throw new MessageException("Problem in connecting to database: " + sqlexp);
		}

		// if Object Create Notificatin for ONSP then get the subdomain value for TNs.
	    if( useSubDomain )
	    {
	        try
	        {
	        	getSubdomainValue(dbConn);
	        }
	        catch(Exception e)
	        {
	        	if( Debug.isLevelEnabled( Debug.MSG_ERROR ) ){
					Debug.log(Debug.MSG_ERROR ,"Error occred in getSubdomainValue():"+e);
				}
	        }
	    }
        
		StringBuffer queryTNSVID = new StringBuffer();

		queryTNSVID.append( "SELECT /*+ index( ");
		
		queryTNSVID.append( tableName );
		
		queryTNSVID.append(" SOA_SV_INDEX_2 ) */ ");

		queryTNSVID.append( SOAConstants.REFERENCEKEY_COL );

		queryTNSVID.append( ", " );

		queryTNSVID.append( SOAConstants.STATUS_COL );
		
		queryTNSVID.append( ", " );
		
		queryTNSVID.append( SOAConstants.PORTINGTN_COL );
		
		queryTNSVID.append( ", " );
		
		queryTNSVID.append( SOAConstants.OID_COL );
		
		queryTNSVID.append( ", " );
		
		queryTNSVID.append( SOAConstants.TransOID_COL );

		queryTNSVID.append( " FROM " );

		queryTNSVID.append( tableName );

		queryTNSVID.append( " WHERE PORTINGTN = ?" );		

		queryTNSVID.append( " AND SPID = ? AND " );

		queryTNSVID.append( " ONSP = ? AND " );

		queryTNSVID.append( " NNSP = ? AND " );

		queryTNSVID.append( " SVID IS NULL " );

		queryTNSVID.append( " ORDER BY CREATEDDATE DESC" );

		
		StringBuffer querySVID = new StringBuffer();

		querySVID.append( "SELECT /*+ index(  ");
		
		querySVID.append( tableName );
		
		querySVID.append(" SOA_SV_INDEX_1 ) */ ");

		querySVID.append( SOAConstants.REFERENCEKEY_COL );

		querySVID.append( ", " );

		querySVID.append( SOAConstants.STATUS_COL );
		
		querySVID.append( ", " );
		
		querySVID.append( SOAConstants.PORTINGTN_COL );
		
		querySVID.append( ", " );
		
		querySVID.append( SOAConstants.OID_COL );
		
		querySVID.append( ", " );
		
		querySVID.append( SOAConstants.TransOID_COL );

		querySVID.append( " FROM " );

		querySVID.append( tableName );

		querySVID.append( " WHERE SVID = ?" );		

		querySVID.append( " AND SPID = ? AND " );

		querySVID.append( " REGIONID = ?" );

		querySVID.append( " ORDER BY CREATEDDATE DESC" );
		
		
		if( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
			Debug.log(Debug.MSG_DATA, " QueryTNSVId :"+ queryTNSVID.toString());
			Debug.log(Debug.MSG_DATA, " QuerySVId :"+ querySVID.toString());
		}

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

				// Get DB connection from context
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
				// Throw the exception to the driver.
				throw new ProcessingException( "DB connection is not available" );
			}

		}
		catch ( FrameworkException e )
		{
			String errMsg = "ERROR: TNRangeLogger: Attempt to get database "
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
		
		long referenceKey = -1;

		String statusValue = null;
		
		String Tn =null;
		String oid = null;
		String transOid = null;

		// generate a REFERENCE Key XML
		XMLMessageGenerator referenceKeyXML = new XMLMessageGenerator(
														SOAConstants.KEY_ROOT );		
		PreparedStatement svidStatement = null;

		PreparedStatement svidTnStatement = null;

		PreparedStatement pstmtUpdate = null;

		PreparedStatement pstmtInsert = null;

		PreparedStatement deleteStatement = null;

		PreparedStatement updateMapStatement = null;

		boolean insertFlag = true;

		boolean updateRefFlag = false;

		long updateRefKey = -1;		

		try
		{
			
			String rootNode =  null;

			// get input XML from context
			Document doc
				= (Document) super.getDOM( inputMessage , mpContext , inputObject );

			XMLMessageParser parser = new XMLMessageParser( doc );

			// If it is a notification and the notifcation contains TnSvIdList
			// set the listDataFlag as true
			if (notification != null)
			{
				rootNode = "SOAToUpstream.SOAToUpstreamBody."
									+ notification 
									+ ".Subscription.TnSvIdList";

				// If rootNode exists in the parsed input xml
				if (parser.exists( rootNode ))
				{
					int tnSvIdCount = parser.getChildCount( rootNode + ".TnSvId" );

					if (tnSvIdCount > 1)
					{
						listDataFlag = true;
					}
					else
					{
						listDataFlag = false;
					}
				}
			}			

			// Create statement object 
			svidStatement = dbConn.prepareStatement( querySVID.toString( ) );

			svidTnStatement = dbConn.prepareStatement( queryTNSVID.toString( ) );


			//set the dummy REFERENCE key in context
			super.set(SOAConstants.REFERENCEKEY_LOCATION, mpContext, 
							inputObject, "-1" );

			// set the dummy TN in context
			super.set( SOAConstants.TN_LOCATION, mpContext, 
							inputObject, "-1");

			// set the dummy SVID in context
			super.set(SOAConstants.SVID_LOCATION, mpContext, inputObject,
								"-1" );
			
			// Reset the values in cases where we're logging again.
			resetColumnValues( );

			// Extract the column values from the arguments.
			extractMessageData( mpContext, inputObject );

			// Create the SQL statement using the column data objects.
            String sqlStmtUpdate = constructUpdateSqlStatement();

            if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA, "Executing Update SQL:\n" + sqlStmtUpdate );
			}

            // Get a prepared statement for the SQL statement.
            pstmtUpdate = dbConn.prepareStatement( sqlStmtUpdate );

			// Create the SQL statement using the column data objects.
            String sqlStmtInsert = constructInsertSqlStatement( );

            if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA, "Executing Insert SQL:\n" + sqlStmtInsert );
			}

            // Create Insert Statement object
            pstmtInsert = dbConn.prepareStatement( sqlStmtInsert );

			if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA, "Executing delete Statement SQL:\n" + deletestmt );
			}

			deleteStatement = dbConn.prepareStatement(deletestmt);

			if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
				Debug.log( Debug.DB_DATA, "Executing updatestmt SQL:\n" + updatestmt );
			}

			updateMapStatement = dbConn.prepareStatement(updatestmt);

			ResultSet rs = null;

			// Process all the TN that the notification contains
			for (int i = startTN, j = 0; i <= endTN; i++, j++ ) {
				
				insertFlag = true;
				
				StringBuffer tn = new StringBuffer( npaValue );
				
				TnOIDTransOID tnoid ;
				
				tn.append( tnDelimiter );
				
				tn.append( nxxValue );
				
				tn.append( tnDelimiter );
				
				tn.append( StringUtils.padNumber( i, SOAConstants.TN_LINE,
															 true, '0' ));

				if (Debug.isLevelEnabled(Debug.MSG_STATUS)){

					Debug.log(Debug.MSG_STATUS, "Querying TN: [" 
								+ tn.toString() + "]");

				}

				// set the TN in context
				super.set( SOAConstants.TN_LOCATION, mpContext, 
												inputObject, tn.toString());

				
				// If the notification doesn't contain listData and contains SVID
				if ( startSVID != -1 
						&& !listDataFlag )
				{
					//set the SVID in context
					super.set(SOAConstants.SVID_LOCATION, mpContext, inputObject,
								String.valueOf( startSVID ));

					startSVID++;
				}
				// If the notification contains listData
				else
				{
					String node = "SOAToUpstream.SOAToUpstreamBody."
									+ notification 
									+ ".Subscription.TnSvIdList.TnSvId("
									+ j + ")" + ".SvId";

					if (parser.exists( node ))
					{
						startSVID = Long.parseLong(parser.getValue( node ));

						//set the SVID in context
						super.set(SOAConstants.SVID_LOCATION, mpContext, inputObject,
								String.valueOf( startSVID ));
					}
				}
				
				svidStatement.setString( 1, getValue( svidValue ) );

				svidStatement.setString( 2, spidValue );

				svidStatement.setString( 3, regionIdValue );					

				// Look for record in SV object table with SVID, REGIONID and SPID
				rs = svidStatement.executeQuery();
					
				// If record exists with SVID, REGIONID and SPID
				if (rs.next())
				{
					referenceKey = rs.getLong( SOAConstants.REFERENCEKEY_COL );

					statusValue = rs.getString( SOAConstants.STATUS_COL );
					
					Tn = rs.getString(SOAConstants.PORTINGTN_COL);
					oid =rs.getString(SOAConstants.OID_COL);
					transOid=rs.getString(SOAConstants.TransOID_COL);
					oidSet.add(oid);
					transOidSet.add(transOid);
					tnoid = new TnOIDTransOID(Tn,oid,transOid);
					oIDTransOidTnlst.add(tnoid);

					//set the REFERENCE key in context
					super.set(SOAConstants.REFERENCEKEY_LOCATION, mpContext, 
								inputObject, String.valueOf( referenceKey ) );

					// Reset the values in cases where we're logging again.
					resetColumnValues( );

					// Extract the column values from the arguments.
					extractMessageData( mpContext, inputObject );

					//update SV Object table based on referenceKey
					update( pstmtUpdate, referenceKey );
				}
				// If record doesn't exists for the SVID
				else 
				{

					svidTnStatement.setString( 1, tn.toString() );

					svidTnStatement.setString( 2, spidValue );

					svidTnStatement.setString( 3, onspValue );

					svidTnStatement.setString( 4, nnspValue );
					
					// Look for record in SV object table with TN, SPID, ONSP and NNSP
					rs = svidTnStatement.executeQuery();

					// If record exists with with TN, SPID, ONSP and NNSP
					if (rs.next())
					{
						insertFlag = false;

						referenceKey = rs.getLong( SOAConstants.REFERENCEKEY_COL );

						statusValue = rs.getString( SOAConstants.STATUS_COL );
						
						Tn = rs.getString(SOAConstants.PORTINGTN_COL);
						oid =rs.getString(SOAConstants.OID_COL);
						transOid=rs.getString(SOAConstants.TransOID_COL);
						oidSet.add(oid);
						transOidSet.add(transOid);
						tnoid = new TnOIDTransOID(Tn,oid,transOid);
						oIDTransOidTnlst.add(tnoid);

						//set the REFERENCE key in context
						super.set(SOAConstants.REFERENCEKEY_LOCATION, mpContext, 
									inputObject, String.valueOf( referenceKey ) );

						// Reset the values in cases where we're logging again.
						resetColumnValues( );

						// Extract the column values from the arguments.
						extractMessageData( mpContext, inputObject );
						
						if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
							Debug.log( Debug.DB_DATA, "Executing insertFlag SQL:\n" + insertFlag );
						}

						// If the status of the record in DB is NPACCreateFailure then set 
						// insertFlag and updateRefFlag as true and delete the record from SV Object table
						if (statusValue.equals(SOAConstants.NPAC_CREATE_FAILURE))
						{							
							insertFlag = true;
														
							deleteStatement.setLong( 1, referenceKey );

							// Delete the record from object table for the referenceKey
							deleteStatement.addBatch();

							updateRefFlag = true;
							updateRefKey = referenceKey;
							
						}
						// Update the SV Object table based on referenceKey
						else
						{
							update( pstmtUpdate, referenceKey );
						}

					}

					// If insertFlag the Insert record in SV Object table
					if (insertFlag)					
					{
																	
						//get reference key from sequence
						referenceKey = PersistentSequence
									.getNextSequenceValue( seqName, false,dbConn );
						
						// If the updateRefFlag is true the upadte Map table with the new referenceKey
						if (updateRefFlag)
						{
						
							updateMapStatement.setLong( 1, referenceKey );
							updateMapStatement.setLong( 2, updateRefKey );
							// Update the referenceKey in message Map table
							updateMapStatement.addBatch();
						}

						//set the REFERENCE key in context
						super.set(SOAConstants.REFERENCEKEY_LOCATION, mpContext, 
								inputObject, String.valueOf( referenceKey ) );

						// Reset the values in cases where we're logging again.
						resetColumnValues();

						// Extract the column values from the arguments.
						extractMessageData( mpContext, inputObject );
						
						  // add Subdomain Value on Conetxt for TNs in Case of Object Create Notificatons for ONSP.
						  if( useSubDomain )
						  {
							    String subdomain = "";
								int tnIndex = tn_Subdomain.indexOf(tn.toString());
								
								if (tnIndex != -1)
								{
									subdomain = (String)subdomain_Tn.get(tnIndex);
								}
								
															
								//set the Subdomain Value in context
								currentSubDomain = subdomain;
								
						  }						
						

						// insert into SV Object table
						insert( pstmtInsert );
					}

					// if resultSet is not null
					if (rs != null)
					{
						// close the resultSet
						rs.close();

						rs = null;
					}
				}

				// add REFERENCE key in REFERENCE key XML
				referenceKeyXML.setValue( SOAConstants.KEYCONTAINER_NODE + "."
					+ SOAConstants.KEY_NODE + "(" + j + ")",
					String.valueOf( referenceKey ));
				
			} // end of for loop
			boolean isRange = false;
			if (oidSet.size() == 1 && transOidSet.size() == 1 && oIDTransOidTnlst.size() > 1){
				isRange = true;
			}else{
				//individual handling
				isRange = false;
			}
			populateOIDTransOID(oIDTransOidTnlst,isRange, inputObject, mpContext);
			
			// Execute all the batch statement
			if (deleteStatement != null) {
				// Delete the referenceKey values, which are added in the batch from object table
				int countDelete[] = deleteStatement.executeBatch();

				
			}

			if (updateMapStatement != null) {
				// Update the referenceKey values, which are added in the batch into message map table
				int countUpdate[] = updateMapStatement.executeBatch();

			}			

			pstmtInsert.executeBatch();
			pstmtUpdate.executeBatch();

			// If the configuration indicates that this SQL operation
			// isn't part of the overall driver
            // transaction, commit the changes now.
            if ( !usingContextConnection )
            {

                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS,
						"Committing data inserted by TNRangeLogger to database." );
				}

                try
                {

                   DBConnectionPool.getInstance( true ).commit( dbConn );

                }
                catch( ResourceException re )
                {

                    Debug.log( Debug.ALL_ERRORS, re.getMessage() );

                }

            }

			// set the REFERENCE key XML in context
			super.set( keyXMLLocation, mpContext, inputObject,
						referenceKeyXML.getDocument() );

			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

				Debug.log(Debug.MSG_STATUS,
					  "TNRangeLogger: Generated REFERENCEKey XML :\n"
						+ referenceKeyXML.generate() );

			}
		}
		catch ( Exception e )
        {
            String errMsg = "ERROR: TNRangeLogger: Attempt to log to database "
							+ "failed with error: " + e.getMessage();

            Debug.log( Debug.ALL_ERRORS, errMsg );

            // If the configuration indicates that this SQL operation isn't part
			// of the overall driver transaction, roll back any changes now.
            if ( !usingContextConnection )
            {

                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log( Debug.MSG_STATUS,
						"Rolling-back any database changes due to TNRangeLogger." );
				}

                try
                {

                    DBConnectionPool.getInstance( true ).rollback( dbConn );

                }
                catch( ResourceException re )
                {

                    Debug.log( Debug.ALL_ERRORS, re.getMessage() );

                }
            }

            // Re-throw the exception to the driver.
            if ( e instanceof MessageException )
			{
                throw (MessageException)e;
			}
            else
			{
                throw new ProcessingException( errMsg );
			}
        }
        finally
        {

			try
			{
				// if statement not closed
				if (svidStatement != null)
				{

					// close the statement
					svidStatement.close();

					svidStatement = null;

				}
				// if statement not closed
				if (svidTnStatement != null)
				{

					// close the statement
					svidTnStatement.close();

					svidTnStatement = null;

				}

				// if prepared statement not closed
				if (pstmtInsert != null)
				{

					// close the prepared statement
					pstmtInsert.close();

					pstmtInsert = null;

				}

				// if prepared statement not closed
				if (pstmtUpdate != null)
				{

					// close the prepared statement
					pstmtUpdate.close();

					pstmtUpdate = null;

				}

				// if statement not closed
				if (deleteStatement != null)
				{

					// close the statement
					deleteStatement.close();

					deleteStatement = null;

				}

				// if statement not closed
				if (updateMapStatement != null)
				{

					// close the statement
					updateMapStatement.close();

					updateMapStatement = null;

				}				
			}
			catch ( SQLException sqle )
			{

				Debug.log( Debug.ALL_ERRORS,
					DBInterface.getSQLErrorMessage(sqle) );

			}

            // If the configuration indicates that this SQL operation isn't
			// part of the overall driver transaction, return the connection
			// previously acquired back to the resource pool.
            if ( !usingContextConnection && (dbConn != null) )
            {
                try
                {

                    // Release the connection to the pool
					DBConnectionPool.getInstance(true)
									.releaseConnection( dbConn );

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
		return( formatNVPair( inputObject ) );
	}
	/**
	 * This method populated Tn , OID and TransOID in response XML.  
	 * @param transOidTnlst
	 * @param isRange
	 * @param mobj
	 * @param mpContext2
	 * @throws MessageException
	 * @throws ProcessingException
	 */
	private void populateOIDTransOID(List<TnOIDTransOID> transOidTnlst, boolean isRange, MessageObject mobj,
			MessageProcessorContext mpContext2) throws MessageException, ProcessingException {
		
		Document dom = mobj.getDOM();
		Iterator iter = transOidTnlst.iterator();
		Element ele = null;
		if(isRange){
			
			StringBuffer tn = new StringBuffer( npaValue );
			tn.append( tnDelimiter );
			tn.append( nxxValue );
			tn.append( tnDelimiter );
			tn.append(startTNValue);
			tn.append( tnDelimiter );
			tn.append(endTNValue);
			TnOIDTransOID tnOiDTransOid = (TnOIDTransOID)iter.next();
			
			if (tnOiDTransOid != null){
				if(tnOiDTransOid.oid != null || tnOiDTransOid.transOid != null){
					
					
					Element xmlElement_Tn = dom.createElement(SOAConstants.TN_NODE);
					xmlElement_Tn.setAttribute("value", tn.toString());
					
					if( tnOiDTransOid.oid  != null){ 
					
						
						Element xmlElement_OID = dom.createElement(SOAConstants.OID);
						xmlElement_OID.setAttribute("value",tnOiDTransOid.oid.toString());
						xmlElement_Tn.appendChild(xmlElement_OID);
					}
					if ( tnOiDTransOid.transOid != null){
						
						
						Element xmlElement_TransOID = dom.createElement(SOAConstants.TransOID);
						xmlElement_TransOID.setAttribute("value",tnOiDTransOid.transOid.toString());
						xmlElement_Tn.appendChild(xmlElement_TransOID);
					}
					ele = (Element) dom.getElementsByTagName("SOAToUpstreamHeader").item(0);
					ele.appendChild(xmlElement_Tn);
				}
			}else{
				
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS, this.getClass() + "OID and TransOID fields are not populated in request XML.");
				}
				
			}
			
			inputObject.set(dom);

			
		}else{
			
				
			while(iter.hasNext()){
				TnOIDTransOID tnOiDTransOid = (TnOIDTransOID)iter.next();
				
				if (tnOiDTransOid != null){
					
					if(tnOiDTransOid.oid != null || tnOiDTransOid.transOid != null){
					
						
						
						Element xmlElement_Tn = dom.createElement(SOAConstants.TN_NODE);
						xmlElement_Tn.setAttribute("value", tnOiDTransOid.tn.toString());
						
						if( tnOiDTransOid.oid  != null){ 
						
							
							Element xmlElement_OID = dom.createElement(SOAConstants.OID);
							
							xmlElement_OID.setAttribute("value",tnOiDTransOid.oid.toString());
							xmlElement_Tn.appendChild(xmlElement_OID);
						}
						if ( tnOiDTransOid.transOid != null){
							
							
							Element xmlElement_TransOID = dom.createElement(SOAConstants.TransOID);
							
							xmlElement_TransOID.setAttribute("value",tnOiDTransOid.transOid.toString());
							xmlElement_Tn.appendChild(xmlElement_TransOID);
						}
						ele = (Element) dom.getElementsByTagName("SOAToUpstreamHeader").item(0);
						ele.appendChild(xmlElement_Tn);
					}
					inputObject.set(dom);
					
				}else{
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(Debug.MSG_STATUS, this.getClass() + "OID and TransOID fields are not populated in request XML.");
					}
					
				}
			}
		}
	}



	/**
	* Reset the column values in the list.
	*/
	private void resetColumnValues ( ) {

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log (Debug.MSG_STATUS, "Resetting column values ..." );
		}

		Iterator iter = columns.iterator( );

		ColumnData cd = null;

		// While columns are available ...
		while ( iter.hasNext() )
		{
			cd = (ColumnData)iter.next( );

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
    private void extractMessageData ( MessageProcessorContext context,
					MessageObject inputObject )
					throws MessageException, ProcessingException {

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log (Debug.MSG_STATUS, "Extracting message data to log ..." );
		}

        Iterator iter = columns.iterator( );

		ColumnData cd = null;

        // While columns are available ...
        while ( iter.hasNext() )
        {
            cd = (ColumnData)iter.next( );

            if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
                Debug.log( Debug.MSG_DATA, "Extracting data for:\n"
							+ cd.describe() );

            // If location was given, try to get a value from it.
            if ( StringUtils.hasValue( cd.location ) )
            {
                // Location contains one or more alternative locations
				// that could contain the column's value.
                StringTokenizer st = new StringTokenizer( cd.location,
															separator );

				String loc = null;

                // While location alternatives are available ...
                while ( st.hasMoreTokens() )
                {
                    // Extract a location alternative.
                    loc = st.nextToken().trim( );

                    // If the value of location indicates that the input
					// message-object's entire value should be used as the
					// column's value, extract it.
                    if ( loc.equalsIgnoreCase( INPUT_MESSAGE )
							|| loc.equalsIgnoreCase( SOAConstants.PROCESSOR_INPUT ) )
                    {

                        if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
							Debug.log( Debug.MSG_DATA, "Using message-object's "
									+ "contents as the column's value." );
						}

                        cd.value = inputObject.get( );

                        break;

                    }

                    // Attempt to get the value from the context or input object.
                    if ( exists( loc, context, inputObject ) )
					{
                        cd.value = get( loc, context, inputObject );
					}

                    // If we found a value, we're done with this column.
                    if ( cd.value != null )
                    {

                        if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
							Debug.log( Debug.MSG_DATA, "Found value for column ["
							+ cd.columnName + "] at location [" + loc + "]." );
						}

                        break;

                    }
                }
            }

            // If no value was obtained from location in context/input,
			// try to set it from default value (if available).
            if ( cd.value == null )
            {

                cd.value = cd.defaultValue;

                if ( cd.value != null )
				{
                    if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
						Debug.log( Debug.MSG_DATA,
						"Using default value for column ["
						+ cd.columnName + "]." );
					}
				}
            }

            // If no value can be obtained ...
            if ( cd.value == null )
            {
                // If the value is required ...
                if ( cd.optional == false )
                {

                    // Signal error to caller.
                    throw new MessageException(
						"ERROR: Could not locate required value for column ["
						+ cd.columnName + "], database table ["
						+ tableName + "]." );

                }
				// No optional value is available, so just continue on.
                else
				{	
                    if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
						Debug.log( Debug.MSG_DATA, "Skipping optional column ["
						+ cd.columnName + "] since no data is available." );
					}
				}
            }
        }

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
			Debug.log (Debug.MSG_STATUS, "Done extracting message data to log." );
		}
	}

	/**
     * Insert a single row into the database table using the given statement.
     *
     * @param  dbConn  The database connection to perform the SQL
	 * INSERT operation against.
     *
     * @exception  MessageException  Thrown on data errors.
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void insert ( PreparedStatement pstmt )
		throws MessageException, ProcessingException {

        // Make sure that at least one column value will be inserted.
        validate( );

        try
        {            
			long refKey = -1;

            // Populate the SQL statement using values obtained from the
			// column data objects.
            populateSqlStatement( pstmt, true, refKey );

            // Execute the SQL INSERT operation.
            pstmt.addBatch( );
           
        }
        catch ( SQLException sqle )
        {

            throw new ProcessingException(
				"ERROR: Could not insert row into database table ["
				+ tableName + "]:\n" + DBInterface.getSQLErrorMessage(sqle) );

        }
        catch ( Exception e )
        {

            throw new ProcessingException(
				"ERROR: Could not insert row into database table ["
				+ tableName + "]:\n" + e.toString() );

        }
        
    }


    /**
     * Construct the SQL INSERT statement from the column data.
     *
     * @return SQL INSERT statement.
	 *
	 * @exception  DatabaseException  Thrown on data errors.
     */
    private String constructInsertSqlStatement ( ) throws DatabaseException {

        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log( Debug.DB_DATA, "Constructing SQL INSERT statement ..." );
		}

        // Construct insert query for the column data
		StringBuffer sb = new StringBuffer( );
        
		StringBuffer sbColumnName = new StringBuffer( );
        
		StringBuffer sbValue = new StringBuffer( );

        sb.append( "INSERT INTO " );

        sb.append( tableName );

        sb.append( " ( " );

        boolean firstOne = true;

        Iterator iter = columns.iterator( );

		ColumnData cd = null;

        // Append the names of columns with non-null values.
        while ( iter.hasNext() )
        {
            cd = (ColumnData)iter.next( );

            // Skip columns with null values since null values aren't inserted.
            if ( SOAUtility.isNull( cd.value ) )
			{
                continue;
			}

            if ( firstOne )
			{
                firstOne = false;
			}			
			else
			{	 
				// if column name is LNPTYPE and notification is SvCreateNotification or SvReleaseNotification or SvReleaseInConflictNotification or SvCreateAckNotification or SvReleaseInConflictAckNotification
				if( cd.columnName.equals(SOAConstants.LNPTYPE_COL))
				{					
					if ( notification.equals(SOAConstants.SV_CREATE_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_NOTIFICATION) 
							|| notification.equals(SOAConstants.SV_RELEASE_CONF_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_ACK_NOTIFICATION) || 
							notification.equals(SOAConstants.SV_CREATE_ACK_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION)) 		
					{						   
						sbColumnName.append( ", " );	
						
						sbValue.append( ", " );
						
					}
				 }
				else
				{
					sbColumnName.append( ", " );
									
					sbValue.append( ", " );	
				}
				
			}			

			// if column name is LNPTYPE and notification is SvCreateNotification or SvReleaseNotification or SvReleaseInConflictNotification
			if( cd.columnName.equals(SOAConstants.LNPTYPE_COL))
			{
				 if ( notification.equals(SOAConstants.SV_CREATE_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_NOTIFICATION) 
						 || notification.equals(SOAConstants.SV_RELEASE_CONF_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_ACK_NOTIFICATION) || 
						 notification.equals(SOAConstants.SV_CREATE_ACK_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION)) 
					 
					sbColumnName.append( cd.columnName );
			}
			else
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
				if( cd.columnName.equals(SOAConstants.LNPTYPE_COL))
				{					
					if ( notification.equals(SOAConstants.SV_CREATE_NOTIFICATION)  || notification.equals(SOAConstants.SV_RELEASE_NOTIFICATION) 
							|| notification.equals(SOAConstants.SV_RELEASE_CONF_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_ACK_NOTIFICATION) || 
							notification.equals(SOAConstants.SV_CREATE_ACK_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION)) 		
					{						   
						sbValue.append( "?" );
						
					}
			    }
                else
			    {
					sbValue.append( "?" );
				}
				
		    }
        }

        if ( useCustomerId )
        {
            //Add customer information
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
        
        if ( useSubDomain )
        {
            //Add customer information
            if ( firstOne )
			{
                firstOne = false;
			}
            else
			{
				sbColumnName.append( ", " );
				
				sbValue.append( ", " );
				
			}

             sbColumnName.append("SUBDOMAINID");
            
			sbValue.append( "?" );
        }        
		
        //Adding rangeid in insert statement
        if (!rangeid.equals("") || rangeid != "")
		{
			sbColumnName.append( ", " );
			
			sbValue.append( ", " );
			
			sbColumnName.append( " RANGEID " );
			
			sbValue.append( rangeid);
		}

        sb.append( sbColumnName.toString() );
		
        sb.append( " ) VALUES ( " );
        
		sb.append( sbValue.toString() );
        
        sb.append( " )" );

        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log( Debug.DB_DATA, "Done constructing SQL INSERT statement." );
		}

        return( sb.toString() );
    }


	/**
     * Construct the SQL UPDATE statement from the column data.
     *
     * @return SQL UPDATE statement.
	 *
	 * @exception  ProcessingException.
     */
    private String constructUpdateSqlStatement ()
									throws ProcessingException {

        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log( Debug.DB_DATA, "Constructing SQL UPDATE statement ..." );
		}

        // Construct sql statement for update
		StringBuffer sb = new StringBuffer( );

        sb.append( "UPDATE " );

        sb.append( tableName );

        sb.append( " SET " );

        boolean firstOne = true;

        Iterator iter = columns.iterator( );

		ColumnData cd = null;

        // Append the names of columns with non-null values.
        while ( iter.hasNext() )
        {
            cd = (ColumnData)iter.next( );

            // Skip columns with null values since null values aren't updated.
			// and skip if update is false
            
			if ( SOAUtility.isNull( cd.value ) || !( cd.update ) )
			{
				continue;
			}

            if ( firstOne )
			{
                firstOne = false;
			}
            else
			{
                sb.append( ", " );
			}

            sb.append( cd.columnName );

			// If the current column is a date column, and the configuration
			// indicates that the current system date should be used for
			// the value, place it in the SQL statement now.
            if ( StringUtils.hasValue( cd.columnType )
				&& cd.columnType.equalsIgnoreCase( SOAConstants.COLUMN_TYPE_DATE )
                && (cd.value instanceof String)
				&& ((String)( cd.value ) ).equalsIgnoreCase( SOAConstants.SYSDATE ) )
			{

				sb.append( " = " );
                sb.append( SOAConstants.SYSDATE );

			}
            else
			{

                sb.append( " = ? " );

			}
        }

        sb.append( " WHERE ");
		sb.append( SOAConstants.REFERENCEKEY_COL );
		sb.append( " = ?" );

        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log( Debug.DB_DATA, "Done constructing SQL UPDATE statement. "
					+ sb.toString() );
		}

        return( sb.toString() );
    }


	/**
     * update into the database table using the given connection.
     *
     * @param  dbConn  The database connection to perform the SQL update
	 * operation against.
	 * @param  referenceKey  The message key, to perform the SQL update.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void update ( PreparedStatement pstmt, long referenceKey )
								throws ProcessingException {

        // Make sure that at least one column value will be updated.
        validate( );

        try
        {            
            // Populate the SQL statement using values obtained from
			// the column data objects.
            populateSqlStatement( pstmt, false, referenceKey );

            // Execute the SQL Update operation.
            pstmt.addBatch( );            
        }
        catch ( SQLException sqle )
        {

            throw new ProcessingException(
						"ERROR: Could not update row into database table ["
						+ tableName + "]:\n"
						+ DBInterface.getSQLErrorMessage(sqle) );

        }
        catch ( ProcessingException e )
        {

            throw new ProcessingException(
						"ERROR: Could not update row into database table ["
						+ tableName + "]:\n" + e.toString() );

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
    private void populateSqlStatement ( PreparedStatement pstmt,
							boolean isInsert, long referenceKey ) throws ProcessingException {

        if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log( Debug.DB_DATA, "Populating SQL INSERT statement ..." );
		}
		
		try
		{
		
	        int Ix = 1;  // First value slot in prepared statement.
	
	        Iterator iter = columns.iterator( );
	
			ColumnData cd = null;
	
	        // Append the names of columns with non-null values.
			while ( iter.hasNext() )
	        {
	            cd = (ColumnData)iter.next( );
	
	            if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
	                Debug.log( Debug.MSG_DATA, "Populating SQL statement for:\n"
								+ cd.describe() );
	
	            // Skip columns with null values since null values aren't inserted
				// or updated and also skip columns for update if update is false.
	            if ( SOAUtility.isNull(cd.value) || ( !isInsert && !( cd.update ) ) )
						continue;
					
	            if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
					Debug.log( Debug.DB_DATA, "Populating prepared-statement slot ["
									+ Ix + "]." );
				}
	
	            // Default is no column type specified.
	            if ( !StringUtils.hasValue(cd.columnType) )
	            {	                
	
					// if column data is null
					if ( SOAUtility.isNull(cd.value) )
					{
						// assign null value to the slot
						pstmt.setNull(Ix, java.sql.Types.VARCHAR );
					}
					else
					{
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log( Debug.MSG_DATA, "Value for column ["
							+ cd.columnName + "] is [" + cd.value.toString() + "]." );
						}

						if (cd.columnName.equals(SOAConstants.LNPTYPE_COL))
						{							
							if ( notification.equals(SOAConstants.SV_CREATE_NOTIFICATION)  || notification.equals(SOAConstants.SV_RELEASE_NOTIFICATION) 
									|| notification.equals(SOAConstants.SV_RELEASE_CONF_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_ACK_NOTIFICATION) 
									|| notification.equals(SOAConstants.SV_CREATE_ACK_NOTIFICATION) || notification.equals(SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION))
							{
								//Check If incoming notification is received for IntraPort request.
								if (spidValue.equals(onspValue) && spidValue.equals(nnspValue)){
									
									Debug.log(Debug.MSG_DATA, "\nThe notification [" +notification.toString()+ "] is received for intra port request.");
									Debug.log(Debug.MSG_DATA, "The Notification parameters:");
									Debug.log(Debug.MSG_DATA, "SPID: ["+spidValue+"], ONSP: ["+onspValue+"], NNSP: ["+nnspValue+"]\n");
									
									// assign the column value to slot
									pstmt.setObject( Ix, LISP );
								}
								else{
									
									// assign the column value to slot
									pstmt.setObject( Ix, cd.value );
								}								
							}
							else
							//// Otherwise decrement slot value in prepared statement.
								Ix--;
						}
						else
							pstmt.setObject( Ix, cd.value );
					}										
					   
				}  
				// if column type is date
	            else
				if ( cd.columnType.equalsIgnoreCase(SOAConstants.COLUMN_TYPE_DATE) )
	            {	                

					// if column data is null
					if ( SOAUtility.isNull(cd.value) )
					{
						// assign null value to the slot
						pstmt.setNull(Ix, java.sql.Types.TIMESTAMP );
					}
					else
					{
						String val = (String)(cd.value);
						
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
							Debug.log( Debug.MSG_DATA, "Value for date column ["
							+ cd.columnName + "] is [" + val + "]." );
						}
	
					// SYSDATE is already in the text of the SQL statement used to create
					// the prepared statement, so there's nothing more to do here.
					if ( val.equalsIgnoreCase(SOAConstants.SYSDATE) )
					{

						if ( Debug.isLevelEnabled( Debug.MSG_STATUS) ){
							Debug.log( Debug.MSG_STATUS, "Skipping date population "
							+ "since SYSDATE is already in SQL string." );
						}

						continue;

					}

					// If we're not inserting the current system date, the caller must
					// have provided an actual date value, which we must now parse.
					if ( !StringUtils.hasValue( cd.dateFormat ) )
					{

						throw new ProcessingException( "ERROR: Configuration for "
								+ " date column [" + cd.columnName
								+ "] does not specify date format." );

					}					

					SimpleDateFormat sdf = new SimpleDateFormat( cd.dateFormat );

					java.util.Date d = sdf.parse( val );
					
					pstmt.setTimestamp( Ix,
										new java.sql.Timestamp( d.getTime() ) );
					}
				}
				// if column type is Text BLOB
				else
				if ( cd.columnType.equalsIgnoreCase( 
											SOAConstants.COLUMN_TYPE_TEXT_BLOB) )
				{
					// if column data is null
					if ( SOAUtility.isNull(cd.value) )
					{
						// assign null value to the slot
						pstmt.setNull(Ix, java.sql.Types.CLOB );
					}
					else
					{
	
					if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
						Debug.log( Debug.MSG_DATA, "Querying column ["
						+ cd.describe() + "]." );
	
					DBLOBUtils.setCLOB( pstmt, Ix,
										Converter.getString(cd.value) );
					}
	
				}
				// if column type is Binary BLOB
				else
				if ( cd.columnType.equalsIgnoreCase( 
										SOAConstants.COLUMN_TYPE_BINARY_BLOB) )
				{
	
					byte[] bytes = null;
	
					if ( cd.value instanceof byte[] )
						bytes = (byte[])cd.value;
	
					else
					if ( cd.value instanceof String )
						bytes = ((String)(cd.value)).getBytes( );
	
					else
					if ( cd.value instanceof Document )
						bytes = Converter.getString(cd.value).getBytes( );
	
					else
					{
	
						throw new ProcessingException(
							"ERROR: Value for database table ["
							+ tableName + "], column [" + cd.columnName
							+ "] of type [" + cd.value.getClass().getName( )
							+ "] can't be converted to byte stream." );
	
					}

					// if column data is null
					if ( SOAUtility.isNull(cd.value) )
					{
						// assign null value to the slot
						pstmt.setNull(Ix, java.sql.Types.BLOB );
					}
					else
					{
	
						if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
						{
							Debug.log( Debug.MSG_DATA, "Querying column ["
										+ cd.describe( ) + "]." );
						}
		
						DBLOBUtils.setBLOB( pstmt, Ix, bytes );
					}
	
				}
				else
				{
	
					throw new ProcessingException(
								"ERROR: Invalid column-type property value ["
								+ cd.columnType + "] given in configuration." );
	
				}
	
				// Advance to next value slot in prepared statement.
				Ix ++;
			}
	
			// if insret and useCustomerId is true
			if ( useCustomerId && isInsert )
			{
	
				// Use Ix slot number as-is, because it was already incremented
				// before exiting above loop.
				SQLBuilder.populateCustomerID( pstmt, Ix );
	
			}
			
			// if insret and useSubDomain is true
			if ( useSubDomain && isInsert )
			{
					
				// Use Ix slot number as-is, because it was already incremented
				// before exiting above loop.
				pstmt.setString(Ix+1,currentSubDomain);
	
			}			

			if (!isInsert)
			{
				pstmt.setObject( Ix, Long.valueOf(referenceKey) );
			}
		}catch( Exception exception )
		{
			throw new ProcessingException( 
							"ERROR:  could not populate sql statement ."
							+ exception.toString() );

		}

		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log( Debug.DB_DATA,
					"Done populating SQL INSERT/UPDATE statement." );
		}
	}

	/**
	* Check that columns were configured and at least one
	* mandatory field has a value to insert.
	*
	* @exception ProcessingException  thrown if invalid
	*/
	private void validate ( ) throws ProcessingException {

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS) ){
			Debug.log (Debug.MSG_STATUS, "Validating column data ..." );
		}

		boolean valid = false;

		Iterator iter = columns.iterator( );

		ColumnData cd = null;

		// While columns are available ...
		while ( iter.hasNext( ) )
		{

			cd = (ColumnData)iter.next( );

			// If we've found at least one value to insert, its valid.
			if ( cd.value != null )
			{
				valid = true;

				break;
			}

		}

		if ( !valid )
		{

			throw new ProcessingException(
				"ERROR: No database column values are available to write to ["
			   + tableName + "]." );

		}

		if ( Debug.isLevelEnabled( Debug.DB_DATA ) ){
			Debug.log( Debug.DB_DATA, "Done validating column data." );
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
        // Location contains one or more alternative locations
		// that could contain the column's value.
		StringTokenizer st = new StringTokenizer( locations,
								DBMessageProcessorBase.SEPARATOR );

		String tok = null;

        // While tokens are available ...
		while ( st.hasMoreTokens() )
        {
            tok = st.nextToken( );

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS) ){
				Debug.log( Debug.MSG_STATUS, "Checking location ["
                                  + tok + "] for value..." );
			}

            // if the value of token exists in context or messageobject.
			if ( exists( tok, mpContext, inputObject ) )
			{
                // get the token value from context and return the value
				return( (String) get( tok, mpContext, inputObject ) );
			}
        }

		// if not exists
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
        
		public final boolean update;

        public Object value = null;


        public ColumnData ( String columnName, String columnType,
							String dateFormat, String location,
							String defaultValue, String optional,
							String update )
							throws FrameworkException {

            this.columnName   = columnName;
            
            this.columnType   = columnType;
            
            this.dateFormat   = dateFormat;
            
            this.location     = location;
            
            this.defaultValue = defaultValue;
            
            this.optional     = StringUtils.getBoolean( optional );
            
			this.update     = StringUtils.getBoolean( update );
			
        }

		/**
		 * To describe the column name, column type, data format, location
		 * and default value.
		 */

        public String describe ( ) {

            StringBuffer sb = new StringBuffer( );

            sb.append( "Column description: Name [" );
            sb.append( columnName );

            if ( StringUtils.hasValue( columnType ) )
            {
                sb.append( "], type [" );
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

            sb.append( "], optional [" );
            sb.append( optional );

			sb.append( "], update [" );
            sb.append( update );

            if ( value != null )
            {
                sb.append( "], value [" );
                sb.append( value );
            }

            sb.append( "]." );

            return( sb.toString() );
        }
    }

    /**
     * This method get the subdomain value for Object Create Notifications 
     * from the existing recored of Object table and SOA_SUBDOMAIN_ALTSPID table.
     * 
     * @param conn, Connection
     */
    private void getSubdomainValue( Connection conn )
    {
		int startTN = 0;
		
		int endTN = 0;
		
		Connection dbCon = null;

		PreparedStatement pstmt = null;
		
		ResultSet results = null;

		startTN = Integer.parseInt(startTNValue);

		endTN = Integer.parseInt(endTNValue);

		ArrayList tnList = new ArrayList();
		
		ArrayList ValidTNList = new ArrayList();
		
		//Added for identifying whether its a true range or not

		String tnSeperator = "-";
		
		if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
			Debug.log(Debug.MSG_DATA ,"Getting the Value of  Subdomain in getSubdomainValue() method.");
		}
		
		// Process all the TN that the notification contains
		for (int i = startTN, j = 0; i <= endTN; i++, j++ ) 
		{
			StringBuffer tn = new StringBuffer( npaValue );
			
			tn.append( tnSeperator );
			
			tn.append( nxxValue );
			
			tn.append( tnSeperator );
			
			tn.append( StringUtils.padNumber( i, SOAConstants.TN_LINE,true, '0' ));
			
			tnList.add(tn.toString());
		}
		
		if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
			Debug.log(Debug.MSG_DATA ," Created All TN List.");
		}
		
		StringBuffer tnQuery = new StringBuffer();
		
		StringBuffer mainQuery = new StringBuffer();

		ArrayList validTNList = new ArrayList();
		
		ArrayList tnWithSubdomain = new ArrayList();
		
		try
		{
            // Get a connection from the pool if not received from the caller.
            if (conn != null)
            {
                dbCon=conn;
            }
            else
            {
                dbCon = DBConnectionPool.getInstance().acquireConnection( );
            }		

            if(dbCon == null) throw new SQLException("DB Connection is not available.");
            
			int i = 0;
	
			int tnCount = tnList.size();
			
			if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
				Debug.log(Debug.MSG_DATA , "getSubdomainValue.tnList: "+tnList);
			}
			
			// Get the Subdomain Value direct from Object Table.
			while (i < tnCount)
			{			
				int j = 1;
	
				StringBuffer tnValue = new StringBuffer();
	
				while (j <= 1000 && i <= tnCount-1 )
				{
					tnValue.append("'");
					tnValue.append(tnList.get(i).toString());
					
					if ( j < 1000 && i != tnCount-1)				
						tnValue.append("',");
					else
						tnValue.append("'");
	
					i++;
					j++;
					
				}// end of inner while
				
				tnQuery.append("select /*+ index ( SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 SOA_SV_INDEX_7 ) */ PORTINGTN, ALTERNATIVESPID,SUBDOMAINID ");	
				tnQuery.append(" from SOA_SUBSCRIPTION_VERSION ");
				tnQuery.append(" where ");
				tnQuery.append("(PORTINGTN, SPID, CREATEDDATE) ");
				tnQuery.append(" in ");
					tnQuery.append( "(select /*+ index ( SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 ) */ ");
					tnQuery.append( " PORTINGTN, SPID, max(CREATEDDATE) ");
					tnQuery.append(" from SOA_SUBSCRIPTION_VERSION ");
					tnQuery.append(" where ");
					tnQuery.append(" PORTINGTN in ("+tnValue+ " ) ");
					tnQuery.append(" and  SPID = '"+spidValue+"' ");
					tnQuery.append(" and STATUS in ( 'active') and NNSP = SPID  ");
					tnQuery.append(" and SUBDOMAINID IS NOT NULL ");
					tnQuery.append(" group by PORTINGTN, SPID)");
				tnQuery.append(" and STATUS in ( 'active')  ");
				tnQuery.append(" and SUBDOMAINID IS NOT NULL  ");
				
	
				mainQuery.append(tnQuery);
				
				if (i <= tnCount-1)
				{
					mainQuery.append(" UNION ");
				}
						
			 }// end of outer while		
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
				Debug.log(Debug.MSG_STATUS,"  getSubdomainValue.mainQuery.SQL_1: "+mainQuery);
			}
			
			pstmt = dbCon.prepareStatement(mainQuery.toString());		 

			results = pstmt.executeQuery();			
						//Stored all the result in tnWithSubdomain with (TN, SUBDOMAIN) combiantion.
			while (results.next())
			{
				String tn= results.getString(1);
				String subdomain = results.getString(3);
				
				if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
					Debug.log(Debug.MSG_DATA ," getSubdomainValue.CreateTnSubDomainData with tn["+tn+"] subdomain["+subdomain+"]");
				}
				
				tnWithSubdomain.add( new CreateTnSubdomainData(tn, subdomain));
			}
			
			closeResultSetAndStatement(results, pstmt);
			
			//Remove tnNos from tnList, for which we get the SUBDOMAIN Value
			tnList = getSuccessTN_having_valid_SUBDOMAIN(tnList, tnWithSubdomain );
			
			
     		
			tnCount = tnList.size();
			
			if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
				Debug.log(Debug.MSG_DATA,"getSubdomainValue.tnWithSubdomain.size: "+ tnWithSubdomain.size());
				Debug.log(Debug.MSG_DATA,"getSubdomainValue.tnList.size: "+ tnCount);
			}
			
			
			
			
			//For rest of the TN we get the ALTSPID value from Object Table.
			if(tnCount >0)
			{
				 i=0;
				 
				 mainQuery.delete(0,mainQuery.length());
				 tnQuery.delete(0,tnQuery.length());
					while (i < tnCount)
					{			
						int j = 1;
			
						StringBuffer tnValue = new StringBuffer();
			
						while (j <= 1000 && i <= tnCount-1 )
						{
							tnValue.append("'");
							tnValue.append(tnList.get(i).toString());
							
							if ( j < 1000 && i != tnCount-1)				
								tnValue.append("',");
							else
								tnValue.append("'");
			
							i++;
							j++;
							
						}// end of inner while
						
						tnQuery.append("select /*+ index ( SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 SOA_SV_INDEX_7 ) */ PORTINGTN, ALTERNATIVESPID,SUBDOMAINID ");	
						tnQuery.append(" from SOA_SUBSCRIPTION_VERSION ");
						tnQuery.append(" where ");
						tnQuery.append("(PORTINGTN, SPID, CREATEDDATE) ");
						tnQuery.append(" in ");
							tnQuery.append( "(select /*+ index ( SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 SOA_SV_INDEX_6 ) */ PORTINGTN, SPID, max(CREATEDDATE) ");
							tnQuery.append(" from SOA_SUBSCRIPTION_VERSION ");
							tnQuery.append(" where ");
							tnQuery.append(" PORTINGTN in ("+tnValue+ " ) ");
							tnQuery.append(" and  SPID = '"+spidValue+"' ");
							tnQuery.append(" and STATUS in ( 'active')  and NNSP = SPID ");
							tnQuery.append(" group by PORTINGTN, SPID)");
						tnQuery.append(" and STATUS in ( 'active')  ");
						
						mainQuery.append(tnQuery);
						
						if (i <= tnCount-1)
						{
							mainQuery.append(" UNION ");
						}
								
					 }// end of outer while
					
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						Debug.log(Debug.MSG_STATUS,"  getSubdomainValue.mainQuery.SQL_2: "+mainQuery);
					}
					 
					pstmt = dbCon.prepareStatement(mainQuery.toString());		 

					results = pstmt.executeQuery();	
					
					 //Stored the result in validTNList with (TN, ALTSPID) combination.
					 while(results.next())
					 {
						 String tn = results.getString(1);
						 String altspid = "";
						 if ( results.getString(2) != null)
							 altspid = results.getString(2); 
						 
						 if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
							 Debug.log(Debug.MSG_STATUS," in resutlSet , tn["+tn+"] altspid["+altspid+"]");
						 }
						 
						 for( int z=0; z< tnList.size(); z++)
						 {
							 String portingTn = tnList.get(z).toString();
							 if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
								 Debug.log(Debug.MSG_STATUS,"TNnnn "+tn);
							 }
							 if(tn.equals(portingTn))
							 {
								 validTNList.add(new CreateTnAltSpidData(tn,altspid));
								 break;
							 }
						 }
					 }
					
					 closeResultSetAndStatement(results, pstmt);
					 
					 tnList = getSuccessTN_having_valid_ALTSPID(tnList, validTNList);
			}
			 
			tnCount = tnList.size();
			
			if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
				Debug.log(Debug.MSG_DATA,"getSubdomainValue.validTNList.size()_1"+ validTNList.size());
			
				Debug.log(Debug.MSG_DATA,"getSubdomainValue.tnList.size"+ tnCount);
			}
			
			 
			//Get the ALTSPID from NBRPoolBlcok Table.
			 if(tnCount > 0)
			 {
					
				 i=0;
				 
				 mainQuery.delete(0,mainQuery.length());
				 tnQuery.delete(0,tnQuery.length());
				 
				while (i < tnCount)
				{			
					int j = 1;
	
					StringBuffer npaValue = new StringBuffer();
					StringBuffer nxxValue = new StringBuffer();
					StringBuffer dashxValue = new StringBuffer();
	
					while (j <= 1000 && i <= tnCount-1 )
					{
						npaValue.append("'");
						npaValue.append(tnList.get(i).toString().substring(SOAConstants.TN_NPA_START_INDEX,SOAConstants.TN_NPA_END_INDEX));
						
						nxxValue.append("'");
						nxxValue.append(tnList.get(i).toString().substring(SOAConstants.TN_NXX_START_INDEX,SOAConstants.TN_NXX_END_INDEX));
						
						dashxValue.append("'");
						dashxValue.append(tnList.get(i).toString().substring(SOAConstants.TN_DASHX_START_INDEX, SOAConstants.TN_DASHX_END_INDEX));
						
						
						if ( j < 1000 && i != tnCount-1)				
						{
							npaValue.append("',");
							nxxValue.append("',");
							dashxValue.append("',");
						
						}
						else
						{
							npaValue.append("'");
							nxxValue.append("'");
							dashxValue.append("'");
						}
						i++;
						j++;
						
					}
					
					tnQuery.append(" select NPA, NXX, DASHX, ALTERNATIVESPID from SOA_NBRPOOL_BLOCK ");	
					tnQuery.append(" where ");
					tnQuery.append(" SPID = '" +spidValue+"'");
					tnQuery.append(" and NPA in ("+npaValue+" ) ");
					tnQuery.append(" and NXX in ("+nxxValue+" ) ");
					tnQuery.append(" and DASHX in ("+dashxValue+" ) ");
					tnQuery.append(" and STATUS ='active' and ALTERNATIVESPID IS NOT NULL ");
	
					mainQuery.append(tnQuery);
					
					if (i <= tnCount-1)
					{
						mainQuery.append(" UNION ");
					}
							
				 }	
				
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS,"  getSubdomainValue.mainQuery.SQL_3: "+mainQuery);
				}
				 
				pstmt = dbCon.prepareStatement(mainQuery.toString());		 

				results = pstmt.executeQuery();	
					 
				 while(results.next())
				 {
					 String npa = results.getString(1);
					 String nxx = results.getString(2);
					 String dashx = results.getString(3);
					 String altspid = "";
					 if( results.getString(4)!= null )
						 altspid = results.getString(4);
					 
					 if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						 Debug.log(Debug.MSG_STATUS," in resutlSet , npa["+npa+"] nxx["+nxx+"] dashx["+dashx+"] altspid["+altspid+"]");
					 }
					 
					 for( int z=0; z< tnList.size(); z++)
					 {
						 String tn = tnList.get(z).toString();
						 
						 if(tn.substring(0,3).equals(npa) && tn.substring(4,7).equals(nxx) && tn.substring(8,9).equals(dashx))
							 validTNList.add(new CreateTnAltSpidData(tn,altspid)); 
					 }
				 }
				 
				 closeResultSetAndStatement(results, pstmt);
				 
				 if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
					 Debug.log(Debug.MSG_DATA,"getSubdomainValue.validTNList.size()_2: "+ validTNList.size());
				 }
				 
				 tnList = getSuccessTN_having_valid_ALTSPID(tnList, validTNList);
			 }		
			 
			 tnCount = validTNList.size();
			 
			 
			 if(tnCount == 0)
			 {
				 if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					 Debug.log(Debug.MSG_STATUS,"validTNList is ZEROOO.");
				 }
			 }
			 else
			 {
				 i=0;
					 
			 	mainQuery.delete(0,mainQuery.length());
			 	tnQuery.delete(0,tnQuery.length());
			 	
				while (i < tnCount)
				{			
					int j = 1;
	
					StringBuffer altspidValue = new StringBuffer();
	
					while (j <= 1000 && i <= tnCount-1 )
					{
						altspidValue.append("'");
						altspidValue.append( ((CreateTnAltSpidData)validTNList.get(i)).altspid);
						
						if ( j < 1000 && i != tnCount-1)				
						{
							altspidValue.append("',");
						}
						else
						{
							altspidValue.append("'");
						}
						i++;
						j++;
						
					}
					
					tnQuery.append(" select CUSTOMERID, SUBDOMAINID, ALTERNATIVESPID ");	
					tnQuery.append(" from SOA_SUBDOMAIN_ALTSPID ");
					tnQuery.append(" where ");
					tnQuery.append(" ALTERNATIVESPID in ("+altspidValue+" ) ");
					if ( useCustomerId )
					{
					 String cid = null;
					 try
				        {
				            cid = CustomerContext.getInstance().getCustomerID( );
				            if(cid != null && !cid.equals(""))
				            tnQuery.append(" AND CUSTOMERID = '" +cid+"'");
				        }
				        catch ( Exception e )
				        {
				            Debug.error( e.getMessage() );

				            throw new DatabaseException( e.getMessage() );
				        }
					}
					mainQuery.append(tnQuery);
					
					if (i <= tnCount-1)
					{
						mainQuery.append(" UNION ");
					}
							
				 }		
			 
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS,"  getSubdomainValue.mainQuery.SQL_4: "+mainQuery);
				}
					
				pstmt = dbCon.prepareStatement(mainQuery.toString());		 

				results = pstmt.executeQuery();	
				 
				if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS,"  Successful Execution of main Query");
				}
				
				ArrayList resultAltSpid = new ArrayList();
				ArrayList resultSubDomain = new ArrayList();
				 
				while(results.next())
				{
					 resultAltSpid.add(results.getString(3));
					 resultSubDomain.add(results.getString(2));
				}

				closeResultSetAndStatement(results, pstmt);
				
				for(int z=0; z<validTNList.size();z++)
				{
					 String reqAltSpid =  ((CreateTnAltSpidData)validTNList.get(z)).altspid;
					 String tn = ((CreateTnAltSpidData)validTNList.get(z)).portingTn;
					
						int tnIndex = 0;
						if(reqAltSpid != null && !(reqAltSpid.equals("")) )
						{
							 tnIndex= resultAltSpid.indexOf(reqAltSpid);
							 if(tnIndex != -1)
							 {
								 String subdomain = (String) resultSubDomain.get(tnIndex);
								 tnWithSubdomain.add( new CreateTnSubdomainData(tn, subdomain) );
							 }
						}
				}
			 }			 
			
			 // Create Two ArrayList with Tn and Subdomain value.
			 if(tnWithSubdomain != null)
			 {
				 for( i=0;i <tnWithSubdomain.size();i++)
				 {
					 String ptn = "";
					 String subdomain = "";
				
					 CreateTnSubdomainData ctsd = (CreateTnSubdomainData)tnWithSubdomain.get(i);
					 
					 ptn = ctsd.portingTn;
					 subdomain = ctsd.subdomain;
					 
					 tn_Subdomain.add(ptn);
					 subdomain_Tn.add(subdomain);
				 }
			 }
		}
		catch(Exception exception )
		{
			closeResultSetAndStatement(results, pstmt);
			
			Debug.log(Debug.ALL_ERRORS,"ERROR: occured at getSubdomainValue() "+ exception.toString());
			  
		}
		finally
		{
			    if ( dbCon != null )
			    {
				try
				{
				    // Return connection back to resource pool if acquired.
				    if (conn == null)
					DBConnectionPool.getInstance().releaseConnection( dbCon );
				}
				catch ( ResourceException re )
				{
				    Debug.log( Debug.ALL_ERRORS, "ERROR: Could return database connection to the pool:\n" + re.toString() );
				}
			    }
            
		}
	        
	    
    }

    /**
     * 
     * Update the FailedTNlist, add the TN in Context.FailedTNList.
     * 
     */
    
    public ArrayList getSuccessTN_having_valid_SUBDOMAIN(ArrayList tn_list, ArrayList tnWithSubdomain)
    {
    	 
    	try
    	{
    		
    		if(tnWithSubdomain != null)
    		{
		    	for( int i=0;i< tnWithSubdomain.size();i++)
		    	{
		    		String validTN =  ((CreateTnSubdomainData)tnWithSubdomain.get(i)).portingTn;

		    		int tnIndex = tn_list.indexOf(validTN);
		    		
		    		if (tnIndex != -1)
					{
						tn_list.remove(tnIndex);
					}
		    	}
    		}
    	}	
	    catch(Exception e)
	    {
	    	if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
				Debug.log(Debug.MSG_DATA," Error in ArrayListgetSuccessTN_having_valid_tnWithSubdomain");
			}
	    }
	    return tn_list;
    } 
    
    /**
     * 
     * Update the FailedTNlist, add the TN in Context.FailedTNList.
     * 
     */
    
    public ArrayList getSuccessTN_having_valid_ALTSPID(ArrayList tn_list, ArrayList validTn_list)
    {
    	 
    	try
    	{
    		if(validTn_list != null)
    		{
		    	for( int i=0;i< validTn_list.size();i++)
		    	{
		    		String validTN =  ((CreateTnAltSpidData)validTn_list.get(i)).portingTn;
				
		    		int tnIndex = tn_list.indexOf(validTN);
					
					if (tnIndex != -1)
					{
						tn_list.remove(tnIndex);
					}
		    	}
    		}
    	}	
	    catch(Exception e)
	    {
	    	if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
				Debug.log(Debug.MSG_DATA," Error in ArrayListgetSuccessTN_having_valid_ALTSPID");
			}
	    }
	    return tn_list;
    } 
    
    /**
     * Utility method for cleaning up a statement and returning a connection
     * to the pool. This method takes care of catching and logging any
     * exceptions.
     *
     * @param statement PreparedStatement this statement will be closed, if not
     *                                    null.
     * @param connection Connection this connection will be returned to the
     *                              connection pool, if not null.
     */
    public static void closeResultSetAndStatement(ResultSet rs,
            					   PreparedStatement statement){
        
       if (rs != null){
           
           try{
               
               rs.close();
              
           }catch(SQLException sqlex){
               
               Debug.error("Could not close result set: "+sqlex);
           }
           
       }
       
       if( statement != null ){

          try{
            
              statement.close();
              
          }catch(SQLException sqlex){
             
              Debug.error("Could not close statement: "+sqlex);
          }

       }     

    }
    /**
     * TnOIDTransOID class is used to encapsulate the Tn , OID and TransOID info in objects.
     * @author msgupta
     *
     */
    private static class TnOIDTransOID {

    	public final String tn;
        public final String oid;
        public final String transOid;
        public TnOIDTransOID(String tn, String oid , String transOid) throws FrameworkException {

            this.tn = tn;
            this.oid = oid;
            this.transOid = transOid;

        }

    }
	
    
	//--------------------------For Testing---------------------------------//

	public static void main(String[] args) {

		Properties props = new Properties();

		props.put( "DEBUG_LOG_LEVELS", "ALL" );

		props.put( "LOG_FILE", "E:\\logmap.txt" );

		Debug.showLevels( );

		Debug.configureFromProperties( props );

		if (args.length != 3)
		{

			 Debug.log (Debug.ALL_ERRORS, "TNRangeLogger: USAGE:  "+
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


		TNRangeLogger tnRangeLogger = new TNRangeLogger();

		try
		{
			tnRangeLogger.initialize("FULL_NEUSTAR_SOA","TNRangeLogger");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("StartTN","4444");
			mob.set("EndTN","4444");
			mob.set("NPA","555");
			mob.set("NXX","123");
			mob.set("SPID","1234");
			mob.set("ONSP","1234");
			mob.set("NNSP","4567");
			mob.set("LNPTYPE","12");
			//mob.set("SVID","3000");
			mob.set("CREATEDBY","Jagan");

			mob.set("CREATEDDATE", "SYSDATE");

			tnRangeLogger.process(mpx,mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		}
		catch(ProcessingException pex)
		{

			Debug.log(Debug.ALL_ERRORS, "Processing Exception: " + pex.getMessage());

		}
		catch(MessageException mex)
		{

			Debug.log(Debug.ALL_ERRORS, "Message Exception: " + mex.getMessage());

		}
	} //end of main method

}