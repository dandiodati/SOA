/**
 * This class contains constant values used by BDD ,Mass SPID update,
 * NPA Split , SV Management
 *
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date			Reason
	-----       -----------     ----------		--------------------------
	1			Ashok			04/12/2004		Created
	2			Ashok			05/05/2004		Added for Mass spid Update
    3           Jigar			06/16/2004		Added constants for KeyContainer
												and Key
	4			Ravi.M			06/16/2004		Added Common Constants for Full
												SOA Component.
	5           Jagan			06/18/2004		Added constants for Full SOA
												Component.
	6			Ashok			06/18/2004		Added constants for
												MapSOANotification component.
	7			Ashok			06/24/2004		Constants added for NPA Split
	8			Jigar			06/30/2004		Added constants for SYSDATE & DATE
	9			Phani 			07/05/2004      Added constants for PDPStartDate
												and PDPEnddate
	10			Abhijit			07/29/2004		Formal review comments incorporated.
	11          Jigar			10/20/2004		Added constants for ValidateModifySv
	12			Sreedhar		03/28/2005		Added Constants for Billing requirements
	
	13			Devaraj			04/22/2005		Added constants for TN indexes.
	14          D.Subbarao	    30/08/2005      Added constants for Notification BDD. 
	15          D.Subbarao	    09/02/2005      Added constants for Notification BDD. 
	16			D.Subbarao		09/05/2005		Added constants for Notification BDD. 
	17			D.Subbarao		09/14/2005		Added constants for GetAndSetMaxTN process.
	18			Jigar			10/06/2005		Added constants for SV_MODIFY_REQUEST_CANCEL_PENDING
	19			D.Subbarao		10/10/2005	    Added constants for Notification BDD.
	20			D.Subbarao		10/17/2005		Added Constants for GetAndSetMaxTN component.
	21			D.Subbarao		10/19/2005		Modified and Added constants.
	22			D.Subbarao		10/20/2005		Added a new constant.
	23			D.Subbarao		10/28/2005		Added and modified some constants.
	24			D.Subbarao		11/24/2005		Eleminated unused constants.
	25			Jigar Talati	11/28/2005		Added CAUSE CODE constants. 
	26			D.Subbarao		11/30/2005		Modified a constant and added two new constants.
	27			D.Subbarao		12/13/2005		Added some constants for GetAndSetMaxTN and NodeCount
												components.
    28			D.Subbarao		12/14/2005		Added a new constant, eleminated two unused constants.
	29			D.Subbarao		02/16/2006		Added some new constants for BDD.
	30			D.Subbarao		02/24/2006		Added a new token constant.
	31			Jigar Talati	04/14/2006		Added SPTYPE constants
	32			Manoj Kumar		06/15/2006		Added SVTYPE constants
	33			Manoj Kumar		07/31/2006		Added SOA_NPANXX_LRN constant.
	34			Abhijit			07/31/2006		Added constants for SOAAccountSVUpdate
												component.
	35          Manoj kumar		01/16/2007		Added MSG_KEY_PROP constant for messagekey for MapLogger.java
	36			Manoj Kumar		02/06/2007		Added the new constants.
	
	37			Peeyush M		05/11/2007		Added new Constants for SubDomain requirement.
 */

package com.nightfire.spi.neustar_soa.utils;

public interface SOAConstants {

	/**
	* The name of the Subscription Version table.
	*/
	public static final String SV_TABLE = "SOA_SUBSCRIPTION_VERSION";

	/**
	 * The name of the Subscription Version temp table.
	 */
	public static final String SV_TEMP_TABLE = "SOA_SUBSCRIPTION_VERSION_TEMP";

	/**
	* The name of the LRN table.
	*/
	public static final String LRN_TABLE = "SOA_LRN";

	/**
	 * The name of the NPA NXX table.
	 */
	public static final String NPANXX_TABLE = "SOA_NPANXX";

	/**
	 * The name of the NPA NXX-X table.
	 */
	public static final String NPANXXX_TABLE = "SOA_NPANXX_X";

	/**
	 * The name of the SPID table.
	 */
	public static final String SPID_TABLE = "SOA_SPID";

	/**
	* The name of the Number Pool Block table.
	*/
	public static final String NBRPOOL_BLOCK_TABLE = "SOA_NBRPOOL_BLOCK";

	/**
	 * The name of the CUSTOMER_LOOKUP table.
	 */
	public static final String CUSTOMER_LOOKUP_TABLE = "CUSTOMER_LOOKUP";

	/**
	 * The name of the LAST_NPAC_RESPONSE_TIME table.
	 */
	public static final String LAST_NPAC_RESPONSE_TIME_TABLE =
												"LAST_NPAC_RESPONSE_TIME";

	/**
	 * The name of the SOA_NPA_SPLIT Master table.
	 */
	public static final String NPA_SPLIT_TABLE = "SOA_NPA_SPLIT";

	/**
	 * The name of the SOA_NPA_SPLIT_NXX  table.
	 */
	public static final String NPA_SPLIT_NXX_TABLE = "SOA_NPA_SPLIT_NXX";

	/**
	 * The name of the SOA_SV_FAILEDSPLIST  table.
	 */
	public static final String SOA_SV_FAILEDSPLIST_TABLE = "SOA_SV_FAILEDSPLIST";

	public static final String REQUEST_KEY_PROP = "REQUEST_TYPE_LOCATION";

	public static final String ACCOUNT_ID = "ACCOUNT_ID_LOCATION";

	public static final String ACCOUNT_NAME = "ACCOUNT_NAME_LOCATION";

        public static final String TN_LIST = "TN_LOCATION";     //list of Tn separated by semicolon
        
        public static final String SVID_LIST = "SVID_LOCATION"; // list of SVID separated by semicolon
        
        public static final String REGION_ID = "REGION_ID_LOCATION";
        
        public static final String SPID = "SPID_LOCATION";         //SPID
        
        public static final String NNSP = "NNSP_LOCATION";              //NNSP
        
        public static final String ONSP = "ONSP_LOCATION";              //ONSP
        
        public static final String INPUT_SOURCE = "INPUT_SOURCE_LOCATION";  //Either A (API) or G (GUI)      

	public static final String ACCOUNT_ADD_SV_REQUEST = "AccountAddSvRequest";

	public static final String ACCOUNT_REMOVE_SV_REQUEST = "AccountRemoveSvRequest";

	public static final String SERVICE_TYPE_PROP = "SERVICE_TYPE";

	public static final String PARTIAL_FAILURE_PROP = "PARTIAL_FAILURE_LOCATION";

	public static final String PARTIAL_FAILURE = "partialfailure";

	public static final String SOA_ADMIN = "SOAADMIN";

	public static final String SOA = "SOA";

	public static final String GUI_REQUEST = "G";
	

	/**
	 * Property prefix for Message key value.
	 */
	public static final String MSG_KEY_PROP = "MESSAGE_KEY";

	public static final String LRN_COL = "LRN";

	public static final String CREATED_BY_PROP = "CREATED_BY_LOC";

	public static final String SPID_COL = "SPID";

	public static final String REGION_COL = "REGIONID";

	public static final String STATUS_COL = "STATUS";

	public static final String CUSTOMER_ID_PROP = "CUSTOMER_ID_LOC";

	public static final String OBJECT_CREATION_DATE_COL = "OBJECTCREATIONDATE";

	public static final String OK_STATUS = "ok";

	public static final String SYSTEM_USER = "System";

	public static final String NAME_COL = "NAME";

	public static final String DATETIME_COL = "DATETIME";

	public static final String NPA_COL = "NPA";

	public static final String NXX_COL = "NXX";

	public static final String OLDNPA_COL = "OLDNPA";

	public static final String NEWNPA_COL = "NEWNPA";

	public static final String PDPSTARTDATE_COL = "PDPSTARTDATE";

	public static final String PDPENDDATE_COL = "PDPENDDATE";

	public static final String DASHX_COL = "DASHX";

	public static final String EFFECTIVEDATE_COL = "EFFECTIVEDATE";

	public static final String MODIFIEDDATE_COL = "MODIFIEDDATE";

	public static final String LASTRESPONSEDATE_COL = "LASTRESPONSEDATE";

	public static final String FIRSTUSEDSPID_COL = "FIRSTUSEDSPID";

	public static final String NPANXXID_COL = "NPANXXID";

	public static final String NPANXXXID_COL = "NPANXX_X_ID";

	public static final String ACTIVEFLAG_COL = "ACTIVEFLAG";

	public static final int NPANXXX_ACTIVEFLAG = 1;

	public static final int NPANXXX_ACTIVEFLAG_DEF = 0;

	public static final int SOA_ORIGINATION = 0;

	public static final String SOA_REQUEST_MESSAGE_TYPE = "soarequest";

	public static final String SOA_ERROR_COUNT = "0";

	public static final String NBR_STATUS = "active";

	public static final String CUSTOMERID_COL = "CUSTOMERID";
	
	public static final String SUBDOMAIN_COL = "SUBDOMAINID";

	public static final String TPID_COL = "TPID";

	public static final String ONSPDUEDATE_COL = "ONSPDUEDATE";

	public static final String CAUSECODE_COL = "CAUSECODE";

	public static final String AUTHORIZATIONFLAG_COL = "AUTHORIZATIONFLAG";

	public static final String LASTREQUESTTYPE_COL = "LASTREQUESTTYPE";

	public static final String CUSTOMERDISCONNECTDATE_COL = "CUSTOMERDISCONNECTDATE";
	
	public static final String EFFECTIVERELEASEDATE_COL = "EFFECTIVERELEASEDATE";
	
	public static final boolean SV_BDD = true;

	public static final boolean NBR_BDD = false;

	public static final String FAILED_SP_FLAG_COL = "FAILEDSPFLAG";

	public static final String FAILED_SP_LIST_COL = "FAILEDSPLIST";

	public static final String OBJECTTYPE_COL = "OBJECTTYPE";

	public static final String ID_COL = "ID";

	public static final String PORTINGTN_COL = "PORTINGTN";
	
	public static final String LASTUPDATE_COL = "LASTUPDATE";

	public static final String NNSP_COL = "NNSP";

	public static final String ONSP_COL = "ONSP";

	public static final String SVID_COL = "SVID";

	public static final String CLASSDPC_COL = "CLASSDPC";

	public static final String CLASSSSN_COL = "CLASSSSN";

	public static final String CNAMDPC_COL = "CNAMDPC";

	public static final String CNAMSSN_COL = "CNAMSSN";

	public static final String ISVMDPC_COL = "ISVMDPC";

	public static final String ISVMSSN_COL = "ISVMSSN";

	public static final String LIDBDPC_COL = "LIDBDPC";

	public static final String LIDBSSN_COL = "LIDBSSN";

	public static final String WSMSCDPC_COL = "WSMSCDPC";

	public static final String WSMSCSSN_COL = "WSMSCSSN";

	public static final String LRN_FILE_TYPE = "SIC-SMURF-LRN";

	public static final String NPA_NXX_FILE_TYPE = "SIC-SMURF-NPANXX";

	public static final String NPA_NXX_X_FILE_TYPE = "SIC-SMURF-NPANXXX";

	public static final String LRN_BDD_FILE_TYPE = "LRN-BDD";

	public static final String SPID_BDD_FILE_TYPE = "SPID-BDD";

	public static final String NPA_NXX_BDD_FILE_TYPE = "NPA-NXX-BDD";

	public static final String NPA_NXX_X_BDD_FILE_TYPE = "NPA-NXX-X-BDD";

	public static final String NPB_BDD_FILE_TYPE = "NPB-BDD";

	public static final String SV_BDD_FILE_TYPE = "SV-BDD";

	public static final String KEY_ROOT = "ReferenceKey";

	public static final String KEYCONTAINER_NODE = "keycontainer";

	public static final String KEY_NODE = "key";

	public static final int TN_LINE = 4;

	public static final String REFERENCEKEY_COL = "REFERENCEKEY";

	public static final String PORTINGTOORIGINAL_COL = "PORTINGTOORIGINAL";

	public static final String LASTMESSAGE_COL = "LASTMESSAGE";

	public static final String REQUESTTYPE_COL = "REQUESTTYPE";

