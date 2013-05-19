/**
 * Copyright (c) 2006, NeuStar, Inc. All Rights Reserved.
 *
 * $Header: //spi/neustar-soa/5.4/com/nightfire/spi/neustar_soa/servicehandler/SoaConditionalQueryServiceHandler.java
 */
package com.nightfire.spi.neustar_soa.servicehandler;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.mgrcore.businessobject.InvalidDataException;
import com.nightfire.mgrcore.im.*;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.sql.*;

/**
 * This class executes queries that are dynamically constructed based on the
 * input data and a query description obtained from the repository.
 * 
 * e.g. The repository configuraion XML is like:
 * 
 * 	<ServiceHandler>
 * 		<action value="query-soa-orders"/>
 *  	<ClassName value="com.nightfire.mgrcore.im.ConditionalQueryServiceHandler"/>
 * 		<PropertyContainer>
 * 			<Property>
 * 				<Name value="WHEN_TEST_0"/>
 * 				<Value value="//Info[PON/@value!='' and VER/@value!='']"/>
 * 			</Property>
 * 			<Property>
 * 				<Name value="WHEN_DEST_0"/>
 * 				<Value value="gw-soa-order_list_0"/>
 * 			</Property>
 * 			<Property>
 * 				<Name value="WHEN_TEST_0"/>
 * 				<Value value="//Info[PON/@value!='' or VER/@value!='']"/>
 * 			</Property>
 * 			<Property>
 * 				<Name value="WHEN_DEST_1"/>
 * 				<Value value="gw-soa-order_list_1"/>
 * 			</Property>
 * 			<Property>
 * 				<Name value="OTHERWISE_DEST"/>
 * 				<Value value="unsupported-service"/>
 * 			</Property>
 * 		</PropertyContainer>
 * 	</ServiceHandler>
 * 
 * WHEN_TEST conditions are XPath expressions. And WHEN_DEST are query names.
 * 
 * Iterative properties WHEN_TEST and WHEN_DEST help to determine Query to be
 * executed conditionally. Each of the when condition is tested in sequence. If
 * any of the when condition is satisfied, its corresponding query will be
 * executed. If no when condition is satisfied, the query defined by
 * OTHERWISE_DEST property will be executed as default.
 */
public class SoaConditionalQueryServiceHandler extends ChoiceHandler {
	/**
	 * Get the information from the database as specified by the query criteria
	 * in the requestBody.
	 * 
	 * @param context
	 *            IMContext Control information for the request.
	 * @param requestBody
	 *            Body of the request.
	 * 
	 * @return ResponseMessage A ResponseMessage object containing the response
	 *         code and the response body.
	 * 
	 * @exception IMInvalidDataException
	 *                Thrown if request data is bad.
	 * @exception IMSystemException
	 *                Thrown if server can't process any more requests due to
	 *                system errors.
	 * @exception IMSecurityException
	 *                Thrown if access is denied.
	 * @exception IMProcessingException
	 *                Thrown if a transient processing error occurs.
	 */

	/**
	 * The node whose value will be used to find the query definition.
	 */
	public static String nnspDueDateFrom = null;

	public static String nnspDueDateTo = null;

	public static String onspDueDateFrom = null;

	public static String onspDueDateTo = null;

	public static String createdDateFrom = null;

	public static String createdDateTo = null;

