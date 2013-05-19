///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.file;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.utils.*;

import java.util.Date;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;

/**
 * This is the base class used for processing lines of delimited input
 * that are used to update the database.
 */
public abstract class DBTokenConsumerBase implements TokenConsumer {

   /**
    * This is the date format used by date values found in the NPAC files.
    * This is used to parse the value of these date strings.
    */
   public static String FILE_DATE_FORMAT = "yyyyMMddHHmmss";  

   /**
    * A DB connection to use while processing tokens.
    */
   private Connection connection;

   /**
    * This is just used to hold the DB Connection's original autocommit
    * value.
    */
   private boolean connectionAutoCommitTemp;

   /**
    * This gets a database connection that will be used in processing.
    * This is called by the DelimitedFileReader before processing an input
    * file.
    *
    * @param filename String the name of the filename being processed
    *                        is passed in because the filename may
    *                        contain information such as the date that
    *                        the file was generated.
    *
    * @throws FrameworkException if a database connection could not
    *                            be acquired.
    */
   public void init(String filename) throws FrameworkException{

      // get a DB Connection from a pool of connections
      connection = DBInterface.acquireConnection();

      try{

         // save the original auto-commit value
         connectionAutoCommitTemp = connection.getAutoCommit();

         // Set to not autocommit. We will either commit everything at the end
         // or rollback.
         // Question: should we autocommit? If we don't commit, will this hold
         //           up the SOA's access to the DB?
         connection.setAutoCommit(false);

      }
      catch(SQLException sqlex){

         throw new FrameworkException("Could not set autocommit on the connection: "+
                                      sqlex);

      }

   }

   /**
    * This is called after the file has been processed. This either commits
    * or rollsback the DB changes as neccessary and releases the DB
    * Connection.
    *
    * @param success boolean this flag indicates whether processing completed
    *                        successfully or not. This is used to determine
    *                        whether changes should be committed or whether
    *                        they should be rolled back.
    */
   public void cleanup(boolean success) {

      try{

         if( connection != null ){

            try{

               if(success){
                  // commit changes
                  connection.commit();
               }
               else{
                  // rollback changes
                  connection.rollback();
               }

            }
            catch(SQLException sqlex){

               String error = "Could not ";
               error += (success) ? "commit changes: " : "rollback changes: ";

               Debug.error(error+sqlex);

            }

            try{

               // restore the original auto-commit setting before returning
               // the connection to the pool
               connection.setAutoCommit(connectionAutoCommitTemp);

            }
            catch(SQLException sqlex){
               Debug.error("Could not reset the autocommit flag on the connection: "+
                           sqlex);
            }

            // return connection to the pool
            DBInterface.releaseConnection(connection);

         }

      }
      catch(FrameworkException fex){

         Debug.error("Could not release database connection: "+fex);

      }

   }

   /**
    * This calls on the DB connection to prepare an SQL statement.
    * init() must be called before calling this method, otherwise,
    * the connection will be null and throw a NullPointerException.
    *
    * @param sql String the SQL statement to prepare.
    * @throws SQLException if the
    * @return PreparedStatement
    */
   protected PreparedStatement prepareStatement(String sql)
                               throws SQLException{

      return connection.prepareStatement(sql);

   }

  /**
    * This utility method returns a Date object for the give String.
    * This assumes that the given date string is in the usual BDD date
    * format (see FILE_DATE_FORMAT), and that the date's time zone is GMT
    * (universal time).
    *
    * @param String date a String date as read from a file.
    * @return Date a java date representing the given date string in local time.
    */
   public static Date parseDate(String date) throws MessageException{

     if(!date.equals("")) {

       return TimeZoneUtil.parse(NPACConstants.UTC, FILE_DATE_FORMAT, date);

     }else{

       return null;

     }

   }
   
   /**
	* This method will return array of records which contains no of
	* updated record in base table , Sv table and NPB table but here
	* it will return empty array
	* 
	* @return int[]
	*/   
   public int[] getRecords()
   {
   		
   		int [] records = new int[3] ;
   	
   		return records ;
   	
   }

}