	public static final String CREATEDDATE_COL = "CREATEDDATE";	

	public static final String OLD_STATUS = "old";

	public static final String CANCELED_STATUS = "canceled";

	public static final String ACTIVE_STATUS = "active";

	public static final String SENDING_STATUS = "sending";

	public static final String PENDING_STATUS = "pending";

	public static final String CANCEL_PENDING_STATUS = "cancel-pending";

	public static final String DISCONNECT_PENDING_STATUS = "disconnect-pending";

	public static final String DOWNLOAD_FAILED_PARTIAL_STATUS =
													"download-failed-partial";

	public static final String DOWNLOAD_FAILED_STATUS = "download-failed";

	public static final String CONFLICT_STATUS = "conflict";

	public static final String DOWNLOAD_REASON_NEW = "new";

	public static final String SV_ACTIVATE_REQUEST = "SvActivateRequest";

	public static final String SV_CREATE_REQUEST = "SvCreateRequest";

	public static final String AUDIT_CREATE_REQUEST = "AuditCreateRequest";

	public static final String AUDIT_CANCEL_REQUEST = "AuditCancelRequest";

	public static final String AUDIT_QUERY_REQUEST = "AuditQueryRequest";

	public static final String GTT_CREATE_REQUEST = "GTTCreateRequest";

	public static final String GTT_DELETE_REQUEST = "GTTDeleteRequest";

	public static final String GTT_MODIFY_REQUEST = "GTTModifyRequest";

	public static final String SV_DISCONNECT_REQUEST = "SvDisconnectRequest";

	public static final String SV_RELEASE_REQUEST = "SvReleaseRequest";

	public static final String SV_RELEASE_IN_CONFLICT_REQUEST =
												"SvReleaseInConflictRequest";

	public static final String SV_CANCEL_REQUEST = "SvCancelRequest";

	public static final String SV_CANCEL_AS_NEW_REQUEST = "SvCancelAckAsNewRequest";

	public static final String SV_CANCEL_AS_OLD_REQUEST = "SvCancelAckAsOldRequest";

	public static final String SV_MODIFY_REQUEST = "SvModifyRequest";

	public static final String SV_QUERY_REQUEST = "SvQueryRequest";

	public static final String SV_REMOVE_FROM_CONFLICT_REQUEST =
                                  "SvRemoveFromConflictRequest";

	public static final String VERSION_OBJECT_CREATION_NOTIFICATION =
										"VersionObjectCreationNotification";

	public static final String VERSION_STS_ATR_VAL_CHAN_NOTIFICATION =
							"VersionStatusAttributeValueChangeNotification";

	public static final String VERSION_ATR_VAL_CHAN_NOTIFICATION =
									"VersionAttributeValueChangeNotification";

	public static final String SV_CREATE_NOTIFICATION = "SvCreateNotification";

	public static final String SV_CREATE_ACK_NOTIFICATION =
													"SvCreateAckNotification";

	public static final String SV_RELEASE_NOTIFICATION =
													"SvReleaseNotification";

	public static final String SV_RELEASE_ACK_NOTIFICATION =
													"SvReleaseAckNotification";

	public static final String SV_RELEASE_CONF_NOTIFICATION =
											"SvReleaseInConflictNotification";

	public static final String SV_RELEASE_CONF_ACK_NOTIFICATION =
										"SvReleaseInConflictAckNotification";

	public static final String SV_ACTIVATE_NOTIFICATION =
										"SvActivateNotification";

	public static final String SV_STS_CHANGE_NOTIFICATION =
												"SvStatusChangeNotification";

	public static final String SV_ATR_CHANGE_NOTIFICATION =
												"SvAttributeChangeNotification";

	public static final String SV_DISCONNECT_NOTIFICATION =
												"SvDisconnectNotification";

	public static final String SV_PTO_NOTIFICATION =
												"SvPortToOriginalNotification";

	public static final String SV_CANCEL_NOTIFICATION = "SvCancelNotification";

	public static final String SV_CANCEL_ACK_NOTIFICATION =
											"SvCancelAckRequestNotification";

	/**
	 * Token indicating that current date/time should be used for date
	 * field values.
	 */
	public static final String SYSDATE = "SYSDATE";

	/**
	 * Property indicating the DateTimeSent node in the generated XML document,
	 * if a new document is being created.
	 */
	public static final String DATE_FORMAT = "MM-dd-yyyy-hhmmssa";

	public static final String OLDTN = "OLDTN";

	public static final String NEWTN = "NEWTN";

	public static final String NPAC_QUEUE_TABLE = "NPAC_QUEUE";

	/**
	* The name of the MESSAGEKEY column.
	*/
	public static final String MESSAGEKEY_COL = "MESSAGEKEY";

	/**
	 * Status used in the NPAC queue to indicate a message that has been sent,
	 * but has yet to receive a reply.
	 */
	public static final String SENT_STATUS = "Sent";

	/**
	 * Constant for "Success"
	 */
	public static final String SUCCESS = "success";

	/**
	 * Constant for "Failure"
	 */
	public static final String FAILURE = "failure";

   public static final String ROOT = "SOAMessage";

	public static final String RESPONSE_ROOT = ROOT;

	public static final String SOA_TO_UPSTREAM_PROP = "SOAToUpstream";
	
	public static final String SOA_TO_UPSTREAM_HEADER_PATH =
			SOA_TO_UPSTREAM_PROP + ".SOAToUpstreamHeader";
	
	public static final String DATESENT =
			SOA_TO_UPSTREAM_HEADER_PATH + ".DateSent";

	public static final String SOA_TO_UPSTREAM_BODY_PATH =
			SOA_TO_UPSTREAM_PROP + ".SOAToUpstreamBody";

	public static final String SV_QUERY_REPLY_PATH =
			SOA_TO_UPSTREAM_BODY_PATH + ".SvQueryReply";

	public static final String QUERY_STATUS_PATH =
	SV_QUERY_REPLY_PATH + ".QueryStatus";

	public static final String SOA_REQUEST_STATUS_PATH =
						QUERY_STATUS_PATH + ".SOARequestStatus";

	public static final String SV_LIST_PATH =
										SV_QUERY_REPLY_PATH + ".SvList";

	public static final String SV_DATA_PATH =
										SV_LIST_PATH + ".SvData";

	public static final String GTTDATA_NODE = ".GTTData";

	public static final String DOWNLOAD_REASON_NODE = ".DownloadReason";

	public static final String SV_OBJECT_TYPE = "SV";

	public static final String NBR_OBJECT_TYPE = "NBR";

	/**
	 * Default TN delimeter.
	 */
	public static final String DEFAULT_DELIMITER = "-";

	/**
     * Property indicating name of the database table to insert row into.
     */
    public static final String TABLE_NAME_PROP = "TABLE_NAME";

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
     * Property prefix giving name of column.
     */
    public static final String COLUMN_PREFIX_PROP = "COLUMN";

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
     * Property prefix indicating whether column value is optional or required.
     */
    public static final String OPTIONAL_PREFIX_PROP = "OPTIONAL";

	/**
     * Property prefix indicating whether column to include in update statement.
     */
    public static final String UPDATE_PREFIX_PROP = "UPDATE";

	/**
     * Property prefix giving default value for column.
     */
    public static final String DEFAULT_PREFIX_PROP = "DEFAULT";

    /**
	 * Types of supported columns that require special processing.
	 */
    public static final String COLUMN_TYPE_DATE        = "DATE";

    public static final String COLUMN_TYPE_TEXT_BLOB   = "TEXT_BLOB";

    public static final String COLUMN_TYPE_BINARY_BLOB = "BINARY_BLOB";

    /**
	 * Token value for LOCATION indicating that the entire contents of the
     * message-processor's input message-object should be used as the
	 * column value.
	 */
    public static final String PROCESSOR_INPUT = "PROCESSOR_INPUT";

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
	 * Property indicating the location of SPID
	 */
	public static final String SPID_PROP = "SPID";

	/**
	 * Property indicating the location of ONSP
	 */
	public static final String ONSP_PROP = "ONSP";

	/**
	 * Property indicating the location of NNSP
	 */
	public static final String NNSP_PROP = "NNSP";

	/**
	 * Property indicating the location of SVID
	 */
	public static final String SVID_PROP = "SVID";

	/**
	 * Property prefix giving location of Delimiter.
	 */
	public static final String TN_DELIMITER_PROP = "TN_DELIMITER";

	/**
     * Property indicating the location seperator for multiple values.
     */
	public static final String DEFAULT_LOCATION_SEPARATOR = "|";

	/**
     * Property indicating whether customer criteria should be added or not to
     * the SQL statement. If not set, default value is true.
     */
    public static final String USE_CUSTOMER_ID_PROP = "USE_CUSTOMER_ID";

	/**
	 * Property indicating the location of start SVID
	 */
	public static final String START_SVID_PROP = "START_SVID";

	/**
	 * Property indicating the location of end SVID
	 */
	public static final String END_SVID_PROP = "END_SVID";

	/**
	 * Property indicating the output location of REFERENCE Key XML
	 */
	public static final String REFERENCEKEY_OUT_LOC_PROP = "REFERENCEKEY_OUT_LOC";

	/**
	 * Property indicating for SQL where statement.
	 */
	public static final String SQL_WHERE_STATEMENT_PROP
										= "SQL_WHERE_STATEMENT";

	/**
	 * Property indicating the output location of Range REFERENCE Key XML
	 * 
	 */	
	public static final String INPUT_LOC_RANGE_REFERENCE_KEY_MESSAGE_PROP 
										= "RANGE_REFERENCE_KEY_MESSAGE";
	
	public static final String RANGE_REFERENCEKEY_OUT_LOC_PROP 
										= "RANGE_REFERENCEKEY_OUT_LOC";


	/**
	 * Property prefix giving column data type.
	 */
	public static final String WHERE_COLUMN_TYPE_PREFIX_PROP =
		"WHERE_COLUMN_TYPE";

	/**
	 * Property prefix giving date format for date types.
	 */
	public static final String WHERE_DATE_FORMAT_PREFIX_PROP =
		"WHERE_DATE_FORMAT";

	/**
	 * Property prefix giving location of column value.
	 */
	public static final String WHERE_LOCATION_PREFIX_PROP = "WHERE_LOCATION";

	/**
	 * Property prefix giving location of default value.
	 */
	public static final String WHERE_DEFAULT_PREFIX_PROP = "WHERE_DEFAULT";

	/**
	 * Property prefix giving location of input data for TN.
	 */
	public static final String INPUT_LOC_TN_PROP = "INPUT_TN_LOC";

	/**
	 * Property prefix giving location of input data for SVID.
	 */
	public static final String INPUT_LOC_SVID_PROP = "INPUT_SVID_LOC";

	/**
	 * 	Property prefix giving location of input data for EndStation.
	 */
	public static final String INPUT_LOC_END_STATION_PROP =
		"INPUT_END_STATION_LOC";

	/**
	 * Property indicating the location of RequestType
	 */
	public static final String REQUEST_TYPE_PROP = "REQUEST_TYPE";

	/**
	 * <code>OUTPUT_FORMAT_PROP</code> takes the static final
	 * String value "OUTPUT_FORMAT". This required property gives
	 * the format of the dates in the output message.
	 *
	 */
	public static String OUTPUT_FORMAT_PROP = "OUTPUT_FORMAT";

	/**
	 * <code>INPUT_LOC_PREFIX_PROP</code> takes the static final
	 * String value "INPUT_LOC". This required property gives
	 * the locations of the dates in the input message that will
	 * be converted.
	 *
	 */
	public static final String INPUT_LOC_PREFIX_PROP = "INPUT_LOC";

	/**
	 * <code>OUTPUT_LOC_PREFIX_PROP</code> takes the static final
	 * String value "OUTPUT_LOC". This required property gives
	 * the locations to which the dates will be written in the
	 * output message.
	 *
	 */
	public static final String OUTPUT_LOC_PREFIX_PROP = "OUTPUT_LOC";

	/**
     * Property prefix giving column data type.
     */
    public static final String COLUMN_NAME_PREFIX_PROP = "COLUMN_NAME";

	/**
	 * Property indicating the location of TN
	 */
	public static final String TN_LOCATION = "@context.CurrentTN";

