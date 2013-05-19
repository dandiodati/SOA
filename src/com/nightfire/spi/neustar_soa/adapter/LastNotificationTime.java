///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import java.sql.*;


import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;

/**
* This class provides access to the last notification time table in the
* database.
*/
public class LastNotificationTime {

   private static final String CUST_SPID_COL = "SPID";

   private static final String REGION_ID_COL = "REGIONID";

   private static final String DATE_COL = "DATETIME";

   private static final String TYPE_COL = "TYPE";

   /**
    * The value of the type column used to indicate that this is a
    * the last notification time for plain-old notifications.
    */
   private static final int DEFAULT_TYPE = 0;

   /**
    * The value of the type column used to indicate that this is a
    * the last notification time for network notifications.
    */
   private static final int NETWORK_TYPE = 1;
   
   /**
    * The value of the type column used to indicate that this is a
    * the last notification time for service providers notifications.
    */

   
   private static final int SERVICE_PROV_TYPE= 2;
   
   /**
   * The SQL statement used to delete any old last notification time
   * before inserting the new one.
   */
   private String delete;

   /**
   * This is the SQL statement used to insert a last notification time
   * into the DB.
   */
   private String insert;

   /**
   * This is the query used to retrieve the last notification time from
   * the DB.
   */
   private String select;

   /**
   * Constructor
   *
   * @param tableName the name of the last notification time DB table.
   */
   public LastNotificationTime(String tableName){

      // create the delete statement
      StringBuffer buffer = new StringBuffer("DELETE FROM ");
      buffer.append(tableName);
      buffer.append(" WHERE ");
      buffer.append(CUST_SPID_COL);
      buffer.append(" = ? AND ");
      buffer.append(REGION_ID_COL);
      buffer.append(" = ? AND ");
      buffer.append(TYPE_COL);
      buffer.append(" = ? ");
      delete = buffer.toString();

      // create the insert statement
      buffer = new StringBuffer("INSERT INTO ");
      buffer.append(tableName);
      buffer.append(" ( ");
      buffer.append(CUST_SPID_COL);
      buffer.append(", ");
      buffer.append(REGION_ID_COL);
      buffer.append(", ");
      buffer.append(DATE_COL);
      buffer.append(", ");
      buffer.append(TYPE_COL);
      buffer.append(") VALUES ( ?, ?, ?, ?)");
      insert = buffer.toString();

      // create the select statement
      buffer = new StringBuffer("SELECT ");
      buffer.append(DATE_COL);
      buffer.append(" FROM ");
      buffer.append(tableName);
      buffer.append(" WHERE ");
      buffer.append(CUST_SPID_COL);
      buffer.append(" = ? AND ");
      buffer.append(REGION_ID_COL);
      buffer.append(" = ? AND ");
      buffer.append(TYPE_COL);
      buffer.append(" = ? ");
      select = buffer.toString();

   }

   /**
   * This retrieves the last notification time for the given
   * customer SPID in the given region.
   *
   * @param customerSPID the ID for the customer to be queried.
   * @param region the region that should be queried for that customer.
   * @throws DatabaseException is the query fails for some reason.
   */
   public java.util.Date getLastNotificationTime(String customerSPID,
                                                 int region)
                                                 throws DatabaseException{

      return getLastNotificationTime(customerSPID,
                                     region,
                                     DEFAULT_TYPE);

   }

