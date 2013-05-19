package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.StringTokenizer;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentSequence;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.NancPropException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
/**
 * 
 * This is first message processor in Validate Gateway chain and responsible to
 * logs input message into SOA_REQUEST_MESSAGE table submitted by Customer
 * without any validation and formatting.
 * 
 */
public class SOAInitialRequestLogger extends DBMessageProcessorBase {

	/**
	 * This variable contains MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;

	/**
	 * This variable contains MessageObject object
	 */
	private MessageObject inputObject = null;

	private static final String CLASSNAME = "SOAInitialRequestLogger";

	/**
	 * The value of SPID
	 */
	private String spid = null;

	private String spidProp = null;

	/**
	 * Input message location
	 */
	private String inputMessage = null;

	/**
	 * This variable used to get the value of subrequest Type
	 */
	private String subRequestType = null;

	private PreparedStatement soaRequestMessageInsertStmt = null;

	private final static String SOA_REQUEST_MESSAGE_TABLE = "SOA_REQUEST_MESSAGE";

	private final static String REQUEST_HEADER = "REQUEST_HEADER";

	//changes made in 5.6.2 release
	// added INITIALMESSAGEID column
	public static final String SOA_REQUEST_MESSAGE_INSERT = "insert into "
			+ SOA_REQUEST_MESSAGE_TABLE
			+ "(INITIALMESSAGEID,MESSAGESUBTYPE, SPID, HEADER, MESSAGE, DATETIME)"
			+ " values (?,?,?,?,?,?)";

	private String message = null;

	private String messageHeaderProp = null;

	private String messageHeader = null;

	// changes made in 5.6.2 release
	// variables for initial message id generated in SOA INITIAL REQUEST LOGGER Message processor.
	private static final String INITIAL_MESSAGE_ID = "INITIAL_MESSAGE_ID";
	private String initMessageID_loc = null;
	private String initMessageID = null;

	// Changes made in 5.6.4
	// output location variable used to store value in context variable 
	//and will be store in SOA_SV_Message table in case of Normal BR.
	private String inputLocTn = null;
	private String inputLocEndStation = null;
	private String outputLocPortedTn = null;
	private String outputLocSPID = null;
	private static final String OUTPUT_LOC_SPID = "OUTPUT_LOC_SPID";
	//changes for TD#10281
	private boolean isTnRangeSubmition = false;
	private boolean isTnSubmition = false;
	
	//Added BPELSPID flag support in 5.6.4.1
	private static final String OUTPUT_LOC_BPELSPIDFLAG = "OUTPUT_LOC_BPELSPIDFLAG";
	private String outputLocBpelSpid = null;
	