	/**
	 * Property indicating the Database having row
	 */
	public static final String ROW_EXIST = "@context.RowExist";

	/**
	 * Property indicating the location of TN
	 */
	public static final String API_FLAG = "@context.REQUEST_HEADER.InputSource";
	
	/**
	 * Property indicating the location of BR Error Status
	 */
	
	public static final String BR_ERROR_STATUS = "@context.BRErrorStatus";

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
	 * The location in the context to put the data validation errors
	 */
	public static final String ERROR_RESULT_LOCATION_PROP = "ERROR_RESULT_LOCATION";

	public static final int  PORTED_TN_LENGTH = 17;

	public static final String LOCAL_TIMEZONE = "LOCAL";

	/**
	 * Constant for Region Id length
	 */
	public static final int REGION_ID_LEN = 4;

	/**
	 * Property giving the SQL SELECT statement to execute.
	 */
	public static final String SQL_QUERY_STATEMENT_PROP
														= "SQL_QUERY_STATEMENT";

	/**
	 * The location in the context to put success or failure of Data Validation
	 */
	public static String IS_VALID_PROP = "IS_VALID";

	/**
	 * Property indicating whether to go for TN Range Looping or not.
	 */
	public static final String RANGE_TN_LOOPING_PROP = "RANGE_TN_LOOPING";

	/**
	 * Property indicating the location of start START_ID
	 */
	public static final String START_ID_PROP = "START_ID";

	/**
	 * Property indicating the location of start END_ID
	 */
	public static final String END_ID_PROP = "END_ID";

	/**
	 * Property indicating the location of FailedSPFlag.
	 */
	public static final String FAILEDSPLIST_FLAG_PROP = "FAILEDSPLIST_FLAG";

	/**
	 * Property indicating the location of ID.
	 */
	public static final String ID_LOCATION = "@context.ID";

	/**
	 * Property indicating the Total TN.
	 */
	public static final String TOTAL_TN_LOCATION = "@context.TotalTns";

	/**
	 * Property prefix giving RequestId value.
	 */
	public static final String REQUEST_ID_PROP = "REQUESTID";

	/**
	 * Property prefix giving RegoinId value.
	 */
	public static final String REGION_ID_PROP = "REGIONID";

	/**
	 * Property prefix giving Action value.
	 */
	public static final String ACTION_VALUE_PROP = "ACTION";

	/**
	 * Property prefix giving Message Status Location
	 */
	public static final String MESSAGE_STATUS_PROP = "MESSAGE_STATUS";

	/**
	 * Property prefix giving Synchronous Response Status Location.
	 */
	public static final String SYNC_RESPONSE_STATUS_PROP
										= "SYNC_RESPONSE_STATUS";

	 /**
	 * Property prefix giving output Location of Sync resp.
	 */
	public static final String SYNC_RESPONSE_OUT_LOC_PROP
										= "SYNC_RESPONSE_OUT_LOC";
	
	/**
	 * Property prefix giving output Location of Sync key.
	 */
	public static final String SYNC_KEY_LOC_PROP
										= "SYNC_RESPONSE_KEY_LOC";

	/**
	 * Property prefix giving output Location of complete failure flag.
	 */
	public static final String COMPLETEFAIL_LOC_PROP="COMPLETE_FAIL_LOC";

	/**
	 * Property indicating the RegionID in the genarated XML document,
	 * if a new document is being created.
	 */
	public static final String REGIONID =
		SOA_TO_UPSTREAM_HEADER_PATH + ".RegionId";


	/**
	 * Property indicating the SynchronousResponse  in  the generated
	 * XML document, if a new document is being created.
	 */
	public static final String SYNCHRONUS_RESPONSE_PATH =
		SOA_TO_UPSTREAM_BODY_PATH + ".SynchronousResponse";

	/**
	 * Property indicating the Action  in  the generated XML document,
	 * if a new document is being created.
	 */
	public static final String ACTION_VALUE_PATH =
		SYNCHRONUS_RESPONSE_PATH + ".Action";

	/**
	 * Property indicating the RequestId in the generated XML document,
	 * if a new document is being created.
	 */
	public static final String REQUEST_ID_PATH =
		SYNCHRONUS_RESPONSE_PATH + ".RequestId";

	/**
	 * Property indicating the RequestStatus  in  the generated XML document,
	 * if a new document is being created.
	 */
	public static final String REQUEST_STATUS_PATH =
			SYNCHRONUS_RESPONSE_PATH + ".RequestStatus";

	/**
	 * Property indicating location of Input message.
	 */
	public static final String INPUT_MSG_LOC = "INPUT_MESSAGE";
	/**
	 * Property indicating location of Output message.
	 */
	public static final String OUTPUT_MSG_LOC = "OUTPUT_MESSAGE";

	/**
	 * Property prefix giving location of input XML data.
	 */
	public static final String INPUT_LOC_REFERENCE_KEY_MESSAGE_PROP
											= "REFERENCE_KEY_MESSAGE";

	/**
	 * Property prefix giving location of Reference  Key.
	 */
	public static final String OUTPUT_LOC_REFERENCE_KEY_PROP
											= "@context.ReferenceKey";

	/**
	 * Property indicating location of input XML message.
	 */
	public static final String INPUT_XML_MESSAGE_LOC_PROP
													= "INPUT_XML_MESSAGE";

	/**
	 * Property indicating location of Creation Time stamp.
	 */
	public static final String CREATION_TS_LOC_PROP = "CREATION_TIMESTAMP";

	/**
	 * Property indicating location of input NewSP.
	 */
	public static final String INPUT_NEWSP_LOC_PROP = "INPUT_NEWSP";

	/**
	 * Property indicating location of input OldSP.
	 */
	public static final String INPUT_OLDSP_LOC_PROP = "INPUT_OLDSP";

	/**
	 * Property indicating location of Status.
	 */
	public static final String STATUS_LOC_PROP = "STATUS";


	/**
	 * Property indicating location of Old Authorization flag.
	 */
	public static final String OLD_AUTHORIZATION_FLAG_LOC_PROP
												= "OLD_AUTHORIZATION_FLAG";

	/**
	 * Property indicating location of failed sps flag.
	 */
	public static final String FAILED_SP_FLAG_LOC_PROP
													= "FAILED_SP_FLAG";

	/**
	 * Property indicating location of output date format.
	 */
	public static final String OUTPUT_DATE_FORMAT_LOC_PROP
													= "OUTPUT_DATE_FORMAT";

	/**
	 * Property indicating location of Input TN.
	 */
	public static final String INPUT_TN_LOC_PROP = "INPUT_TN";

	/**
	 * Property indicating location of Output XML Message.
	 */
	public static final String OUTPUT_XML_MESSAGE_LOC_PROP
												= "OUTPUT_XML_MESSAGE";
	/**
	 * Property indicating location of Npac Notification.
	 */
	public static final String NPAC_NOTIFICATION_LOC_PROP
													= "NPAC_NOTIFICATION";
	/**
	 * Property indicating location of Output TN.
	 */
	public static final String OUTPUT_TN_LOC_PROP = "OUTPUT_TN";

	/**
	 * Property indicating location of Output EndStation.
	 */
	public static final String OUTPUT_ENDSTATION_LOC_PROP
													= "OUTPUT_ENDSTATION";

	/**
	 * Property indicating location of output NewSP.
	 */
	public static final String OUTPUT_NEWSP_LOC_PROP = "OUTPUT_NEWSP";

	/**
	 * Property indicating location of output OldSP.
	 */
	public static final String OUTPUT_OLDSP_LOC_PROP = "OUTPUT_OLDSP";

	/**
	 * Property indicating location of output couse code.
	 */
	public static final String OUTPUT_CAUSECODE_LOC_PROP = "OUTPUT_CAUSECODE";

	/**
	 * Property indicating location of output old sp due date.
	 */
	public static final String OUTPUT_DUEDATE_LOC_PROP = "OUTPUT_DUEDATE";

	/**
	 * The property prefix giving location of NPA
	 */
	public static final String INPUT_LOC_NPA_PROP = "INPUT_LOC_NPA";

	/**
	 * The property prefix giving location of NXX
	 */
	public static final String INPUT_LOC_NXX_PROP = "INPUT_LOC_NXX";

	/**
	 * The property prefix giving location of DASHX
	 */
	public static final String INPUT_LOC_DASHX_PROP = "INPUT_LOC_DASHX";

	/**
	 * The property prefix giving loation of NPA_NXX_X
	 */
	public static final String OUTPUT_LOC_NPA_NXX_X_PROP = "OUTPUT_LOC_NPANXXX";

	/**
	*The property prefix giving loation of NPA_NXX_X
	*/

	public static final String INPUT_LOC_NPA_NXX_X_PROP = "NPA_NXX_X";

	/**
	*The property prefix giving location of NPA
	*/

	public static final String OUTPUT_LOC_NPA_PROP = "OUTPUT_LOC_NPA";

	/**
	*The property prefix giving location of NXX
	*/

	public static final String OUTPUT_LOC_NXX_PROP = "OUTPUT_LOC_NXX";

	/**
	*The property prefix giving location of DASHX
	*/

	public static final String OUTPUT_LOC_DASHX_PROP = "OUTPUT_LOC_DASHX";

	/**
	 * Property indicating location of Old NPA.
	 */
	public static final String OLD_NPA_LOC_PROP = "OLD_NPA";
	/**
	 * Property indicating location of New NPA.
	 */
	public static final String NEW_NPA_LOC_PROP = "NEW_NPA";

	/**
	 * Property indicating name of Query Request.
	 */
	public static final String QUERY_REQUEST_TYPE_PREFIX_PROP
												= "QUERY_REQUEST_TYPE";

	/**
	 * Property prefix giving start location of column's start value.
	 */
	public static final String START_LOCATION_PREFIX_PROP = "START_LOCATION";

	/**
	 * Property prefix giving end location of column's end value.
	 */
	public static final String END_LOCATION_PREFIX_PROP = "END_LOCATION";

	/**
	 * Property prefix giving node name.
	 */
	public static final String NODE_NAME_PREFIX_PROP = "NODE_NAME";

	/**
	 * Property prefix giving location of output query reply XML.
	 */
	public static final String OUTPUT_XML_PREFIX_PROP = "OUTPUT_XML";

	/**
	* <code>INPUT_TIME_ZONE_PROP</code> takes the static final String
	* value "INPUT_TIME_ZONE". This required property gives the time
	* zone of the times in the input message. The time zone values
	* are those supported by the java.util.TimeZone class as given by
	* the static method java.util.TimeZone.getAvailableIDs(). Setting
	* the Value to "LOCAL" will give the local time zone.
	*
	*/
	public static String INPUT_TIME_ZONE_PROP = "INPUT_TIME_ZONE";

	/**
	* <code>OUTPUT_TIME_ZONE_PROP</code> takes the static final
	* String value "OUTPUT_TIME_ZONE". This required property gives
	* the time zone of the times in the output message.
	*
	*/
	public static String OUTPUT_TIME_ZONE_PROP = "OUTPUT_TIME_ZONE";

	/**
	* <code>INPUT_FORMAT_PROP</code> takes the static final
	* String value "INPUT_FORMAT". This required property gives
	* the format that will be used to parse the times in the
	* input message. The time formats are those supported by
	* the java.text.SimpleDateFormat class.
	*
	*/
	public static String INPUT_FORMAT_PROP = "INPUT_FORMAT";

	/*
	 * Private Static Strings
	 */
	public static String OUTPUT_ROOT_NODE_PROP = "OUTPUT_ROOT_NODE";

	public static String INPUT_MESSAGE_LOCATION_PROP =	"INPUT_MESSAGE_LOCATION";

	public static String OUTPUT_BASE_NODE_PROP = "OUTPUT_BASE_NODE";

	public static String BATCH_BASE_NODE_PROP = "BATCH_BASE_NODE";

	/**
	 * This Property indicates whether the node is inside a
	 * container node and the processing is then handled accordingly.
	 */
	public static final String CONTAINER_CHILDNODE_INDICATOR = "(*)";

	/**
	 * The length of the CONTAINER_CHILDNODE_INDICATOR variable.
	 */
	public static final int CHILDNODE_INDICATOR_LENGTH =
		CONTAINER_CHILDNODE_INDICATOR.length();