   /**
   * This retrieves the last notification time for the given
   * customer SPID in the given region.
   *
   * @param customerSPID the ID for the customer to be queried.
   * @param region the region that should be queried for that customer.
   * @throws DatabaseException is the query fails for some reason.
   */
   private java.util.Date getLastNotificationTime(String customerSPID,
                                                  int region,
                                                  int type)
                                                  throws DatabaseException{

      java.util.Date last = null;

      if( Debug.isLevelEnabled(Debug.DB_STATUS) ){

         Debug.log(Debug.DB_STATUS,
                   "Retrieving last "+
                   ((type == NETWORK_TYPE) ? "network " : "")+
                   "notification time for SPID ["+
                   customerSPID+
                   "] and region ["+region+"]");

      }

      Connection connection = DBInterface.acquireConnection();
      PreparedStatement statement = null;
      try{

         statement = connection.prepareStatement(select);

         statement.setString(1, customerSPID);
         statement.setString(2, Integer.toString(region));
         statement.setInt(3, type);

         ResultSet results = statement.executeQuery();

         if( results.next() ){

            last = results.getTimestamp(1);

         }
      }
      catch(SQLException sqlex){

         throw new DatabaseException(sqlex);

      }
      finally{
    	 try {
			statement.close();
		} catch (SQLException e) {
			Debug.log(Debug.ALL_ERRORS, "Error: error occured in closing the prepared statement" +
					", " + e.getMessage());
		}
         DBInterface.releaseConnection(connection);

      }

      if(last == null){

         Debug.warning("No last notification date found for SPID ["+
                       customerSPID+"] and region ["+region+"]");

      }
      else if(Debug.isLevelEnabled(Debug.DB_STATUS)){

         String date = TimeZoneUtil.convert(NPACConstants.UTC,
                                            NPACConstants.UTC_TIME_FORMAT,
                                            last);
         Debug.log(Debug.DB_STATUS,
                   "Retrieved last notification time ["+
                   date+
                   "] for SPID ["+
                   customerSPID+
                   "] and region ["+region+"]");

      }

      return last;

   }

   /**
   * This sets a last notification time based on the given
   * notification. The customer SPID, region, and notification
   * time are all retrieved from the given notification data
   * and inserted in the last notification time table.
   *
   * @param parsedNotification the parsed XML notification.
   * @param throws DatabaseException if the statement fails to instert the
   *               last notification time into the database.
   * @param throws MessageException if the required fields cannot be
   *                                found in the given notification.
   *
   */
   public void setLastNotificationTime(XMLMessageParser parsedNotification,
                                       boolean networkNotification)
                                       throws DatabaseException,
                                              MessageException{

      try{

         String spid = parsedNotification.getTextValue(NPACConstants.CUSTOMER_ID);

         String region =
            parsedNotification.getTextValue(NPACConstants.NPAC_REGION_ID);

         String time =
            parsedNotification.getTextValue(NPACConstants.MESSAGE_DATE_TIME);

         int type = (networkNotification) ? NETWORK_TYPE : DEFAULT_TYPE;

         if( Debug.isLevelEnabled(Debug.DB_STATUS) ){

            Debug.log(Debug.DB_STATUS,
                      "Setting last "+
                      ((type == NETWORK_TYPE) ? "network " : "")+
                      "notification time ["+
                      time+
                      "] for SPID ["+
                      spid+
                      "] and region ["+region+"]");

         }

         java.util.Date newTime =
                           TimeZoneUtil.parse( NPACConstants.UTC,
                                               NPACConstants.UTC_TIME_FORMAT,
                                               time );

         setLastNotificationTime(spid, region, newTime, type);

       }
       catch(MessageException mex){

          Debug.error("Could not set the last notification time: "+
                      mex);

          throw mex;

       }

   }

   /**
   * Sets the last notification time, taking a numeric region parameter.
   */
   public void setLastNotificationTime(String customerSPID,
                                       int region,
                                       java.util.Date newTime)
                                       throws DatabaseException{

      setLastNotificationTime(customerSPID,
                              Integer.toString(region),
                              newTime);

   }

   /**
   * Sets the last notification time for the given SPID and region.
   *
   * @param customerSPID the customer ID
   * @param region the String version of the numeric region code.
   * @param newTime the new last notification time for the given SPID and
   *                region.
   */
   public void setLastNotificationTime(String customerSPID,
                                       String region,
                                       java.util.Date newTime)
                                       throws DatabaseException{

      setLastNotificationTime(customerSPID,
                              region,
                              newTime,
                              DEFAULT_TYPE);

   }


