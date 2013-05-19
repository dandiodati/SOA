/**
 * This is the base class used for the database connection ,prepared statement.
 * 
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.framework.db.DBInterface
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.framework.util.FrameworkException
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			06/24/2004			Created
	2			Ashok			07/14/2004			Review comments incorporated
  
 */
package com.nightfire.spi.neustar_soa.file;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;


public class DBBase {

   	/**
     * A DB connection .
     */
   	private Connection connection;

   	/**
     * This is just used to hold the DB Connection's original autocommit
     * value.
     */
   	private boolean connectionAutoCommitTemp;

   	/**
     * This gets a database connection .
     *
     * @throws FrameworkException if a database connection could not
     *                            be acquired.
     */
   	public void init() throws FrameworkException{
		   
      	try{
			
			//	get a DB Connection from a pool of connections
			 connection = DBInterface.acquireConnection();
				 
         	// save the original auto-commit value
         	connectionAutoCommitTemp = connection.getAutoCommit();

         	// Set to not autocommit. We will either commit everything 
         	// at the end or rollback.
           	connection.setAutoCommit(false);

      	}catch(DatabaseException de){
      		
			Debug.error("Could not acquire database connection: "+de);

      	}
      	catch(SQLException sqlex){

         throw new FrameworkException("Could not set autocommit on the " +
         												"connection: "+sqlex);

      	}

   	}

   	/**
     *  This either commits or rollsback the DB changes as neccessary 
     * and releases the DBConnection.
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
               		Debug.error("Could not reset the autocommit flag on the " +
               											"connection: "+sqlex);
               											
            	}
			
            	// return connection to the pool
            	DBInterface.releaseConnection(connection);
				

         	}

      	}
      	catch(DatabaseException de){

         	Debug.error("Could not release database connection: "+de);

      	}

   	}

   	/**
     * This calls on the DB connection to prepare an SQL statement.
     * init() must be called before calling this method, otherwise,
     * the connection will be null and throw a NullPointerException.
     *
     * @param sql String the SQL statement to prepare.
     * @throws SQLException 
     * @return PreparedStatement
     */
   	protected PreparedStatement prepareStatement(String sql)
                               throws SQLException{

      	return connection.prepareStatement(sql);

   	}

}