	/**
	 * The Round brackets used to access the child nodes.
	 * For eg: in xml
	 * <Container type="container">
	 *    <ActivityDetail>
	 *      <Datetime = "04-20-2004-0500PM">
	 *    </ActivityDetail>
	 *    <ActivityDetail>
	 *      <Datetime = "04-20-2004-0500PM">
	 *    </ActivityDetail>
	 * </Container>
	 * The Datetime node is accessed as Container.ActivityDetail(0).Datetime
	 * and Container.ActivityDetail(1).Datetime.
	 */
	public static final String OPEN_PAREN = "(";

	public static final String CLOSED_PAREN = ")";

	/**
	 * Input format to be used for parsing the date.
	 */
	public static final String INPUT_FORMAT = "MM-dd-yyyy-hhmma";

	/**
	 * Property indicating name of the REFERENCE key sequence.
	 */
	public static final String REFERENCEKEY_SEQ_NAME_PROP
							= "REFERENCEKEY_SEQ_NAME";

	/**
	 * Property indicating the location of REFERENCE Key
	 */
	public static final String REFERENCEKEY_LOCATION = "@context.ReferenceKey";
	
	/**
	 * Property indicating the location of SubDomain Key
	 */
	public static final String SUBDOMAIN_LOCATION = "@context.SubdomainId";

	/**
	 * Property indicating the location of SVID
	 */
	public static final String SVID_LOCATION = "@context.SVID";

	/**
	 *  Property prefix giving location of PortedTn.
	 */
	public static final String OUTPUT_LOC_PORTED_TN_PROP =
		"OUTPUT_LOC_PORTED_TN";

	/**
	 *  Property prefix giving location of NPA_NXX.
	 */
	public static final String OUTPUT_LOC_NPA_NXX_PROP = "OUTPUT_LOC_NPA_NXX";

	/**
	 *  Property prefix giving location of NPA_NXX_X.
	 */
	public static final String OUTPUT_LOC_NPA_NXX_X =
		"OUTPUT_LOC_NPA_NXX_X";

	/**
	 *  Property prefix giving location of StartTn.
	 */
	public static final String OUTPUT_LOC_START_TN_PROP = "OUTPUT_LOC_START_TN";

	/**
	 *  Property prefix giving location of EndTn
	 */
	public static final String OUTPUT_LOC_END_TN_PROP = "OUTPUT_LOC_END_TN";

	/**
	 *  Property prefix giving location of StartPortedTn
	 */
	public static final String OUTPUT_LOC_START_PORTED_TN_PROP
				= "OUTPUT_LOC_START_PORTED_TN";

	/**
	 *  Property prefix giving location of EndPortedTn
	 */
	public static final String OUTPUT_LOC_END_PORTED_TN_PROP
					= "OUTPUT_LOC_END_PORTED_TN";

	/**
	 * Property indicating name of the database table to get status,LastMessage.
	 */
	public static final String OBJECT_TABLE_NAME_PROP = "OBJECT_TABLE_NAME";

	/**
	 * Property indicating name of the database table to check for the validity.
	 */
	public static final String LOOKUP_TABLE_NAME_PROP = "LOOKUP_TABLE_NAME";


	/**
	 * Property indicating location of Input root node.
	 */
	public static final String INPUT_ROOT_NODE_LOC = "INPUT_ROOT_NODE";

	/**
	 * Property indicating location of Output root node.
	 */
	public static final String OUTPUT_ROOT_NODE_LOC = "OUTPUT_ROOT_NODE";

	/**
	 * Property indicating location of Attribute Change Time stamp.
	 */
	public static final String ATTRIBUTECHANGETIMESTAMP_COL = "ATTRIBUTECHANGETIMESTAMP";

	/**
	 * Property indicating location of Status Change Time stamp.
	 */
	public static final String STATUSCHANGETIMESTAMP_COL = "STATUSCHANGETIMESTAMP";

	/**
	 * Property indicate location of notification type.
	 */
	public static final String NPAC_NOTIFICATION_PROP = "NPAC_NOTIFICATION_TYPE";

	/**
	 * Property indicate out of sequence notification type.
	 */
	public static final String OUTOFSYNCTNLIST = "OUTOFSYNCTNLIST";	

	public static final String LASTERRORMESSAGE_COL = "LASTERRORMESSAGE";

	public static final String LSPP = "lspp";

	public static final String LISP = "lisp";

	public static final String POOL = "pool";

	public static final String QUERY_REPLY_TYPE_PREFIX_PROP
													= "QUERY_REPLY_TYPE";

	public static final String SV_QUERY_REPLY = "SvQueryReply";

	public static final String NPA_NXX_QUERY_REPLY = "NpaNxxQueryReply";

	public static final String NPA_NXX_X_QUERY_REPLY = "NpaNxxXQueryReply";

	public static final String LRN_QUERY_REPLY = "LrnQueryReply";

	public static final String NBR_QUERY_REPLY = "NumberPoolBlockQueryReply";

	public static final String QUERY_STATUS = ".QueryStatus";

	public static final String SOA_REQUEST_STATUS = ".SOARequestStatus";

	public static final String NPA_NXX_DATA_PATH =
										SOA_TO_UPSTREAM_BODY_PATH +
										".NpaNxxQueryReply.NpaNxxList.NpaNxxData";

	public static final String NPA_NXX_X_DATA_PATH =
				SOA_TO_UPSTREAM_BODY_PATH +
				".NpaNxxXQueryReply.NpaNxxXList.NpaNxxXData";

	public static final String LRN_DATA_PATH =
					SOA_TO_UPSTREAM_BODY_PATH +
					".LrnQueryReply.LrnList.LrnData";

	public static final String NPB_DATA_PATH =
				SOA_TO_UPSTREAM_BODY_PATH +
				".NumberPoolBlockQueryReply.NumberPoolBlockList.NumberPoolBlockData";

	public static final String NPANXXVALUE_NODE = ".NpaNxxValue";

	public static final String NPA_NXX_DOWNLOAD_REASON_NODE
											= ".NpaNxxDownloadReason";

	public static final String NPA_NXX_X_DOWNLOAD_REASON_NODE
												= ".NpaNxxXDownloadReason";

	public static final String LRN_DOWNLOAD_REASON_NODE
													= ".LrnDownloadReason";

	public static final String NPBID_COL = "NPBID";
        
        public static final String NPBTYPE_COL = "NPBTYPE";

	public static final String NPB_OBJECT_TYPE = "NBR";

	/**
	 * Property indicating Due date check is required or not.
	 */
	public static final String DUE_DATE_CHECK_LOC_PROP
													= "DUE_DATE_CHECK_LOC";

	/**
	 * Property indicating current validation is positive or negative
	 */
	public static final String POSITIVE_VALIDATION_PROP
												= "POSITIVE_VALIDATION";

	/**
	 * Property indicate location of notification type.
	 */
	public static final String NOTIFICATION_PROP = "NOTIFICATION_TYPE";

	/**
	 * Property indicating New SP Due date.
	 */
	public static final String NNSP_DUE_DATE_COL = "NNSPDUEDATE";

	/**
	 * Property indicating Old SP Due date.
	 */
	public static final String ONSP_DUE_DATE_COL = "ONSPDUEDATE";

	/**
	 * Property indicating datatype as number.
	 */
	public static final String NUMBER_TYPE = "3";

	/**
	 * Property indicating datatype as varchar2.
	 */
	public static final String VARCHAR2_TYPE = "12";

	/**
	 * Property prefix indicating whether column required meta data check or not.
	 */
	public static final String METADATACHECK_PREFIX_PROP = "META_DATA_CHECK";

	/**
	 * Property indicating the location into which the result should be placed.
	 */
	public static final String RESULT_LOCATION_PROP = "RESULT_LOCATION";

	/**
	 * Property indicating whether a single row or multiple rows should be returned.
	 */
	public static final String SINGLE_RESULT_FLAG_PROP = "SINGLE_RESULT_FLAG";
	/**
	 * Property prefix giving date format for date types used in query criteria.
	 */
	public static final String INPUT_DATE_FORMAT_PREFIX_PROP = "INPUT_DATE_FORMAT";

	/**
	 * Property indicating the output location of PartialFailure XML
	 */
	public static final String PARTIALFAILURE_ERROR_MESSAGE_PROP = "PARTIALFAILURE_MESSAGE_LOC";

	/**
	 * Name of parent node for one row's-worth of data.
	 */
	public static final String ROW_DELIMITER = "ROW";

	/**
	 * Property indicating the namespace associated with the CustomerID column if any.
	 * If not set, this value is ignored.
	 */
	public static final String CUSTOMER_ID_NAMESPACE_PROP = "CUSTOMER_ID_NAMESPACE";

	public static final String COLUMN_TYPE_NUMBER        = "NUMBER";

	public static final String SOAORIGINATION_COL = "SOAORIGINATION";

	public static final String LASTRESPONSE_NPANXX_X = "NpaNxxXCreateNotification";

	public static final String CLASSDPC_NODE = "ClassDPC";

	public static final String CLASSSSN_NODE = "ClassSSN";

	public static final String CNAMDPC_NODE = "CnamDPC";

	public static final String CNAMSSN_NODE = "CnamSSN";

	public static final String ISVMDPC_NODE = "IsvmDPC";

	public static final String ISVMSSN_NODE = "IsvmSSN";

	public static final String LIDBDPC_NODE = "LidbDPC";

	public static final String LIDBSSN_NODE = "LidbSSN";

	public static final String WSMSCDPC_NODE = "WsmscDPC";

	public static final String WSMSCSSN_NODE = "WsmscSSN";

	public static final String ENDUSERLOCATIONVALUE_NODE = "EndUserLocationValue";

	public static final String ENDUSERLOCATIONTYPE_NODE = "EndUserLocationType";

	public static final String BILLINGID_NODE = "BillingId";

	public static final String CAUSECODE_NODE = "CauseCode";

	public static final String CUSTOMERDISCONNECTDATE_NODE = "CustomerDisconnectDate";

	public static final String EFFECTIVERELEASEDATE_NODE = "EffectiveReleaseDate";

	public static final String LRN_NODE = "Lrn";

	public static final String GTTID_NODE = "GTTID";

	public static final String NEWSPDUEDATE_NODE = "NewSPDueDate";

	public static final String OLDSPDUEDATE_NODE = "OldSPDueDate";

	public static final String OLDSPAUTHORIZATION_NODE = "OldSPAuthorization";

	public static final String REQUEST_HEADER_PATH =
                                      "UpstreamToSOA.UpstreamToSOAHeader";

	public static final String REQUEST_BODY_PATH =
                                      "UpstreamToSOA.UpstreamToSOABody";

	public static final String SUBSCRIPTION_NODE = "Subscription";

	public static final String SVID_REGION_NODE = "SvIdRegion";

	public static final String SVID_NODE = "SvId";

	public static final String REGIONID_NODE = "RegionId";

	public static final String TN_NODE = "Tn";

	public static final String AUDITNAME_NODE = "AuditName";

	public static final String AUDITID_NODE = "AuditId";

	public static final String ROOT_NODE = "UpstreamToSOA.UpstreamToSOABody.SvModifyRequest.DataToModify.";

	public static final String CONTEXT_ROOT_NODE = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/";

	public static final int DATE_HHMI_LENGTH = 17;
	
	public static final String T1_CREATE_REQUEST_NOTIFICATION = "T1CreateRequestNotification";
	
	public static final String T2_FINAL_CREATE_WINDOW_EXPIRATION_NOTIFICATION = "T2FinalCreateWindowExpirationNotification";
	
	public static final String CREATING_STATUS = "creating";
	
	public static final String NPAC_CREATE_FAILURE ="NPACCreateFailure";

	public static final String NPACCREATFAILURE_STATUS = "NPACCreateFailure";
	
	public static final String  T1_CONCURRENCE_REQUEST_NOTIFICATION= "T1ConcurrenceRequestNotification";
	
	public static final String T2_FINAL_CONCURRENCE_WINDOW_EXPIRATION_NOTIFICATION = "T2FinalConcurrenceWindowExpirationNotification";
	
	public static final String LRN_CREATE_REQUEST = "LrnCreateRequest";
	
