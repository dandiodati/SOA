package com.nightfire.spi.neustar_soa.utils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public class SubRanges {
/**
 * This static method is used for getting the SubRanges for a given RangeId and SessionId
 * @param rangeId as long
 * @param sessionId as String
 * @throws ProcessingException
 */
	public static void getSubRanges(long rangeId, String sessionId) throws ProcessingException {

		Connection dbConn = null;
		CallableStatement proc = null;

		try {
			// Get a database connection from the appropriate
			// location - based
			// on transaction characteristics.

			dbConn = DBConnectionPool.getInstance().acquireConnection();

			if (dbConn == null) {
				// Throw the exception to the driver.
				throw new ProcessingException("DB "
						+ "connection is not available");
			}
		} catch (FrameworkException fe) {
			String errMsg = "ERROR: SOAAccountSVUpdate:"
					+ " Attempt to get database connection"
					+ " failed with error: " + fe.getMessage();

			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS )){
				Debug.log(Debug.ALL_ERRORS, errMsg);
			}
			
			throw new ProcessingException(fe.getMessage());
		}

		try {
			proc = dbConn.prepareCall("{ call GET_SUBRANGES(?, ?) }");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				Debug.log(Debug.MSG_STATUS, "Value for slot [1] is [" + rangeId	+ "]");
			}			
			proc.setLong(1, rangeId);
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				Debug.log(Debug.MSG_STATUS, "Value for slot [2] is [" + sessionId + "]");
			}			
			proc.setString(2, sessionId);
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				Debug.log(Debug.MSG_STATUS, "Executing Stored Procedure");
			}
			
			proc.execute();
		} catch (SQLException sqlex) {
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS )){
				Debug.log(Debug.ALL_ERRORS,DBInterface.getSQLErrorMessage(sqlex));
			}
			Debug.logStackTrace(sqlex);
			// throw new IMProcessingException(e.getMessage() );
		} finally {
			try {
				if (proc != null)
					proc.close();
			} catch (Exception e) {
				Debug.logStackTrace(e);
				
				if ( Debug.isLevelEnabled( Debug.ALL_WARNINGS )){
					Debug.log(Debug.ALL_WARNINGS, "Unable to close the Statement");
				}
				
			} finally {
				try {
					DBConnectionPool.getInstance()
								.releaseConnection(dbConn);
				} catch (ResourceException re) {
					Debug.logStackTrace(re);
					if ( Debug.isLevelEnabled( Debug.ALL_WARNINGS )){
						Debug.log(Debug.ALL_WARNINGS, "Unable to return DB connection.");
					}
				}
			}
		}
	}

	 /**
	 * The main is method is used for Testing
	 *
	 	 * @param  args
	 *
	 * @return  void
	 *
	 * @exception  ProcessingException  Thrown if processing fails.	 
	 */
	public static void main(String[] args) {
		try {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				Debug.log(Debug.MSG_STATUS, "Executing main method");
			}
			
			SubRanges.getSubRanges(490L, "1");			
		} catch (ProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