	/**
	 * Initializes this object via its persistent properties.
	 * 
	 * @param key
	 *            Property-key to use for locating initialization properties.
	 * @param type
	 *            Property-type to use for locating initialization properties.
	 * 
	 * @exception ProcessingException
	 *                when initialization fails
	 */
	public void initialize(String key, String type) throws ProcessingException {

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, CLASSNAME + ", initialize() method.");
		}

		super.initialize(key, type);
			
		// get the Message form configuration property.
		inputMessage = getPropertyValue(SOAConstants.INPUT_MSG_LOC);

		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG, CLASSNAME
					+ ", Value of inputMessage:[" + inputMessage + "].");
		}

		// get the Message Header form configuration property.
		messageHeaderProp = getPropertyValue(REQUEST_HEADER);

		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG, CLASSNAME
					+ ", Value of messageHeader Prop:[" + messageHeaderProp
					+ "].");
		}

		// get the SPID form configuration property.
		spidProp = getPropertyValue(SOAConstants.SPID);
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG, CLASSNAME + ", Value of SPID:["
					+ spidProp + "].");
		}

		// changes made in 5.6.2 release	
		// get the location of initial messgaeId form configuration property.
		initMessageID_loc = getPropertyValue(INITIAL_MESSAGE_ID);
		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug.log(Debug.SYSTEM_CONFIG, "Value of INITIAL_MESSAGE_ID:["
					+ initMessageID_loc + "].");
		}

		if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
			Debug
					.log(Debug.SYSTEM_CONFIG, CLASSNAME
							+ ", Initialization done.");
		}

		inputLocTn = getPropertyValue( 
				SOAConstants.INPUT_LOC_TN_PROP);

		inputLocEndStation = getPropertyValue( 
						SOAConstants.INPUT_LOC_END_STATION_PROP);
		
		outputLocPortedTn =	getPropertyValue(
		SOAConstants.OUTPUT_LOC_PORTED_TN_PROP);
		
		outputLocSPID =	getPropertyValue(
				OUTPUT_LOC_SPID);
		
		outputLocBpelSpid = getPropertyValue(OUTPUT_LOC_BPELSPIDFLAG);
	}

	/**
	 * Extract data values from the context/input, and use them to insert a
	 * row(s) into the SOA_REQUEST_MESSAGE database table.
	 * 
	 * @param mpContext
	 *            The context
	 * @param inputObject
	 *            Input message to process.
	 * 
	 * @return The given input, or null.
	 * 
	 * @exception ProcessingException
	 *                Thrown if processing fails.
	 * @exception MessageException
	 *                Thrown if message is bad.
	 */

	public NVPair[] process(MessageProcessorContext mpcontext,
			MessageObject inputobject) throws MessageException,
			ProcessingException {

		ThreadMonitor.ThreadInfo tmti = null;

		if (inputobject == null) {
			return null;
		}

		this.mpContext = mpcontext;
		this.inputObject = inputobject;

		Connection dbConn = null;

		try {

			tmti = ThreadMonitor.start("Message-processor [" + getName()
					+ "] started processing the request");

			dbConn = DBConnectionPool.getInstance(true).acquireConnection();
			if (dbConn == null) {
				// Throw the exception to the driver.
				throw new ProcessingException("DB connection is not available");
			}
			// changes made in 5.6.2 release
			// Get the initial Message ID value from sequence
			initMessageID = Integer.toString(PersistentSequence
					.getNextSequenceValue(SOAConstants.MULTITN_SEQUENCEID,
							false, dbConn));
			
			if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
				Debug.log(Debug.NORMAL_STATUS, CLASSNAME
						+ ", Generated InitialMessageId :- "+initMessageID);
			}
			//changes made in 5.6.2 release
			// setting initial Message id in context
			set(initMessageID_loc, mpContext, inputObject, initMessageID);

			// Changes made for 5.6.4 release (TD# 10281)
			String tnValue =  getValue(inputLocTn);
			String endStationValue =
				 getValue(inputLocEndStation);
			// Check for TnRange Submition
			if(isTnRangeSubmition && isTnSubmition)
			{
				tnValue=null;
			}
			else
			{
				if(endStationValue!=null)
				{
					// append EndStation value if Tn is valid.
					if(SOAUtility.validateTelephoneNo(tnValue))
					{
						tnValue = tnValue+"-"+endStationValue;
					}
					else
					{
						tnValue=null;
					}
				}
			}
			boolean isValidTn = true;
			if(tnValue!=null)
			{
				tnValue = tnValue.replaceAll(",",";");
				StringTokenizer tnTokens = new StringTokenizer(tnValue, ";");
				String tn="";
				int totalToken = tnTokens.countTokens();
				// Check if request is submitted as Multi Tn
				if(totalToken > 0)
				{	
					for(int i=0; i < totalToken; i++)
					{
				        tn = tnTokens.nextElement().toString();
				        // if request is submitted as ;;305-584-6000, so its ignore blank ";" & doesnot set flag as isValidTn = false
				        if(tn.trim().length() > 0)
				        {
					      	if((tn.length() == 17 || tn.length()==12) && SOAUtility.validateTelephoneNo(tn))
							{
					      		// store only first valid tn in context
					      		if(i==0)
					      		{
									if(tn.length() == 12)
										tnValue = tn+"-"+tn.substring(8,12);
									else
										tnValue = tn;
					      		}
							}
					      	else // set isValidTn = false, only for inValid Tn
					      	{
					      		isValidTn = false;
					      		break;
					      	}
				        }
					}
				}
				else
				{
					// checking Single valid tn number
					if((tnValue.length() == 17 || tnValue.length()==12) && SOAUtility.validateTelephoneNo(tnValue))
					{
						if(tnValue.length() == 12)
							tnValue = tnValue+"-"+tnValue.substring(8,12);
					}
					else
					{
						isValidTn = false;
					}
				}
				
				//only valid tn will be set in context
				if(isValidTn)
				{
					set(outputLocPortedTn, mpContext, inputObject, tnValue);
				}
				
			}	
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			{
				Debug.log(Debug.MSG_STATUS, CLASSNAME
						+ ", portedTn :- "+tnValue);
			}
			//	end (TD# 10281)
			
			try {
				// extracting spid
				spid = getString(spidProp, mpContext, inputObject);
			} catch (Exception me) {
				if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
					Debug
							.log(
									Debug.SYSTEM_CONFIG,
									CLASSNAME
											+ ",  Exception when getting value for spid property  :\n"
											+ me.toString());
				}

			}
			//changes made in 5.6.4 for TD#10186
			if(spid!=null && spid.length() == 4)
			{
				set(outputLocSPID, mpContext, inputObject, spid);
			}
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			{
				Debug.log(Debug.MSG_STATUS, CLASSNAME
						+ ", SPID :- "+spid);
			}	
			
			// Added in SOA 5.6.4.1
			// Fetching the BPELSPID support flag from SOA_NANC_SUPPOR_FLAGS
			// table and set to the context.
			
			String bpelSpidSupportFlag = null;
			
			if(spid!=null && spid.length() == 4){
				bpelSpidSupportFlag = String.valueOf(NANCSupportFlag.getInstance(spid).getBpelSpidFlag());
			}									
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS, CLASSNAME
						+ ", bpelSpidSupportFlag for spid [" + spid + "] is: ["
						+ bpelSpidSupportFlag + "]");
			}
			
			if(bpelSpidSupportFlag != null){
				set(outputLocBpelSpid, mpcontext, inputobject, bpelSpidSupportFlag);				
			}
			
			try {
				// extracting message header
				messageHeader = getString(messageHeaderProp, mpContext,
						inputObject);
			} catch (Exception me) {
				if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
					Debug
							.log(
									Debug.SYSTEM_CONFIG,
									CLASSNAME
											+ ", Exception when getting value for messageHeader property  :\n"
											+ me.toString());
				}

			}
			
			XMLMessageParser inputParser = new XMLMessageParser(messageHeader);
			Debug.log(Debug.MSG_STATUS, "header.InputSource["+inputParser.getMessage()+"]");
			if(inputParser.exists("InputSource")){
				
				String inputSource = inputParser.getValue("InputSource");
				Debug.log(Debug.MSG_STATUS, "header.InputSource["+inputSource+"]");
				
				CustomerContext.getInstance().set("InputSource", inputSource);
				
			}else{
				Debug.log(Debug.MSG_STATUS, "header.InputSource not found so setting it with [A]");
				CustomerContext.getInstance().set("InputSource", "A");
			}
			// end Td#10186			
			// getting value of spid level flag for logging initial request in SOA_REQUEST_MESSAGE table.
			String logReqFlag = null;
			logReqFlag = String.valueOf(NANCSupportFlag.getInstance(spid)
					.getLogInitMessageFlag());

			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS, CLASSNAME
						+ ", LogInitMessage Flag for spid [" + spid + "] is: ["
						+ logReqFlag + "]");
			}
			
			// if flag is on (1)
			if (logReqFlag != null && "1".equals(logReqFlag)) {

				try {
					// input message
					message = getString(inputMessage, mpContext, inputObject);
				} catch (Exception me) {
					if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
						Debug
								.log(
										Debug.SYSTEM_CONFIG,
										CLASSNAME
												+ ", Exception when getting value for inputMessage property :\n"
												+ me.toString());
					}
				}

				try {
					// changes made in 5.6.2 release
					// taking value from header xml
					subRequestType = getString(
							SOAConstants.SOA_SUBREQUEST_TYPE_LOC, mpContext,
							inputObject);
					if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
						Debug.log(Debug.NORMAL_STATUS, CLASSNAME
								+ ", SubRequestType :- " + subRequestType);
					}
					if (subRequestType == null) {
						if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
							Debug
									.log(
											Debug.SYSTEM_CONFIG,
											CLASSNAME
													+ ", Didn't find MessageSubRequestType in header xml");
						}
					}
				} catch (Exception e) {
					if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
						Debug
								.log(
										Debug.SYSTEM_CONFIG,
										CLASSNAME
												+ ",  Didn't find MessageSubRequestType in header xml :\n"
												+ e.toString());
					}
				}
				
				
				try {

					// insert into SOA_Request_Message table
					insertSoaRequest(dbConn);

					// commiting changes
					DBConnectionPool.getInstance(true).commit(dbConn);

				} catch (SQLException sqlex) {

					String errMsg = CLASSNAME
							+ ", ERROR: Attempt to log to database "
							+ "failed with error: " + sqlex.getMessage();

					Debug.log(Debug.ALL_ERRORS, errMsg);

					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug.log(Debug.MSG_STATUS, CLASSNAME
								+ ", Rolling-back any database changes done.");
					}
					try {
						DBConnectionPool.getInstance(true).rollback(dbConn);
					} catch (ResourceException re) {
						Debug.log(Debug.ALL_ERRORS, CLASSNAME + ", "
								+ re.getMessage());
					}
				} catch (FrameworkException fe) {

					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug.log(Debug.MSG_STATUS, CLASSNAME
								+ ", Rolling-back any database changes done");
					}
					try {
						DBConnectionPool.getInstance(true).rollback(dbConn);
					} catch (ResourceException re) {
						Debug.log(Debug.ALL_ERRORS, re.getMessage());
					}
				} finally {

					try {
						if (soaRequestMessageInsertStmt != null) {
							soaRequestMessageInsertStmt.close();
							soaRequestMessageInsertStmt = null;
						}

					} catch (SQLException sqle) {
						Debug.log(Debug.ALL_ERRORS, CLASSNAME + ", "
								+ DBInterface.getSQLErrorMessage(sqle));
					}

				}

			} else// flag off (0)
			{
				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug
							.log(
									Debug.MSG_STATUS,
									CLASSNAME
											+ ", For spid "
											+ spid
											+ ", LogInitMessage Flag is "
											+ logReqFlag
											+ "[ off ], so request message will not be logged in "
											+ SOA_REQUEST_MESSAGE_TABLE
											+ " table");
				}
			}

		} catch (NancPropException e) {

			if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
				Debug.log(Debug.SYSTEM_CONFIG, CLASSNAME + ", "
						+ e.getMessage());
			}
		} catch (Exception e) {
			if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG)) {
				Debug.log(Debug.SYSTEM_CONFIG, CLASSNAME + ", Error : "
						+ e.getMessage());
			}
		} finally {

			ThreadMonitor.stop(tmti);

			try {
				DBConnectionPool.getInstance(true).releaseConnection(dbConn);
				dbConn = null;
			} catch (Exception e) {
				Debug.log(Debug.ALL_ERRORS, CLASSNAME + ", " + e.toString());
			}

		}
		return (formatNVPair(inputObject));
	}

	/**
	 * insert message in SOA_REQUEST_MESSAGE table
	 * 
	 * @param dbConn
	 * @throws SQLException
	 * @throws DatabaseException 
	 */
	private void insertSoaRequest(Connection dbConn) throws SQLException,
			DatabaseException {
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, CLASSNAME
					+ ", insertSoaRequest() method.");
		}

		soaRequestMessageInsertStmt = dbConn
				.prepareStatement(SOA_REQUEST_MESSAGE_INSERT);

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
			Debug.log(Debug.NORMAL_STATUS, CLASSNAME
					+ ", Insert query for SOA_REQUEST_MESSAGE: \n"
					+ SOA_REQUEST_MESSAGE_INSERT);
		}

		// inserting initialMessageID, changes made in 5.6.2 release
		soaRequestMessageInsertStmt.setString(1, initMessageID);
		soaRequestMessageInsertStmt.setString(2, subRequestType);
		soaRequestMessageInsertStmt.setString(3, spid);
		soaRequestMessageInsertStmt.setString(4, messageHeader);
		soaRequestMessageInsertStmt.setString(5, message);
		Date datetime = new Date();
		soaRequestMessageInsertStmt.setTimestamp(6, new Timestamp(datetime
				.getTime()));

		soaRequestMessageInsertStmt.execute();

		if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {

			Debug.log(Debug.NORMAL_STATUS, CLASSNAME + ", Request Message has "
					+ "inserted into the " + SOA_REQUEST_MESSAGE_TABLE
					+ "  Table with InitialMessageId :- "+initMessageID);

		}
	}
	
	/**
	 * This method tokenizes the input string and return an
	 * object for exsisting value in context or messageobject.
	 *
	 * @param  locations as a string
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  object
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	private String getValue(
		String locations)
		throws MessageException, ProcessingException {

		StringTokenizer st =
			new StringTokenizer(locations, MessageProcessorBase.SEPARATOR);

		String tok = null;
		while (st.hasMoreTokens()) {

			tok = st.nextToken();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"Checking location [" + tok + "] for value ...");
			}

			if (exists(tok, mpContext, inputObject))
			{
				// Changes for TD#10281
				if(tok.indexOf("Subscription.Tn") > -1 || tok.indexOf("Subscription.Sv.Tn") > -1)
				{
					isTnSubmition = true;
				}
				if(tok.indexOf("TnRange") > -1 )
				{
					isTnRangeSubmition = true;
				}
				// End TD#10281
				return (String)(get(tok, mpContext, inputObject));
			}
		}

		return null;
	}
}