	public static final String LRN_DELETE_REQUEST = "LrnDeleteRequest";
	
	public static final String LRN_QUERY_REQUEST = "LrnQueryRequest";
	
	public static final String LRN_ID_NODE = "LrnId";
	
	public static final String NPANXX_CREATE_REQUEST = "NpaNxxCreateRequest";
	
	public static final String NPANXX_DELETE_REQUEST = "NpaNxxDeleteRequest";
	
	public static final String NPANXX_QUERY_REQUEST = "NpaNxxQueryRequest";
	
	public static final String NPA_NODE = "NpaValue";
	   
	public static final String NXX_NODE = "NxxValue";
	
	public static final String NPA_NXX_ID_NODE = "NpaNxxId";
	
	public static final String NPB_ACTIVATE_REQUEST = "NumberPoolBlockActivateRequest";
	
	public static final String NPB_MODIFY_REQUEST = "NumberPoolBlockModifyRequest";
	
	public static final String NPB_QUERY_REQUEST = "NumberPoolBlockQueryRequest";
 
	public static final String NPANXXX_QUERY_REQUEST = "NpaNxxXQueryRequest";
 
	public static final String SP_QUERY_REQUEST = "ServiceProviderQueryRequest";
 
	public static final String NPANXXX = "NpaNxxX";
    
	public static final String BLOCKID = "BlockId";
 
	public static final String BLOCK_HOLDER_SPID = "BlockHolderSPID";
	 
	public static final String LRN_VALUE_NODE = "LrnValue";

	public static final String SPID_NODE = "SPID";
	
	public static final String AUDIT_SPID_NODE = "AuditRequestingServiceProviderId";

	public static final String SV_MODIFY_REQUEST_ACTIVE = "SvModifyRequestActive";

	public static final String SV_MODIFY_REQUEST_DISCONNECT_PENDING = "SvModifyRequestDisconnectPending";

	public static final String REQUEST_HEADER_SUBREQUEST_PROP = "SUBREQUEST_TYPE";
		
	public static final String IS_RANGE_REQUEST_OUT_LOC_PROP ="IS_RANGE_REQUEST_OUT";
	
	
	public static final int TN_START_INDEX = 0;
	
	public static final int TN_END_INDEX = 12;
	
	public static final int STARTTN_END_INDEX = 8;
	
	public static final String SINGLE_QUOTE = "'";
	
	public static final String LEFT_BRACES = "[";
	
	public static final String RIGHT_BRACES = "]";
	
	public static final int MILLISECONDS = 86400000;
	
	public static final int TN_NPA_START_INDEX = 0;
	
	public static final int TN_NPA_END_INDEX = 3;
	
	public static final int TN_NXX_START_INDEX = 4;
	
	public static final int TN_NXX_END_INDEX = 7;
	
	public static final int TN_DASHX_START_INDEX = 8;
	
	public static final int TN_DASHX_END_INDEX = 9;
	
	public static final int DASHX_NPA_START_INDEX = 0;
	
	public static final int DASHX_NPA_END_INDEX = 3;
	
	public static final int DASHX_NXX_START_INDEX = 3;
	
	public static final int DASHX_NXX_END_INDEX = 6;
	
	public static final int DASHX_START_INDEX = 6;
	
	public static final int DASHX_END_INDEX = 7;
	
	public static final int DATE_START_INDEX = 0;
	
	public static final int DATE_MIDLLE_INDEX = 15;
	
	public static final int DATE_END_INDEX = 17;
	
	public static final String DATE_TIME_CONCATENATION = "00";

    // The following constants will be used for inserting into MessageSubtype
	// column of some SOA database tables.
	
	public static final String SENT="Sent";

	public static final String REQUEST = "Request";

	public static final String INSERT = "INSERT ";

	public static final String SOA_MESSAGE = "SOA_MESSAGE";

	public static final String RESPONSE = "Response";
	
	public static final String NOTIFICATION = "Notification";

	public static final String RECEIVED = "Received";

	public static final String SYSTEM = "System";
	
	/**
	 * The name of the domain.
	 */
	public static final String DOMAIN_NAME = "DOMAIN";

	/**
	 * The name of source file.
	 */
	public static final String SOURCE_FILE_NAME = "SOURCE_FILE_NAME";

	/**
	 * The type of source file.
	 */
	public static final String SOURCE_FILE_TYPE = "SOURCE_FILE_TYPE";

	/**
	 * The name of Database.
	 */
	public static final String DATABASE_NAME = "DBNAME";

	/**
	 * The name of Database User.
	 */
	public static final String DATABASE_USER_NAME = "DBUSER";

	/**
	 * The name of Database Password.
	 */
	public static final String DATABASE_PASSWORD = "DBPASSWORD";

		/**
	 * The name of log file. 
	 */
	public static final String LOG_FILE_NAME="LOG_FILE_NAME";
	
	/**
	 * The name of report file. 
	 */
	public static final String REPORT_FILE_NAME="REPORT_FILE_NAME";


	public static final String NULL_ERROR=" Required data either missing or invalid format in " +
												" the Notification BDD File";
	
	public static final String REQ_NULL_ERROR=" Required parameters might contain null data";
	

	/**
	 * The title name is reason"
	 */
	public static final String REASON="REASON";
    /**
     *  The column of version telephone number
     */ 
	public static final String VERSIONTN_COL="VERSIONTN";
	/**
	 *  The column name is Audit id.
	 */
	public static final String AUDITID_COL="AUDITID";
	/**
	 *  The column name is Numberpoolblockid.
	 */
	public static final String NBRPOOLBLKID_COL="NBRPOOLBLKID";
	// The following constants will be used to display a header for failed record.
	
	public static final String NPAC_OPER_INO_RECORD="NPAC OPERATION INFO RECORD";
	
	public static final String SUB_AUDIT_RECORD="SUB AUDIT RECORD";
	
	public static final String NBR_POOLBLK_RECORD="NBRPOOLBLOCK RECORD";
	
	public static final String SV_RECORD="SV RECORD";
	
	public static final String NPANXX_RECORD="NPANXX RECORD";
	/**
	 * An extention of a file.
	 */
	public static final String FILE_EXTENSION = ".csv";

	public static final String SOA_CONSTANTS_CLASS = "com.nightfire.spi.neustar_soa.utils.SOAConstants";
	
		
    // The following constants will be used to map from NAPC to SOA.
	
	public static final String AUDIT_DISCRE_REPORT_NOTIFICATION = "AuditDiscrepancyReportNotification";
	
	public static final String AUDIT_OBJECT_CREATE_NOTIFICATION = "AuditCreateNotification";
	
	public static final String AUDIT_OBJECT_DELETE_NOTIFICATION = "AuditDeleteNotification";
	
	public static final String AUDIT_RESULTS_NOTIFICATION ="AuditResultsNotification";
	
	public static final String NBR_POOLBLK_OBJECT_CREATE="NumberPoolBlockCreateNotification";
	
	public static final String NBR_POOLBLK_ATTR_VAL_CH_NOTIFICATION="NumberPoolBlockModifyNotification";
	
	public static final String NBR_POOLBLK_ATTR_VAL_STATUS_CH_NOTIFICATION="NumberPoolBlockStatusChangeNotification";
	
	public static final String SOA_SV_DONORSP_CUSTOMER_DISCONNECTDATE ="SvReturnToDonorNotification";

	public static final String SV_OLDSP_CONCURR_REQUEST_NOTIFICATION ="VersionOldSP_ConcurrenceRequestNotification";

	public static final String SV_NEW_NPANXX = "NpaNxxCreateNotification";
	
	public static final String SOA_LNP_NPAC_SMS_OPER_INFO="NPACShutdownNotification";
	
		
	// The following constants will be used to refer NAPC notifications
	
	public static final String SV_NEWSP_CREATE_REQUEST_NOTIFICATION= "VersionNewSP_CreateRequestNotification";
	
	public static final String SV_VERSION_CANCELLATION_ACK_REQUEST_NOTIFICATION = "VersionCancellationAcknowledgeRequestNotification";

	public static final String SV_DONORSP_CUSTOMER_DISCONNECTDATE_NOTIFICATION = "VersionCustomerDisconnectDateInfoNotification";
	
	public static final String SUB_NEW_NPANXX_NOTIFICATION="VersionNewNPA_NXXNotification";
	
	public static final String NBR_POOLBLK_OBJECT_CREATION_NOTIFICATION = "NumberPoolBlockCreationNotification";

	public static final String NBR_POOLBLK_ATTR_VAL_CHANGE_NOTIFICATION = "NumberPoolBlockAttributeValueChangeNotification";

	public static final String NBR_POOLBLK_STA_ATTR_VAL_CHANGE_NOTIFICATION = "NumberPoolBlockStatusAttributeValueChangeNotification";

	public static final String SUB_AUDIT_RESULTS_NOTIFICATION = "SubscriptionAuditResultsNotification";

	public static final String SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION = "SubscriptionAuditDiscrepancyRptNotification";

	public static final String SUB_AUDIT_OBJECT_CREATION_NOTIFICATION = "SubscriptionAuditCreationNotification";

	public static final String SUB_AUDIT_OBJECT_DELETION_NOTIFICATION = "SubscriptionAuditDeletionNotification";
	
	public static final String VER_OLDSP_FINAL_CONCUR_WINDOW_EXP_NOTIFICATION="VersionOldSPFinalConcurrenceWindowExpirationNotification";

	public static final String VER_NEWSP_FINAL_CREATE_WINDOW_EXP_NOTIFICATION = "VersionNewSPFinalCreateWindowExpirationNotification";

	public static final String LNP_NPAC_SMS_OPER_INFO="LnpNPAC_SMS_OperationalInformation";
	

   // The following constants will be representing the responses which are received from NPAC.

	public static final String NPAC_SV_CAN_ACK_REQ="subscriptionVersionCancellationAcknowledgeRequest";
	
	public static final String NPAC_SV_RANGE_CAN_ACK_REQ="subscriptionVersionRangeCancellationAcknowledgeRequest";
	
	public static final String NPAC_SV_DONOR_CUST_DIS="subscriptionVersionDonorSP-CustomerDisconnectDate";
	
	public static final String NPAC_SV_RANGE_DONOR_CUST_DIS="subscriptionVersionRangeDonorSP-CustomerDisconnectDate";
	
	public static final String NPAC_SV_NEWSP_CRE_REQ="subscriptionVersionNewSP-CreateRequest";
	
	public static final String NPAC_SV_RANGE_NEWSP_CRE_REQ="subscriptionVersionRangeNewSP-CreateRequest";
	
	public static final String NPAC_SV_OLDSP_CONCUR_REQ="subscriptionVersionOldSP-ConcurrenceRequest";
	
	public static final String NPAC_SV_RANGE_OLDSP_CONCUR_REQ="subscriptionVersionRangeOldSP-ConcurrenceRequest";
	
	public static final String NPAC_SV_STA_ATTR_VAL_CH="subscriptionVersionStausAttributeValueChange";
		
	public static final String NPAC_SV_RANGE_STA_ATTR_VAL_CH="subscriptionVersionRangeStausAttributeValueChange";
	
	public static final String NPAC_SV_OBJ_CRE="subscriptionVersionNPAC-ObjectCreation";
		
	public static final String NPAC_SV_RANGE_OBJ_CRE="subscriptionVersionRangeNPAC-ObjectCreation";
	
	public static final String NPAC_SV_ATTR_VAL_CH="subscriptionVersionNPAC-attributeValueChange";
	
	public static final String NPAC_SV_RANGE_ATTR_VAL_CH="subscriptionVersionRangeNPAC-attributeValueChange";
	
	public static final String NPAC_SUB_AUDIT_DIS_RPT="subscriptionAudit-DiscrepancyRpt";
	
	public static final String NPAC_SUB_AUDIT_RSLTS="subscriptionAuditResults";
	
	public static final String NPAC_SUB_AUDIT_OBJ_CR="subscriptionAudit-ObjectCreation";
	
	public static final String NPAC_SUB_AUDIT_OBJ_DEL="subscriptionAudit-ObjectDeletion";
	
	public static final String NPAC_LNP_SMS_OPER_INFO="lnpNPAC-SMS-Operational-Information";
	