	@Override
	public ServiceHandler.ResponseMessage process(IMContext context,
			String requestBody) throws IMInvalidDataException,
			IMSystemException, IMSecurityException, IMProcessingException {
		try {
			XMLMessageParser reqParser = new XMLMessageParser(requestBody);

			// Get the query to be executed.
			String mappedAction = findNextAction(reqParser.getDocument());

			if (Debug.isLevelEnabled(IMConstants.IM_SYSTEM_CONFIG))
				Debug.log(IMConstants.IM_SYSTEM_CONFIG, "Executing query ["
						+ mappedAction + "].");

			context.setInvokingAction(mappedAction);
			requestBody = getRequestBodyWithLastupdate(requestBody);

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, " requestBody : " + requestBody);
			
			// Make a call to DynamicQueryServiceHandler for executing query.
			return new DynamicQueryServiceHandler().process(context,
					requestBody);
		}
		// if a manager exception is thrown from the next service handler then
		// just throw it instead of wrapping it with another exception.
		catch (IMInvalidDataException e) {
			throw e;
		} catch (IMProcessingException e) {
			throw e;
		} catch (IMSecurityException e) {
			throw e;
		} catch (IMSystemException e) {
			throw e;
		} catch (MessageException e) {
			throw new IMInvalidDataException(this.getClass().getName() + ": "
					+ e.toString());
		} catch (Exception e) {
			throw new IMProcessingException(this.getClass().getName() + ": "
					+ e.toString());
		}
	}

	/**
	 * This static method is used for executing selecting record from
	 * soa_subscription_version
	 * 
	 * @param requestBody
	 *            as String
	 * @throws DatabaseException
	 */

	public static String getRequestBodyWithLastupdate(String requestBody)
			throws IMInvalidDataException, IMSystemException,
			IMSecurityException, IMProcessingException, DatabaseException {

		Connection dbConn = null;
		Statement stmt = null;
		ResultSet results = null;
		String returnDateFrom = null;
		String returnDateTo = null;
		XMLPlainGenerator gen = null;
	        StringBuffer mainQuery = null;
		StringBuffer whereQuery = null;
		try {
			intializeParameters();
			XMLMessageParser parser = new XMLMessageParser(requestBody);
			if (parser.exists("Info.NewSPDueDate.From")) {
				nnspDueDateFrom = convertedDate(parser
						.getValue("Info.NewSPDueDate.From"));
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, "nnspDueDateFrom: "
							+ nnspDueDateFrom);
				}
			}
			if (parser.exists("Info.NewSPDueDate.To")) {
				nnspDueDateTo = convertedDate(parser
						.getValue("Info.NewSPDueDate.To"));
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, "nnspDueDateTo: " + nnspDueDateTo);
				}
			}
			if (parser.exists("Info.OldSPDueDate.From")) {
				onspDueDateFrom = convertedDate(parser
						.getValue("Info.OldSPDueDate.From"));
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, "onspDueDateFrom: " + onspDueDateFrom);
				}
			}
			if (parser.exists("Info.OldSPDueDate.To")) {
				onspDueDateTo = convertedDate(parser
						.getValue("Info.OldSPDueDate.To"));
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				
					Debug.log(Debug.MSG_STATUS, "onspDueDateTo: " + onspDueDateTo);
				}
				
			}
			if (parser.exists("Info.CreatedDate.From")) {
				createdDateFrom = convertedDate(parser
						.getValue("Info.CreatedDate.From"));
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, "createdDateFrom: "
							+ createdDateFrom);
				}
				
			}
			if (parser.exists("Info.CreatedDate.To")) {
				createdDateTo = convertedDate(parser
						.getValue("Info.CreatedDate.To"));
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					
					Debug.log(Debug.MSG_STATUS, "createdDateTo: " + createdDateTo);
				}
			}

			if (nnspDueDateFrom != null || nnspDueDateTo != null
					|| onspDueDateFrom != null || onspDueDateTo != null
					|| createdDateFrom != null || createdDateTo != null) {

				try {
					// Get a database connection from the appropriate
					// location - based
					// on transaction characteristics.

					dbConn = DBConnectionPool.getInstance().acquireConnection();

					if (dbConn == null) {
						// Throw the exception to the driver.
						throw new DatabaseException("DB "
								+ "connection is not available");
					}
				} catch (FrameworkException fe) {
					String errMsg = "ERROR:"
							+ " Attempt to get database connection"
							+ " failed with error: " + fe.getMessage();

					if ( Debug.isLevelEnabled( Debug.ALL_ERRORS )){
						Debug.log(Debug.ALL_ERRORS, errMsg);
					}

					throw new DatabaseException(fe.getMessage());
				}

				mainQuery = new StringBuffer();
				whereQuery = new StringBuffer();

				mainQuery.append("SELECT ");
				mainQuery.append("MIN (LASTUPDATE) AS LASTUPDATEFROM ");
				mainQuery.append(", ");
				mainQuery.append("MAX (LASTUPDATE)AS LASTUPDATETO ");
				mainQuery.append(" FROM SOA_SUBSCRIPTION_VERSION WHERE");
				
				if (nnspDueDateFrom != null && !nnspDueDateFrom.equals("")) {
					whereQuery.append(" NNSPDUEDATE >= TO_DATE('" + nnspDueDateFrom + "','MM-DD-YYYY-HHMISSPM')");
				}
				if (whereQuery.toString().length() > 1 && nnspDueDateTo != null	&& !nnspDueDateTo.equals("")) {
					whereQuery.append(" AND NNSPDUEDATE <= TO_DATE('"+ nnspDueDateTo + "','MM-DD-YYYY-HHMISSPM')");
				} else if (nnspDueDateTo != null && !nnspDueDateTo.equals("")) {
					whereQuery.append(" NNSPDUEDATE <= TO_DATE('"+ nnspDueDateTo + "','MM-DD-YYYY-HHMISSPM')");
				}
				if (whereQuery.toString().length() > 1 && onspDueDateFrom != null && !onspDueDateFrom.equals("")) {
					whereQuery.append(" AND ONSPDUEDATE >= TO_DATE('"  + onspDueDateFrom + "','MM-DD-YYYY-HHMISSPM')");
				} else if (onspDueDateFrom != null	&& !onspDueDateFrom.equals("")) {
					whereQuery.append(" ONSPDUEDATE >= TO_DATE('" + onspDueDateFrom + "','MM-DD-YYYY-HHMISSPM')");
				}
				if (whereQuery.toString().length() > 1 && onspDueDateTo != null	&& !onspDueDateTo.equals("")) {
					whereQuery.append(" AND ONSPDUEDATE <= TO_DATE('" + onspDueDateTo + "','MM-DD-YYYY-HHMISSPM')");
				} else if (onspDueDateTo != null && !onspDueDateTo.equals("")) {
					whereQuery.append(" ONSPDUEDATE <= TO_DATE('" + onspDueDateTo + "','MM-DD-YYYY-HHMISSPM')");
				}
				if (whereQuery.toString().length() > 1 && createdDateFrom != null && !createdDateFrom.equals("")) {
					whereQuery.append(" AND CREATEDDATE >= TO_DATE('" + createdDateFrom + "','MM-DD-YYYY-HHMISSPM')");
				} else if (createdDateFrom != null	&& !createdDateFrom.equals("")) {
					whereQuery.append(" CREATEDDATE >= TO_DATE('" + createdDateFrom + "','MM-DD-YYYY-HHMISSPM')");
				}
				if (whereQuery.toString().length() > 1 && createdDateTo != null	&& !createdDateTo.equals("")) {
					whereQuery.append(" AND CREATEDDATE <= TO_DATE('" + createdDateTo + "','MM-DD-YYYY-HHMISSPM')");
				} else if (createdDateTo != null && !createdDateTo.equals("")) {
					whereQuery.append(" CREATEDDATE <= TO_DATE('"+ createdDateTo + "','MM-DD-YYYY-HHMISSPM')");
				}
				mainQuery = mainQuery.append(whereQuery);
				
				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
					Debug.log(Debug.NORMAL_STATUS, "mainQuery: "+ mainQuery.toString());

				stmt = dbConn.createStatement();

				results = stmt.executeQuery(mainQuery.toString());
				while (results.next()) {
					if (results.getTimestamp(1) != null)
						returnDateFrom = results.getTimestamp(1).toString();
					if (results.getTimestamp(2) != null)
						returnDateTo = results.getTimestamp(2).toString();
				}
				if(returnDateFrom!=null)
				returnDateFrom = convertedDateFormat(returnDateFrom);
				if(returnDateTo!=null)
				returnDateTo = convertedDateTimeFormat(returnDateTo);

			} else {
				// Updated this part for date before 10 days from sysdate
				Date todayDate = new Date();
				long todayTime = todayDate.getTime();				
				long lastUpdateTime = todayTime - 864000000;
				
				Date LastUpdate = new Date(lastUpdateTime);				
				SimpleDateFormat sdfFrom = new SimpleDateFormat("MM-dd-yyyy");
				returnDateFrom = sdfFrom.format(LastUpdate);
				SimpleDateFormat sdfTo = new SimpleDateFormat("MM-dd-yyyy-hhmmssa");
				returnDateTo = sdfTo.format(todayDate);
			}
			
			if(returnDateFrom!=null)
			returnDateFrom = convertedDate(returnDateFrom);
			if(returnDateTo!=null)
			returnDateTo = convertedDate(returnDateTo);
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "returnDateFrom " + returnDateFrom);
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "returnDateTo: " + returnDateTo);
			
			gen = new XMLPlainGenerator(requestBody);
			if (returnDateFrom != null && !returnDateFrom.equals(""))
				gen.setValue("Info.LastUpdateDate.From", returnDateFrom);

			if (returnDateTo != null && !returnDateTo.equals(""))
				gen.setValue("Info.LastUpdateDate.To", returnDateTo);

			requestBody = gen.getOutput();
		} catch (InvalidDataException e) {
			throw new IMInvalidDataException(e.toString());
		} catch (MessageException e) {
			throw new IMInvalidDataException(e.toString());
		} catch (Exception e) {
			throw new IMProcessingException(e.toString());
		} finally {
			try {
				if (results != null)
					results.close();
				results = null;
				if (stmt != null)
					stmt.close();
				stmt = null;
				if (dbConn != null) {
					DBConnectionPool.getInstance().releaseConnection(dbConn);
				}
				dbConn = null;
				} catch (Exception e) {
				Debug.logStackTrace(e);
				
				if ( Debug.isLevelEnabled( Debug.ALL_WARNINGS ))
					Debug.log(Debug.ALL_WARNINGS, "Unable to close the Statement");
			}
		}
		return requestBody;
	}

	/**
	 * This static method is used to convert 17 digit date to 19 digit date. 
	 * @param str as input date string with 17 digit date.
	 * @return string with 19 digit date.
	 */

	public static String convertedDate(String str) {
		String inputDate = null;

		if (str.length() == SOAConstants.DATE_HHMI_LENGTH) {

			inputDate = str.substring(SOAConstants.DATE_START_INDEX,
					SOAConstants.DATE_MIDLLE_INDEX)
					+ SOAConstants.DATE_TIME_CONCATENATION
					+ str.toString().substring(SOAConstants.DATE_MIDLLE_INDEX,
							SOAConstants.DATE_END_INDEX);

		} else {
			inputDate = str.toString();
		}
		return inputDate;
	}

	/**
	 * This static method is used to convert inout date format[yyyy-MM-dd HH:mm:ss] in to output date format [MM-dd-yyyy].
	 * @param str as input date string with yyyy-MM-dd HH:mm:ss date format
	 * @return string with MM-dd-yyyy-hhmmssa date format.
	 */

	public static String convertedDateTimeFormat(String str) {
		String inputString = null;
		Date inputDate = null;
		SimpleDateFormat parseSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			inputDate = parseSDF.parse(str);
		} catch (ParseException e) {
			Debug.error("");
		}
		SimpleDateFormat formatSDF = new SimpleDateFormat("MM-dd-yyyy-hhmmssa");
		inputString = formatSDF.format(inputDate);
		return inputString;

	}
	/**
	 * This static method is used to convert inout date format[yyyy-MM-dd HH:mm:ss] in to output date format [MM-dd-yyyy].
	 * @param str as input date string with yyyy-MM-dd HH:mm:ss date format
	 * @return string with MM-dd-yyyy date format.
	 */
	
	public static String convertedDateFormat(String str) {
		String inputString = null;
		Date inputDate = null;
		SimpleDateFormat parseSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			inputDate = parseSDF.parse(str);
		} catch (ParseException e) {
			Debug.error("");
		}
		SimpleDateFormat formatSDF = new SimpleDateFormat("MM-dd-yyyy");
		inputString = formatSDF.format(inputDate);
		return inputString;

	}
	/**
	 * The static method to initialize parameters.
	 *
	 */
	public static void intializeParameters()
	{
		nnspDueDateFrom = null;
		nnspDueDateTo = null;
		onspDueDateFrom = null;
		onspDueDateTo = null;
		createdDateFrom = null;
		createdDateTo = null;
	}
}
