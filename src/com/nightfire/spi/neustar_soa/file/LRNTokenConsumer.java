///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.file;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;

import java.util.Date;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * This takes the LRN data for a specific service provider in a specific region
 * from lines of an LRN BDD file and then uses this data to update the
 * SOA_LRN table in the database.
 */
public class LRNTokenConsumer extends NPACTokenConsumer{

   	/**
     * The SQL insert statement used to insert new LRN data.
     */
   	public static final String INSERT = "insert into "+
                                       SOAConstants.LRN_TABLE+
                                       " values ( ?, ?, ?, ?, ?, ?, ?," +
                                       " ?, ?, ?, ?, ? , ?)";

	/**
     * The SQL update statement used to update the LRN data.
     */
   	public static final String UPDATE = "update " + SOAConstants.LRN_TABLE +
   						" set LRNID=?,OBJECTCREATIONDATE=?,STATUS=? " +
   						" where SPID=? and LRN=? and REGIONID=?";
   	/**
     * This identifies the region that for the data is being imported.
     * This will be set into the LRN table.
     * This is an int value from 0 to 7.
     */
   	private String region;

   	/**
     * Constructor.
     *
     * @param region String the index of the region for the LRN
     *                      data being processed.
     * @throws NumberFormatException if the given region value is not numeric
     *                               or if it is not in the valid range of
     *                               regions from 0 to 7.
     */
   	public LRNTokenConsumer(String region )
                           throws NumberFormatException{

      	int regionValue = Integer.parseInt(region);

     	if( regionValue < 0 ||
          	regionValue >= NPACConstants.REGION_COUNT ){

         	throw new NumberFormatException("["+region + 
											"] is not a valid region.");

      	}

      	this.region = region;

   	}
   
   	/**
	 *
	 * @param filename String the filename is given because it may contain
	 *                        important date information.
	 * @throws FrameworkException
	 */
   	public void init(String filename) throws FrameworkException {

	   // initialize the DB connection
	   super.init(filename);

	}

	/**
     * Returns the SQL statement used to insert a full row of data into the LRN
     * table.
     *
     * @return String the SQL insert statement.
     */
   	protected String getInsertSQL(){

      return INSERT;

   }
   	/**
     *
     *
     * @param insertStatement PreparedStatement the precompiled insert statement.
     * @param params String[] The tokens from a line of input. These
     *                        values are used as the parameters of the
     *                        insertStatement.
     * @throws SQLException if an error occurs while setting the parameter
     *                      values on the insert statement.
     * @throws FrameworkException if one of the params is in an invalid format.
     *                            For example, a bad date. This is not expected
     *                            to happen.
     */
   	protected void setInsertParams(PreparedStatement insertStatement,
                                  String[] params)
                                  throws SQLException,
                                         FrameworkException{

        int column = 1;

        // SPID
        insertStatement.setString(column, params[0]);

        // Region ID
        insertStatement.setString(++column, region);
         
		// LRN Value
        insertStatement.setString(++column, params[2]);
       
        // LRN ID
        insertStatement.setInt(++column, Integer.parseInt(params[1]));

        // Status
        insertStatement.setString(++column, SOAConstants.OK_STATUS);
         
		// GTTID
		insertStatement.setNull(++column, java.sql.Types.INTEGER);

        // Last Request
        insertStatement.setNull(++column, java.sql.Types.VARCHAR );

        // Last Request Date 
        insertStatement.setNull(++column,java.sql.Types.DATE );

        // Last Response
        insertStatement.setNull(++column, java.sql.Types.VARCHAR );

        // Last Response Date
        insertStatement.setNull(++column,java.sql.Types.DATE );

        // Created By
        insertStatement.setString(++column, SOAConstants.SYSTEM_USER);

        // Created Date
        // use current time
        insertStatement.setTimestamp(++column,
                                     new Timestamp(System.currentTimeMillis())
                                      );

        // Object Creation Date
        Date objectCreationDate = parseDate(params[3]);
        insertStatement.setTimestamp(++column,
                                      new Timestamp(objectCreationDate.getTime()));


   	}
	/**
     * Returns the SQL statement used to update the data into the LRN
     * table based upon a criteria.
     *
     * @return String the SQL update statement.
     */
   	protected String getUpdateSQL(){

      return UPDATE;

   }
   	/**
    *
    *
    * @param updateStatement PreparedStatement the precompiled update statement.
    * @param params String[] The tokens from a line of input. These
    *                        values are used as the parameters of the
    *                        updateStatement.
    * @throws SQLException if an error occurs while setting the parameter
    *                      values on the update statement.
    * @throws FrameworkException if one of the params is in an invalid format.
    *                            For example, a bad date. This is not expected
    *                            to happen.
    */
  	protected void setUpdateParams(PreparedStatement updateStatement,
                                 String[] params)
                                 throws SQLException,
                                        FrameworkException{

       int column = 1;

    	//LRN ID
       updateStatement.setInt(column, Integer.parseInt(params[1]));
       
       //Object Creation Date
       Date objectCreationDate = parseDate(params[3]);
       if(objectCreationDate!=null)       
           updateStatement.setTimestamp(++column,
                        new Timestamp(objectCreationDate.getTime()));
       else
           updateStatement.setNull(++column, java.sql.Types.DATE);
       
       //Status
       updateStatement.setString(++column, SOAConstants.OK_STATUS);

       //SPID Value
       updateStatement.setString(++column, params[0]);

       //LRN value
       updateStatement.setString(++column, params[2]);

       //Region ID
       updateStatement.setString(++column, region);       
  	}

}