	public static final String NPAC_SV_NEW_NPANXX="subscriptionVersionNewNPA-NXX";
	
	public static final String NPAC_SV_OLDSP_FINAL_CONCUR_WINEXP="subscriptionVersionOldSPFinalConcurrenceWindowExpiration";
	
	public static final String NPAC_SV_RANGE_OLDSP_FINAL_CONCUR_WINEXP="subscriptionVersionRangeOldSPFinalConcurrenceWindowExpiration";
	
	public static final String NPAC_NBR_POOLBLK_OBJ_CR="numberPoolBlock-objectCreation";
	
	public static final String NPAC_NBR_POOLBLK_ATTR_VAL_CH="numberPoolBlock-attributeValueChange";
	
	public static final String NPAC_NBR_POOLBLK_STA_ATTR_VAL_CH="numberPoolBlockStatusAttributeValueChange";
	
	public static final String NPAC_SV_NEWSP_FINAL_CRE_WINEXP="subscriptionVersionNewSPFinalCreateWindowExpiration";
	
	public static final String NPAC_SV_RANGE_NEWSP_FINAL_CRE_WINEXP="subscriptionVersionRangeNewSPFinalCreateWindowExpiration";
	


	public static final String SV="SV";
	
	public static final String NPB="NPB";
	
	public static final String AUDIT_BDD_FILE_TYPE="AUDIT_BDD";
	
	public static final String NPAC_BDD_FILE_TYPE="AUDIT_BDD";
    
	public static final String FAILED_SPLIST_XML="<?xml version=\"1.0\"?>";
	
	public static final String FAILED_SPLIST_ROOT_START="<FailedServiceProviderList>";
	
	public static final String FAILED_SPLIST_ROOT_END="</FailedServiceProviderList>";
	
	public static final String FAILED_SPLIST_SPID="<ServiceProviderId value=\"";
	
	public static final String FAILED_SPLIST_SERVICENAME="<ServiceProviderName value=\"";
	
	//VersionNewSPFinalCreateWindowExpirationNotification;
	// The following constants will be used for tables to store the Bulk Down
	// Load data into
	// the respective tables.

	public static final String SOA_AUDIT_NPAC = "SOA_AUDIT_NPAC";

	public static final String SOA_SV_MESSAGE = "SOA_SV_MESSAGE";

	public static final String SOA_NPANXX_MESSAGE = "SOA_NPANXX_MESSAGE";

	public static final String SOA_NBRPOOL_BLOCK_MESSAGE = "SOA_NBRPOOL_BLOCK_MESSAGE";

	public static final String SOA_MESSAGE_MAP = "SOA_MESSAGE_MAP";

	public static final String SOA_SV_RANGE = "SOA_SV_RANGE";
	
	public static final String SOA_SV_RANGE_MAP = "SOA_SV_RANGE_MAP";
	
	public static final String SOA_SV_FAILEDSPLIST="SOA_SV_FAILEDSPLIST";
	
	public static final String SOA_COMMON_MESSAGE="SOA_COMMON_MESSAGE";
	

	// The following insert statements will be called to store the Bulk Down
	// Load data into the abovementioned tables
	public static final String SOA_MSG_MAP = "insert into " + SOA_MESSAGE_MAP +"(MESSAGEKEY, REFERENCEKEY)"
			+ " values (?,?)";
	
	public static final String SOA_MSG_MAP_INSERT = "INSERT INTO " + SOA_MESSAGE_MAP +"(MESSAGEKEY, REFERENCEKEY, SYNCMSGKEY)"
	+ " values (?, ?, ?)";

	public static final String SOA_RANGE_MAP = "insert into " + SOA_SV_RANGE +"(RANGEID, MESSAGESUBTYPE, MESSAGEKEY, DATETIME, SUBDOMAINID)"
	+ " values (?,?,?,?,?)";
	
	public static final String SOA_RANGE_MAP_INSERT = "insert into " + SOA_SV_RANGE_MAP +"(RANGEID, REFERENCEKEY)"
	+ " values (?,?)";

	//Added to update SOA_SUBSCRIPTION_VERSION for rangeid
	public static final String SOA_SUBSCRIPTION_VERSION_UPDATE = "Update SOA_SUBSCRIPTION_VERSION SET RANGEID=? WHERE REFERENCEKEY=? ";

