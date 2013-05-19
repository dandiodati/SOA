package com.nightfire.spi.neustar_soa.queue;

import java.sql.*;

import com.nightfire.spi.neustar_soa.adapter.NPACComServer;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;

import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;

import com.nightfire.mgrcore.queue.QueueConstants;

public class NPACQueueUtils {

   private static String UPDATE = "UPDATE "+
                                  SOAConstants.NPAC_QUEUE_TABLE+
                                  " SET "+
                                  SOAConstants.STATUS_COL+
                                  " = ? WHERE "+
                                  SOAConstants.MESSAGEKEY_COL+
                                  " = ?";

   private static String UPDATE_LAST_ERROR = "UPDATE "+
                                  SOAConstants.NPAC_QUEUE_TABLE+
                                  " SET "+
                                  SOAConstants.LASTERRORMESSAGE_COL+
                                  " = ? WHERE "+
                                  SOAConstants.MESSAGEKEY_COL+
                                  " = ?";

   private static String RETRY_ALL_SENT_MESSAGES =
                                               "UPDATE "+
                                               SOAConstants.NPAC_QUEUE_TABLE+
                                               " SET "+
                                               SOAConstants.STATUS_COL+
                                               " = '"+
                                               QueueConstants.RETRY_STATUS+
                                               "' WHERE "+
                                               SOAConstants.STATUS_COL+
                                               " = '"+
                                               SOAConstants.SENT_STATUS+"'"+
                                               " AND ARRIVALTIME > SYSDATE-";

   private static String DELETE = "DELETE FROM "
								+ SOAConstants.NPAC_QUEUE_TABLE + " WHERE "
								+ SOAConstants.MESSAGEKEY_COL + " = ?";
   
   /**
    * Performs an update on the queue, updating the message's status to the
    * given value.
    *
    * @param messageKey the param to uniquely identify the record to be
    *                   updated.
    * @param status String the new status value.
    */
   public static void updateStatus(String messageKey, String status){
      update(messageKey, status, UPDATE);
   }

   public static void updateLastErrorMessage(String messageKey, String status){
      update(messageKey, status, UPDATE_LAST_ERROR);
   }
   
   public static void deleteOnSuccess(String messageKey) {
		delete(messageKey, DELETE);
	}
   
   private static void update(String messageKey,
                              String status,
                              String statement){

      try{

         Connection conn = DBInterface.acquireConnection();

         PreparedStatement pstmt = null;

         try {

            pstmt = conn.prepareStatement(statement);

            pstmt.setString(1, status);
            pstmt.setString(2, messageKey);

            int updateCount = pstmt.executeUpdate();

            if( updateCount != 1 ){

               Debug.log(Debug.ALL_WARNINGS,
                         "Queue update for message with message key ["+
                         messageKey+"] updated "+updateCount+" records.");

            }

            conn.commit();

         }
         catch (SQLException sqlex) {
            Debug.error("Could not update status to ["+status+
                        "] for message key ["+messageKey+
                        "]: "+sqlex);
         }
         finally{

            close( pstmt );
            DBInterface.releaseConnection( conn );

         }

      }
      catch(DatabaseException dbex){
         Debug.error("Could not update status to ["+status+
                     "] for message key ["+messageKey+
                     "]: "+dbex);
      }

   }
   
   private static void delete(String messageKey, String statement) {

		try {

			Connection conn = DBInterface.acquireConnection();

			PreparedStatement pstmt = null;

			try {

				pstmt = conn.prepareStatement(statement);

				pstmt.setString(1, messageKey);

				int deleteCount = pstmt.executeUpdate();

				if (deleteCount != 1) {

					Debug.log(Debug.ALL_WARNINGS,
							"Queue delete for message with message key ["
									+ messageKey + "] updated " + deleteCount
									+ " records.");

				}

				conn.commit();

			} catch (SQLException sqlex) {
				Debug.error("Could not able to delete the message key [" + messageKey + "]: " + sqlex);
			} finally {

				close(pstmt);
				DBInterface.releaseConnection(conn);

			}

		} catch (DatabaseException dbex) {
			Debug.error("Could not able to delete the message key [" + messageKey + "]: " + dbex);
		}

	}
   
   /**
    * This resets the status of all 'sent' messages in the NPAC queue to
    * have a status of retry,
    */
   public static void retryAllSentMessages(String pickTime){

      try{

         Connection conn = DBInterface.acquireConnection();

         PreparedStatement pstmt = null;

         try {
        	 
			long time_in_mins = Long.parseLong(pickTime);
			
			//converting minutes in number of days
			long time_in_days = time_in_mins/1440;
			 
			String retryQuery = RETRY_ALL_SENT_MESSAGES + time_in_days;
			 
			pstmt = conn.prepareStatement(retryQuery);
			
			int updateCount = pstmt.executeUpdate();
			
			conn.commit();
			Debug.log(Debug.ALL_WARNINGS,
			          "SQL query: ["+retryQuery+"] and only ["+ time_in_days +"] days Records will be reset to retry");
			
			Debug.log(Debug.ALL_WARNINGS,
			          "Reset ["+updateCount+"] sent records to retry.");


         }
         catch (SQLException sqlex) {
            Debug.error("Could not update status to ["+
                        QueueConstants.RETRY_STATUS+
                        "] for all sent messages: "+
                        sqlex);
         }
         finally{

            close( pstmt );
            DBInterface.releaseConnection( conn );

         }

      }
      catch(DatabaseException dbex){
            Debug.error("Could not update status to ["+
                        QueueConstants.RETRY_STATUS+
                        "] for all sent messages: "+
                        dbex);
      }

   }
    /**
    * Close the given prepared statement. This is a convenience method that
    * with check to see that the pstmt is not null and will handle the
    * exception that the can be thrown when closing the statement.
    *
    * @param pstmt PreparedStatement
    */
   private static void close(PreparedStatement pstmt){

      if( pstmt != null ){

         try{

            pstmt.close();

         }
         catch(SQLException sqlex){
            Debug.error("Could not close prepared statement: "+
                        sqlex.toString());
         }

      }

   }
   /**
    * This retrieve the Connectivity key from persistent property table
    * 
    * @param spid the param to uniquely identify the record
    */
	public static String getConnectivityKey(String spid) {
		String connectivityKey = null;
		try {
			Connection conn = DBInterface.acquireConnection();
			PreparedStatement pstmt = null;
			ResultSet rs = null;

			try {
				pstmt = conn
						.prepareStatement(SOAQueryConstants.CONNECTIVITY_SELECT);

				pstmt.setString(1, spid);

				rs = pstmt.executeQuery();
                
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
			         Debug.log(Debug.MSG_STATUS,
							"Executed query:[ " + SOAQueryConstants.CONNECTIVITY_SELECT + "]");			         
			      }
				if (rs.next()) {
					connectivityKey = rs.getString(1);
				}
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
			         Debug.log(Debug.MSG_STATUS,
							"Connectivity key =["+ connectivityKey +"]for spid: " + spid + "");			         
			      }
			} catch (SQLException sqlex) {
				Debug.error("Could not select key for spid [" + spid + "] "
						+ sqlex);
			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException sqlex) {
						Debug.error("Could not close result set: "
								+ sqlex.toString());
					}
				}
				close(pstmt);
				DBInterface.releaseConnection(conn);
			}

		} catch (DatabaseException dbex) {
			Debug.error("Could not select key for spid [" + spid + "] " + dbex);
		}
		return connectivityKey;
	}
}