   /**
   * Sets the last notification time for the given SPID and region.
   *
   * @param customerSPID the customer ID
   * @param region the String version of the numeric region code.
   * @param newTime the new last notification time for the given SPID and
   *                region.
   */
   private void setLastNotificationTime(String customerSPID,
                                        String region,
                                        java.util.Date newTime,
                                        int type)
                                        throws DatabaseException{


      if( Debug.isLevelEnabled(Debug.DB_STATUS) ){

         Debug.log(Debug.DB_STATUS,
                   "Setting last " +
                   ((type == NETWORK_TYPE) ? "network " : "")+
                   "notification time ["+
                   newTime+
                   "] for SPID ["+
                   customerSPID+
                   "] and region ["+region+"]");

      }

      Connection connection = DBInterface.acquireConnection();
      PreparedStatement statement = null;
      try{

         boolean autoCommit = connection.getAutoCommit();

         try{

            // we want to execute these two statements within a transaction
            connection.setAutoCommit(false);

            // delete the old value
            statement = connection.prepareStatement(delete);

            statement.setString(1, customerSPID);
            statement.setString(2, region);
            statement.setInt(3, type);

            statement.execute();
            statement.close();

            // insert the new value
            statement = connection.prepareStatement(insert);

            statement.setString(1, customerSPID);
            statement.setString(2, region);
            Timestamp sqlTime = new Timestamp( newTime.getTime() );
            statement.setTimestamp(3, sqlTime);
            statement.setInt(4, type);

            statement.execute();
            

            connection.commit();

         }
         finally{

            try{
               connection.setAutoCommit(autoCommit);
            }
            catch(Exception ex){

               Debug.error("Could not restore auto commit value: "+
                           ex);

            }

         }

      }
      catch(SQLException sqlex){

         throw new DatabaseException(sqlex);

      }
      finally{
    	
    	try {
  			statement.close();
  		} catch (SQLException e) {
  			Debug.log(Debug.ALL_ERRORS, "Error: error occured in closing the prepared statement" +
  					", " + e.getMessage());
  		}
        DBInterface.releaseConnection(connection);

      }

   }
   
   /**
    * This retrieves the last service providers notification time.
    *
    * @param spid the SPID for the customer whose last notification time
    *             should be returned.
    * @param region the region that should be queried for that customer.
    * @return Date returns Lastnotification time  in Date format.
    * @throws DatabaseException is the query fails for some reason.
    */
   
   public java.util.Date getLastServiceProvNotificationTime(String spid, int region) throws DatabaseException
   {
	   return getLastNotificationTime( spid, region, SERVICE_PROV_TYPE );
	   
   }
   
   /**
    * Sets the last ServiceProviders time.
    */
    public void setLastServiceProvNotificationTime(String spid,
                                               int region,
                                               java.util.Date newTime)
                                               throws DatabaseException{

       setLastNotificationTime( spid,
                                Integer.toString(region),
                                newTime,
                                SERVICE_PROV_TYPE );

    }



   /**
   * This retrieves the last network notification time.
   *
   * @param spid the SPID for the customer whose last notification time
   *             should be returned.
   * @param region the region that should be queried for that customer.
   * @throws DatabaseException is the query fails for some reason.
   */
   public java.util.Date getLastNetworkNotificationTime(String spid,
                                                        int region)
                                                        throws DatabaseException{

      return getLastNotificationTime( spid, region, NETWORK_TYPE );

   }

   /**
   * Sets the last network notification time.
   */
   public void setLastNetworkNotificationTime(String spid,
                                              int region,
                                              java.util.Date newTime)
                                              throws DatabaseException{

      setLastNotificationTime( spid,
                               Integer.toString(region),
                               newTime,
                               NETWORK_TYPE );

   }
  
		   

   /**
   * Used for testing. This queries the last notification time
   * based on a SPID and region.
   */
   public static void main(String[] args){

      Debug.enableAll();

      try{

         DBInterface.initialize(args[0], args[1], args[2]);

         LastNotificationTime test =
            new LastNotificationTime(
               NPACComServer.DEFAULT_LAST_NOTIFICATION_TIME_TABLE);

         java.util.Date date =
                     test.getLastNotificationTime(args[3],
                                                  Integer.parseInt(args[4]),
                                                  DEFAULT_TYPE);

         System.out.println("Date: "+date);


      }
      catch(Exception ex){

         ex.printStackTrace();

      }

   }

}