	public static final String SOA_NOTIFICATION_DATA_INSERT = "insert into "
			+ SOA_SV_MESSAGE
			+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,PORTINGTN,DATETIME,MESSAGE) values(?,?,?,?,?,?,?,?,?,?)";
	
	public static final String SUB_AUDIT_OBJECT_CREATE_DELEETE_INSERT = "insert into "
			+ SOA_AUDIT_NPAC
			+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,AUDITID,DATETIME,MESSAGE) values (?,?,?,?,?,?,?,?,?,?)";

	public static final String SUB_AUDIT_DIS_RPT_INSERT = "insert into "
			+ SOA_AUDIT_NPAC
			+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,TNRANGE,DATETIME,MESSAGE) values (?,?,?,?,?,?,?,?,?,?)";

	public static final String SUB_AUDT_RSLTS_INSERT = "insert into "
			+ SOA_AUDIT_NPAC
			+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,DATETIME,MESSAGE) values (?,?,?,?,?,?,?,?,?)";
	
	public static final String SV_NEW_NPA_NXX_INSERT = "insert into "
			+ SOA_NPANXX_MESSAGE
			+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,DATETIME,MESSAGE,NPANXXID,STARTNPA,STARTNXX) values (?,?,?,?,?,?,?,?,?,?,?,?)";

	public static final String NBRPOOLBLOCK_OBJCREATION_INSERT = "insert into "
			+ SOA_NBRPOOL_BLOCK_MESSAGE
			+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,NPBID,DATETIME,MESSAGE,STARTNPA,STARTNXX,STARTDASHX) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public static final String NBRPOOLBLK_STAUS_ATTR_VAUE_CHANGE_INSERT = "insert into "
			+ SOA_NBRPOOL_BLOCK_MESSAGE
			+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,NPBID,DATETIME,MESSAGE) values (?,?,?,?,?,?,?,?,?,?)";
	
	public static final String FAILED_SPLIST_INSERT="insert into " + SOA_SV_FAILEDSPLIST + " values (?,?,?,?)";
		
	public static final String NPAC_OPERATIONS_INSERT="insert into " + SOA_COMMON_MESSAGE + " values (?,?,?,?,?,?)";	

	// This constants will be used to set the messages are SvID and MaxTN into context.
	
	public static final String KEY_NODE_CHILD_SVID="SvId";
	
	public static final String KEY_NODE_CHILD_MAXTN="Tn";
	
	public static final String OUTPUT_MESSAGE_MAXTN="OUTPUT_MESSAGE_MAXTN";
	
	public static final String OUTPUT_MESSAGE_SVID="OUTPUT_MESSAGE_SVID";

	public static final String OUTPUT_RECORD_COUNT="OUTPUT_RECORD_COUNT";
	
	public static final String NUMBER_OF_RECORD="NUMBER_OF_RECORD";
	
	public static final String INPUT_NODE_NAME="INPUT_NODE_NAME";

	public static final String INPUT_PORTINGTN="INPUT_PORTINGTN"; 
	
	public static final String NODE_ATTRIBUTE="value";

	public static final String SV_MODIFY_REQUEST_CANCEL_PENDING = "SvModifyRequestCancelPending";

	// This constants will be used to update the LASTMESSAGE column in 
    // SOA_SUBSCRIPTION_VERSION table.
	
	public static final String SV_LASTMESSAGE_UPDATE_QUERY="update /*+ INDEX(" + SV_TABLE + " SOA_SV_INDEX_1 SOA_SV_INDEX_2) */ " +
		SV_TABLE + " SET LASTMESSAGE=? WHERE SPID=? AND SVID=? AND PORTINGTN=?";

    public static String SOA_AUDIT="SOA_AUDIT";
	
	public static final String AUDIT_LASTRES_UPDATE_OBJE_CRE_QUERY="update " +
	SOA_AUDIT + " SET LASTRESPONSE=?,LASTRESPONSEDATE=? WHERE SPID=? " +
		" AND AUDITID=?";
	
	public static final String AUDIT_LASTRES_UPDATE_DESRPT_QUERY="update " +
	SOA_AUDIT + " SET LASTRESPONSE=?,LASTRESPONSEDATE=?,TNRANGE=?  WHERE SPID=?";
	
	public static final String AUDIT_LASTRES_UPDATE_RSLTS_QUERY="update " +
	SOA_AUDIT + " SET LASTRESPONSE=?,LASTRESPONSEDATE=?  WHERE SPID=?";
	
	public static final String LASTRES_UPDATE_NPANXX__QUERY="update /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_2) */ " +
	NPANXX_TABLE + " SET LASTRESPONSE=?,LASTRESPONSEDATE=? WHERE NPANXXID=?";
	
	public static final String LASTRES_UPDATE_NBRPOOLBLK__QUERY="update " +
	NBRPOOL_BLOCK_TABLE + " SET LASTRESPONSE=?,LASTRESPONSEDATE=? " +
			" WHERE SPID=? AND NPA=? and NXX=? AND DASHX=?";
	
	public static final String NBRPOOLBLK__SELECT_QUERY="SELECT SPID FROM " +
	NBRPOOL_BLOCK_TABLE + " WHERE SPID=? AND NPA=? and NXX=? AND DASHX=?";	
	
	/**
	* The SQL insert statement used to insert new NBRPOOL_BLOCK data.
	*/
		
	public static final String NBRPOOLBLK__INSERT_QUERY = "INSERT INTO SOA_NBRPOOL_BLOCK ( " +
	"SPID, NPA, NXX, DASHX, LRN, CLASSDPC, CLASSSSN, CNAMDPC, CNAMSSN, " +
	" ISVMDPC, ISVMSSN, LIDBDPC, LIDBSSN, WSMSCDPC, WSMSCSSN, " +
	"NPBID, SOAORIGINATION, STATUS, ACTIVATIONDATE, CREATEDBY, " +
	"CREATEDDATE, OBJECTCREATIONDATE,CUSTOMERID) VALUES ( ?, ?, ?, ?, ?, " +
	"?, ?, ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	public static final String NPANXX__INSERT_QUERY = "INSERT INTO SOA_NPANXX ( " +
	"SPID, NPA, NXX, LRN, CLASSDPC, CLASSSSN, CNAMDPC, CNAMSSN, " +
	" ISVMDPC, ISVMSSN, LIDBDPC, LIDBSSN, WSMSCDPC, WSMSCSSN, " +
	"NPBID, SOAORIGINATION, STATUS, ACTIVATIONDATE, CREATEDBY, " +
	"CREATEDDATE, OBJECTCREATIONDATE,CUSTOMERID) VALUES ( ?, ?, ?, ?, " +
	"?, ?, ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public static final String NPB__INSERT_QUERY = "INSERT INTO SOA_NBRPOOL_BLOCK ( " +
	"SPID, NPA, NXX, DASHX, LRN, CLASSDPC, CLASSSSN, CNAMDPC, CNAMSSN, " +
	" ISVMDPC, ISVMSSN, LIDBDPC, LIDBSSN, WSMSCDPC, WSMSCSSN, " +
	"NPBID, SOAORIGINATION, STATUS, ACTIVATIONDATE, CREATEDBY, " +
	"CREATEDDATE, OBJECTCREATIONDATE) VALUES ( ?, ?, ?, ?, ?, " +
	"?, ?, ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public static final String LASTRES_UPDATE_NBRPOOLBLKSTA__QUERY="update " +
	NBRPOOL_BLOCK_TABLE + " SET LASTRESPONSE=?,LASTRESPONSEDATE=?" +
			" WHERE SPID=? AND NPBID=?";
	
	/**
     * This is the date format used by date values found in the NPAC files. This
     * is used to parse the value of these date strings.
     */
    public static final String FILE_DATE_FORMAT = "yyyyMMddHHmmss";
	
	public static final String MESSAGESUBTYPE_ERROR="The messagesubtype element" +
                        		" might contain null data, null data cann't be " +
                        		"inserted in not null column";

	public static final String LSR_NOT_RECEIVED = "LSRNotReceived";
	
	public static final String FOC_NOT_ISSUED = "FOCNotIssued";

    // This constant will be used to retrieve the value of some nodes in the component classes.

	public static final String KEY_NODE_CHILD_SUB_MAXTN="subscription_version_tn";

	// This constant will be used in BDD to determine whether the token is for LRN or not. 
	public static final String LRNTOKEN="LRNTOKEN";
 
	// This constant will be used in BDD to determine whether the token is for NPANXX or not.
	public static final String NPANXXTOKEN="NPANXXTOKEN";
 
	// This constant will be used in BDD to determine whether the token is for NPANXXX or not.
	public static final String NPANXXXTOKEN="NPANXXXTOKEN";
 
	// This constant will be used in BDD to determine whether the token is for SOA_NBRPOOL_BLOCK or not.
	public static final String NBRBLKTOKEN="NBRBLKTOKEN";

	// This constant will be used in BDD to determine whether the token is for SOA_SPID or not.
	public static final String SPIDTOKEN="SPIDTOKEN";
	
	public static final String SPTYPE_WIRELINE = "wireline";
	
	public static final String SPTYPE_WIRELESS = "wireless";
	
	public static final String SPTYPE_NONCARRIER = "non_carrier";
	
	public static final String SPTYPE_CLASS1_VOIP = "Class 1 VoIP";
	
	public static final String SPTYPE_OTHERCARRIER = "other_carrier";
	
	public static final String SVTYPE_WIRELINE = "Wireline";
	
	public static final String SVTYPE_WIRELESS = "Wireless";

	public static final String SVTYPE_CLASS2_VOIP = "Class 2 VoIP";

	public static final String SVTYPE_VOWIFI = "VoWIFI";

	public static final String SVTYPE_PRE_PAID_WIRELESS = "Pre-Paid Wireless";

	public static final String SVTYPE_CLASS1_VOIP = "Class 1 VoIP";

	public static final String SVTYPE_SVTYPE6 = "SvType6";
	
	public static final String SUBSCRIPTION_VERSION_INSERT = "INSERT INTO SOA_SUBSCRIPTION_VERSION ( " +
			"CUSTOMERID, REFERENCEKEY, SPID, REGIONID, NNSP, ONSP, LNPTYPE, PORTINGTN, NNSPDUEDATE, " +
			"ONSPDUEDATE, SVID, LRN, CLASSDPC, CLASSSSN, CNAMDPC, CNAMSSN, ISVMDPC, ISVMSSN, LIDBDPC," +
			" LIDBSSN, WSMSCDPC, WSMSCSSN, PORTINGTOORIGINAL, BILLINGID, ENDUSERLOCATIONTYPE, " +
			"ENDUSERLOCATIONVALUE, AUTHORIZATIONFLAG, CAUSECODE, CONFLICTDATE, AUTHORIZATIONDATE, " +
			"CUSTOMERDISCONNECTDATE, EFFECTIVERELEASEDATE, STATUS, LASTRESPONSE, LASTRESPONSEDATE, " +
			"DISCONNECTCOMPLETEDATE, ACTIVATIONDATE, LASTREQUESTTYPE, LASTREQUESTDATE, LASTMESSAGE, " +
			"OBJECTCREATIONDATE, OBJECTMODIFIEDDATE, PRECANCELLATIONSTATUS, STATUSOLDDATE, " +
			"OLDSPCONFLICTRESOLUTIONDATE, NEWSPCONFLICTRESOLUTIONDATE, OLDSPCANCELETIONDATE, " +
			"NEWSPCANCELETIONDATE, CANCELEDDATE, BUSINESSTIMERTYPE, BUSINESSHOURS, FAILEDSPFLAG, " +
			"CREATEDBY, CREATEDDATE, SVTYPE, ALTERNATIVESPID, VOICEURI, MMSURI, POCURI, PRESURI ) VALUES ( ?, " +
			"?, ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
			"?, ?, ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * The name of the SOA_NPANXX_LRN  table.
	 */
	public static final String SOA_NPANXX_LRN = "SOA_NPANXX_LRN";

	public static final String REFKEY_SEPARATOR = ";";

	public static final String REFERENCE_KEY_PROP = "REFERENCE_KEY_LOCATION";

	public static final String RANGE_ID_PROP = "RANGE_ID_LOCATION";

	public static final String KEY_SEPERATOR_PROP = "KEY_SEPERATOR";
	
	public static final String OUTPUT_FAILED_TN_LOC="OUTPUT_FAILED_LOC";

	public static final String FAILED_TN_LOCATION = "@context.FailedTns";
	
	public static final String FAILED_TN_LOC_PROP = "FAILED_TN";
	
	public static final String PASSED_TN_LOC_PROP = "PASSED_TN";
	
	public static final String SV_OBJECT_SELECT = "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, REFERENCEKEY, STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE PORTINGTN = ? AND SPID = ? AND ONSP = ? AND NNSP = ? AND STATUS  IN('conflict','pending','creating','NPACCreateFailure' ) ORDER BY CREATEDDATE DESC";
	
	public static final String SOA_SV_MESSAGE_DATA_INSERT = "insert into "
		+ SOA_SV_MESSAGE
		+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,PORTINGTN,DATETIME,INTERFACEVERSION,USERID,MESSAGE,INVOKEID, SUBDOMAINID,INPUTSOURCE, MULTITNSEQUENCEID,CAUSECODE) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	public static final String SOA_SV_MESSAGE_DATA_INSERT_QUERY = "insert into "
		+ SOA_SV_MESSAGE
		+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,PORTINGTN,DATETIME,INTERFACEVERSION,USERID,MESSAGE,INVOKEID, SUBDOMAINID, REGIONID, INPUTSOURCE, MULTITNSEQUENCEID) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	public static final String SOA_SV_REQUEST_QUEUE_INSERT = "INSERT INTO SOA_REQUEST_QUEUE (ID, MESSAGETYPE, ARRIVALTIME, ERRORCOUNT, DEFERREDCOUNT, STATUS, MESSAGE, SPID)VALUES ( ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public static final String SOA_LAST_COLUMN_UPDATE = "UPDATE SOA_SUBSCRIPTION_VERSION SET LASTREQUESTTYPE = ?, LASTREQUESTDATE = ?, LASTMESSAGE = ?, LASTPROCESSING = ?";

	public static final String ACCOUNTID_COL = "ACCOUNTID";

	public static final String ACCOUNTNAME_COL = "ACCOUNTNAME";

	public static final String ACCOUNT_ID_NODE = "AccountId";

	public static final String ACCOUNT_NAME_NODE = "AccountName";
	
	public static final String SV_MODIFY_STATUS_NODE = "UpstreamToSOA.UpstreamToSOABody.SvModifyRequest.SvStatus";
	
	public static final String LASTREQUESTDATE_COL = "LASTREQUESTDATE";
        
        public static final String ACTIVATION_DATE_COL = "ACTIVATIONDATE";
	
	public static final String CREATEDBY_COL = "CREATEDBY";

	public static final String LONG_TIMER_TYPE = "long";

	public static final String ACTIVATION_DATE = "@context.ActivationDate";

	public static final String CANCELLATION_DATE = "@context.CanceledDate";

	public static final String DISCONNECT_COMPLETE_DATE = "@context.DisconnectedCompleteDate";

	public static final String STATUS_OLD_DATE = "@context.StatusOldDate";

	public static final String OBJECT_MODIFIED_DATE = "@context.ObjModifiedDate";

	public static final String STATUS_CONTEXT = "@context.Status";

	public static final String SOA_SV_DONOR_NOTIFICATION ="SvReturnToDonorNotification";

	public static final String OLD_SP_RESOLUTION_DATE = "@context.ONSPConflictResolutionDate";

	public static final String NEW_SP_RESOLUTION_DATE = "@context.NNSPConflictResolutionDate";

	public static final String DISCONNECT_ONSP = "@context.DisconnectOnspValue";

	public static final String CAUSE_CODE_VALUE = "@context.CausecodeValue";

        public static final String AUTHORIZATION_FLAG = "AUTHORIZATION";
	
	public static final String AUTHORIZATION_Date = "SOAToUpstream.SOAToUpstreamBody.SvAttributeChangeNotification.ModifiedData.OldSPAuthorizationTimestamp";

	public static final String SV_QUERY_GEN_TN_LIST_PROP = "SV_QUERY_TN_LIST";
	
	public static final String SV_QUERY_GEN_CUST_ID_PROP = "SV_QUERY_CUSTOMER_ID";

	public static final String SV_QUERY_GEN_XML_LOCATION_PROP = "SV_QUERY_XML_LOCATION";
	
	public static final String MSG_KEY_UPDATE_TABLE_NAME_PROP = "MSG_KEY_UPDATE_TABLE_NAME";
	
	public static final String MSG_KEY_SEARCH_TABLE_NAME_PROP = "MSG_KEY_SEARCH_TABLE_NAME";
	
	public static final String NEUSTAR_FULL_SOA_MSG_KEY = "NEUSTARFULLSOAMSGKEY";

	public static final String NEUSTAR_FULL_SOA_QUEUE_ID = "SOAREQUESTQUEUEID";

	public static final String NEUSTAR_FULL_SOA_RANGE_KEY = "NEUSTARFULLSOASVRANGEKEY";

	public static final String NEUSTAR_FULL_SOA_REF_KEY = "NEUSTARFULLSOAREFKEY";

	public static final String NEUSTAR_FULL_SOA_RANGEID_KEY = "SOARANGEID";

	public static final String SHORT_TIMER_TYPE = "short";

	public static final String CONTEXT_ROOT_NODE_LRN = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/Lrn";

	public static final String CONTEXT_ROOT_NODE_NNSPDUEDATE = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/NewSPDueDate";
	
	public static final String CONTEXT_ROOT_NODE_ONSPDUEDATE = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/OldSPDueDate";
	
	public static final String CONTEXT_ROOT_NODE_ONSPAUTHORIZATION = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/OldSPAuthorization";
	
	public static final String CONTEXT_ROOT_NODE_CLASSDPC = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/ClassDPC";
	
	public static final String CONTEXT_ROOT_NODE_CLASSSSN = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/ClassSSN";
	
	public static final String CONTEXT_ROOT_NODE_LIDBDPC = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/LidbDPC";
	
	public static final String CONTEXT_ROOT_NODE_LIDBSSN = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/LidbSSN";
	
	public static final String CONTEXT_ROOT_NODE_ISVMDPC = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/IsvmDPC";
	
	public static final String CONTEXT_ROOT_NODE_ISVMSSN = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/IsvmSSN";
	
	public static final String CONTEXT_ROOT_NODE_CNAMDPC = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/CnamDPC";
	
	public static final String CONTEXT_ROOT_NODE_CNAMSSN = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/CnamSSN";
	
	public static final String CONTEXT_ROOT_NODE_WSMSCDPC = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/WsmscDPC";
	
	public static final String CONTEXT_ROOT_NODE_WSMSCSSN = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/WsmscSSN";
	
	public static final String CONTEXT_ROOT_NODE_ENDLOCVALUE = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/EndUserLocationValue";
	
	public static final String CONTEXT_ROOT_NODE_ENDLOCTYPE = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/EndUserLocationType";
	
	public static final String CONTEXT_ROOT_NODE_BILLINGID = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/BillingId";
	
	public static final String CONTEXT_ROOT_NODE_CAUSECODE= "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/CauseCode";
	
	public static final String CONTEXT_ROOT_NODE_CUSTOMERDISCONNECTDATE = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/CustomerDisconnectDate";
	
	public static final String CONTEXT_ROOT_NODE_EFFECTIVERELEASEDATE = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/EffectiveReleaseDate";
	
	public static final String CONTEXT_ROOT_NODE_SVSTATUS = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/SvStatus";
	
	public static final String CONTEXT_ROOT_NODE_SVTYPE = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/SvType";
	
	public static final String CONTEXT_ROOT_NODE_ALTERNATIVESPID = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/AlternativeSPID";
	
	public static final String CONTEXT_ROOT_NODE_VOICEURI = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/VoiceURI";
	
	public static final String CONTEXT_ROOT_NODE_MMSURI = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/MMSURI";
	
	public static final String CONTEXT_ROOT_NODE_POCURI = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/PoCURI";
	
	public static final String CONTEXT_ROOT_NODE_PRESURI = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/PRESURI";
	
	public static final String CONTEXT_ROOT_NODE_SMSURI = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/SMSURI";
	
	public static final String ATTRIBUTE_TIMESTAMP = "@context.AttributeTimeStamp";

	public static final String STATUS_TIMESTAMP = "@context.StatusTimeStamp";

	public static final String OUTOFSEQUENCE_LOC = "OUTOFSEQUENCE";

	public static final String REQUEST_ERROR_PATH = SYNCHRONUS_RESPONSE_PATH + ".RequestError";

	public static final String ERROR_CONTAINER_PATH = REQUEST_ERROR_PATH + ".ErrorContainer";
	
	public static final String RULE_ERROR_PATH = ERROR_CONTAINER_PATH + ".RuleError";

	public static final String SOA_NPAC_QUEUE_INSERT = "INSERT INTO NPAC_QUEUE (ID, MESSAGETYPE, ARRIVALTIME, ERRORCOUNT, DEFERREDCOUNT, STATUS, MESSAGE, MESSAGEKEY, TABLENAME, SPID)VALUES ( ?, ?, ?, ?, ?, ?, ?, ?,?,?)";

	public static final String SOA_NPAC_MESSAGE_INSERT = "INSERT INTO NPAC_MESSAGE (MESSAGEKEY, DATETIME, MESSAGETYPE, MESSAGE, TABLENAME, MESSAGESUBTYPE, SPID)VALUES ( ?, ?, ?, ?, ?, ?, ?)";

	public static final String NPAC_QUEUE_ID = "NPACQUEUEID";

	public static final String NPAC_QUEUE_MESSAGE_TYPE = "npac";

	public static final String NPAC_QUEUE_STATUS = "awaitingDelivery";

	public static final String SOA_QUEUED_STATUS = "Queued";

	public static final String SOA_DATAERROR_STATUS = "Data Error";

	public static final String CONTEXT_TN = "@context.tn";

	public static final String SVTYPE_COL = "SVTYPE";

	public static final String ALTERNATIVESPID_COL = "ALTERNATIVESPID";
	
	public static final String ALTERNATIVE_ENDUSERLOCATIONVALUE_COL = "ALTENDUSERLOCATIONVALUE";
	
	public static final String ALTERNATIVE_ENDUSERLOCATIONTYPE_COL = "ALTENDUSERLOCATIONTYPE";
	
	public static final String ALTERNATIVE_BILLINGID_COL = "ALTBILLINGID";
	
	public static final String VOICEURI_COL = "VOICEURI";
	
	public static final String MMSURI_COL = "MMSURI";
	
	public static final String POCURI_COL = "POCURI";
	
	public static final String PRESURI_COL = "PRESURI";
	
	public static final String SMSURI_COL = "SMSURI";
	
	public static final String CONFLICT_DATE = "CONFLICTDATE";
	
	public static final String AUTHORIZATIONDATE_COL = "AUTHORIZATIONDATE";

	public static final String BUSINESSTIMERTYPE_COL = "BUSINESSTIMERTYPE";

	public static final String BUSINESSHOURS_COL = "BUSINESSHOURS";

	public static final String NNSPDUEDATE_COL = "NNSPDUEDATE";

	public static final String LNPTYPE_COL = "LNPTYPE";

	public static final String RANGEID_COL = "RANGEID";

	public static final String ENDUSERLOCATIONVALUE_COL = "ENDUSERLOCATIONVALUE";
	
	public static final String ENDUSERLOCATIONTYPE_COL = "ENDUSERLOCATIONTYPE";
	
	public static final String BILLINGID_COL = "BillingId";
	
	public static final String MESSAGE_TYPE_LOC = "MESSAGE_TYPE";
	
	public static final String MESSAGE_SUB_TYPE_LOC = "MESSAGE_SUB_TYPE";
	
	public static final String SYNC_RESPONSE_MESSAGE_LOC = "SYNC_RESPONSE_MESSAGE";
	
	public static final String INTERFACE_VERSION_LOC = "INTERFACE_VERSION";
	
	public static final String CUSTOMER_ID_LOC = "CUSTOMER_ID";	

	public static final String INPUT_LOC_REQUEST_ID_MESSAGE_PROP = "REQUEST_ID_MESSAGE";

	public static final String ACTION_LOC = "ACTION";

	public static final String SYNCHRONOUS_RESPONSE = "SynchronousResponse";

    public static final String SVID_SEPARATOR = ";";
        
    public static final String PORTINGTN_SEPARATOR = ";";
	
    public static final String OID = "OID";

    public static final String TransOID = "TransOID";
    
    public static final String INVOKE_ID_PROP = "INVOKEID";
    
    public static final String OID_COL = "OID";
    
    public static final String TransOID_COL = "TRANSOID";
    
    public static final String NPA = "NPA";
    
    public static final String NXX = "NXX";
    
    public static final String OIDTransOIDPOPULATION_FLAG = "OIDTransOIDPOPULATION_FLAG";
    
    public static final String BLOCKID_PROP = "BlockId";
    
    public static final String NPANXX_X_PROP = "NpaNxxX";
    
    public static final String NPB_LASTREQUESTTYPE = "LASTREQUESTTYPE";
    
    public static final String NPB_LASTRESPONSE = "LASTRESPONSE";
    
    public static final String NBR_POOLBLK_STATUS_CH_NOTIFICATION = "NumberPoolBlockStatusChangeNotification";
    
    public static final String NBR_POOLBLK_ACTIVATE_NOTIFICATION = "NumberPoolBlockActivateNotification";
    
    public static final String BLOCK_STATUS = "BlockStatus";
    
    public static final String OUTPUT_NPBSOA_NOTIFICATION = "OUTPUT_NOTIFICATION";
    
    /**
	 * Property indicating the location of SPID
	 */
	public static final String SPID_LOC = "@context.SPID";
	
	public static final String MULTITN_SEQUENCEID = "MULTITN_MESSAGE_ID";

	// location of MessageSubRequestType in context
	public static final String SOA_SUBREQUEST_TYPE_LOC = "@context.REQUEST_HEADER.Subrequest";

	/**
	 * Property indicating the location of SQL_QUERY_TIMEOUT
	 */
	public static final String SQL_QUERY_TIMEOUT_PROP = "SQL_QUERY_TIMEOUT";
	
	public static final String NPAC_REQUEST_SUCCESS_REPLY = "NPACRequestSuccessReply";
	
	public static final String NPAC_REQUEST_FAILURE_REPLY = "NPACRequestFailureReply";

	public static final Object LASTPROCESSING_COL = "LASTPROCESSING";
	
	public static final String GET_PRIMARY_SPID_QUERY = "SELECT PROPERTYVALUE FROM PERSISTENTPROPERTY WHERE KEY IN (SELECT KEY FROM PERSISTENTPROPERTY WHERE PROPERTYTYPE = 'NPAC Com Server' AND PROPERTYVALUE = ? ) AND PROPERTYNAME='DEFAULT_PRIMARY_SPID'";
	
	//Changes for 5.6.5 release (NANC 441 req)
	public static final String NNSP_SIMPLEPORTINDICATOR_COL = "NNSPSIMPLEPORTINDICATOR";
	public static final String ONSP_SIMPLEPORTINDICATOR_COL = "ONSPSIMPLEPORTINDICATOR";
    public static final String MEDIUM_TIMER_TYPE = "medium";
    public static final String CANADA_REGION = "7";
    public static final String ACTIVATETIMEREXPIRATION_COL = "ACTIVATETIMEREXPIRATION";
    public static final String CONTEXT_ROOT_NODE_NNSPSIMPLEPORTINDICATOR = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/NNSPSimplePortIndicator";
    public static final String CONTEXT_ROOT_NODE_ONSPSIMPLEPORTINDICATOR = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/ONSPSimplePortIndicator";

    public static final String CONTEXT_ROOT_NODE_NPB_ID = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/NumberPoolBlockModifyRequest/BlockId";

    public static final String LASTALTERNATIVESPID_COL = "LASTALTERNATIVESPID";
    public static final String CONTEXT_ROOT_NODE_LASTALTERNATIVESPID = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/LastAlternativeSPID";
    // end 5.6.5 changes
    
    public static final String ATTRIBUTECHANGEINVOKEID_COL = "LASTATTRIBUTECHANGEINVOKEID";
    public static final String STATUSCHANGEINVOKEID_COL = "LASTSTATUSCHANGEINVOKEID";
    
    public static final String INVOKEID = "@context.InvokeId";
    
    public static final String IS_BDDNOTIFICATION = "@context.is_bddnotification";
    
    //Added in SOa 5.6.7.1 
    public static final String IS_RECOVERYREPLYNOTIFICATION = "@context.is_RecoveryReplyNotification";

    public static final String USE_INVOKEID_PROP = "USE_INVOKEID_PROP";
    
    //Added in SOA 5.6.6
	public static String SEA_POOL_ID = "SEADB";
	public static String SEA_DBNAME = "SEA_DBNAME";
	public static String SEA_DBUSER = "SEA_DBUSER";
	public static String SEA_DBPASSWORD = "SEA_DBPASSWORD";
	public static String DBDRIVER="DBDRIVER";
	public static String SEA_DBPropFilePath = "DB_PROPFILE_PATH";
	public static String PORTINGTN_LOC = "PORTINGTN_LOC";
	public static String SUBREQUESTTYPE = "SUBREQUESTTYPE";
	public static String OLDSPAUTHORIZATION_PROP = "OLDSPAUTHORIZATION";
	public static String OUTPUT_CUSTID_LOC = "OUTPUT_CUSTID_LOC";
	public static String SOA_SOAP_URL = "SOA_SOAP_URL";
	public static String TN_ENDSTATION_PROP = "EndStation";
	public static String SOA_REQUEST_TEMPLATE_PATH = "SOA_REQUEST_TEMPLATE_PATH";
	public static String START_SVID = "StartId";
	public static String END_SVID = "EndId";
	
	//Added in SOA 5.8
	public static String NTS_POOL_ID = "NTSDB";
	public static String NTS_DBNAME = "NTS_DBNAME";
	public static String NTS_DBUSER = "NTS_DBUSER";
	public static String NTS_DBPASSWORD = "NTS_DBPASSWORD";
	public static String OMS_CUSTID = "OMS_CUSTID";
	public static String OLD_SP_NODE = "OldSP";
	public static String LNP_TYPE_NODE = "LnpType";
	public static String NNSP_SIMPLEPORTINDICATOR_NODE = "NNSPSimplePortIndicator";
	public static String NTS_DB_PACKAGE_NAME = "NTS_DB_PACKAGE_NAME";
	public static String PORTPS_WS_URL = "PORTPS_WS_URL";
	public static String PORTPS_WS_USERNAME = "PORTPS_WS_USERNAME";
	public static String PORTPS_WS_PASSWORD = "PORTPS_WS_PASSWORD";	
	public static String NTS_LOOKUP_TIMEOUT = "NTS_LOOKUP_TIMEOUT";
	public static String PORTPS_LOOKUP_TIMEOUT = "PORTPS_LOOKUP_TIMEOUT";
	public static String NTS_RETRY_COUNT = "NTS_RETRY_COUNT";
	public static String PORTPS_RETRY_COUNT = "PORTPS_RETRY_COUNT";
	
	public static String NEW_SP_NODE = "NewSP";
	public static final String SPCUSTOM1_COL = "SPCUSTOM1";
	public static final String SPCUSTOM2_COL = "SPCUSTOM2";
	public static final String SPCUSTOM3_COL = "SPCUSTOM3";
		
	public static final String LastRequest = "SubRequest";
	
	/**
     * Boolean property indicating whether SV unique index exception occurred
     */
     public static final String IS_SV_UNIQUE_EXCEPTION = "@context.IsSvUniqueExp";
     
     /**
     * Property indicating the SV unique index error message
     */
     public static final String SV_DUPLICATE_ERROR_MSG = "@context.SvDupError";
     
     /**
     * Property indicating SV duplicate error count
     */
     public static final String SV_DUPLICATE_ERROR_COUNT = "@context.SvDupErrCount";

}
