/**
 * This takes the SPID data in a specific region
 * from lines of an SPID BDD file and then uses this data to update the
 * SOA_SPID table in the database.
 * 
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants
 * @see com.nightfire.spi.neustar_soa.utils.NPACConstants
 * @see com.nightfire.spi.neustar_soa.utils.NPACTokenConsumer
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			04/12/2004			Created
	2			Ashok			06/23/2004			Database design changed
	3			D.Subbarao		02/15/2006			Implemented some abstract
													methods with empty.
    4			D.Subbarao		02/16/2006			Modified.
    5			D.Subbarao		02/24/2006			Implemented setUpdateParams,
    												getUpdateSQL methods and 
    												added UPDATE object.
    6			Jigar Talati	04/14/2006			Added SPTYPE mapping
 */

package com.nightfire.spi.neustar_soa.file;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class SPIDTokenConsumer extends NPACTokenConsumer {

	/**
	* The SQL insert statement used to insert new SPID data.
	*/
	public static final String INSERT =
		"insert into "
			+ SOAConstants.SPID_TABLE
			+ " values ( ?, ?, ?, ?, ?, ?, ?, ?)";

	public static final String UPDATE="update " + SOAConstants.SPID_TABLE
	  	+  " set  NAME=?,  STATUS=?, SPTYPE=? where SPID=? AND REGIONID=?";

	/**
	* This identifies the region that for the data is being imported.
	* This will be set into the SPID table.
	* This is an int value from 0 to 7.
	*/
	private String region;

	/**
	* Constructor.
	* @param region String the index of the region for the SPID
	*                      data being processed.
	* @throws NumberFormatException if the given region value is not numeric
	*                               or if it is not in the valid range of
	*                               regions from 0 to 7.
	*/
	public SPIDTokenConsumer(String region) throws NumberFormatException {

		int regionValue = Integer.parseInt(region);

		if (regionValue < 0 || regionValue >= NPACConstants.REGION_COUNT) {

			throw new NumberFormatException(
				"[" + region + "] is not a valid region.");

		}

		this.region = region;

	}

	/**
	 * Returns the SQL statement used to insert a full row of data into the SPID
	 * table.
	 *
	 * @return String the SQL insert statement.
	 */
	protected String getInsertSQL() {

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
	protected void setInsertParams(
		PreparedStatement insertStatement,
		String[] params)
		throws SQLException, FrameworkException {

		int column = 1;

		try {

			// Region ID
			insertStatement.setString(column, region);

			// SPID
			insertStatement.setString(++column, params[0]);

			// NAME Value
			insertStatement.setString(++column, params[1]);

			// STATUS Value
			insertStatement.setString(++column, SOAConstants.OK_STATUS);

			// LASTRESPONSE value
			insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			// LASTRESPONSEDATE									  
			insertStatement.setNull(++column, java.sql.Types.DATE);

			// Created Date
			// use current time
			insertStatement.setTimestamp(
				++column,
				new Timestamp(System.currentTimeMillis()));
			
			// SPTYPE Value
			insertStatement.setString(++column, SOAUtility.getSPType(params[2]));
			
		} catch (SQLException sqle) {

			throw new FrameworkException(
				"Unable to set values for " + "insertStatement : " + sqle);
		}
	}

	/**
     * This method must be implemented by child classed to provide the
     * implementation-specific SQL used to update the date into the database.
     *
     * @return String an SQL update statement.
     */
   	protected String getUpdateSQL(){     	    
		return UPDATE;
   	}

   	/**
     * This must be implemented by child classes to populate the
     * given statement with the given data.
     *
     * @param updateStatement PreparedStatement the statement whose parameters
     *                                           need to be populated.
     * @param params String[] the line of tokens whose values will be used
     *                         to populate the statement.
     * @throws SQLException thrown if the statement cannot be populated.
     * @throws FrameworkException thrown if some other error occurs.
     *
     */
   	protected void setUpdateParams(PreparedStatement updateStatement,
                                           String[] params)
                                           throws SQLException,
												FrameworkException{
   	    int column = 1;
		// NAME Value
   		updateStatement.setString(column, params[1]);

		// STATUS Value
   		updateStatement.setString(++column, SOAConstants.OK_STATUS);

		//  SPTYPE
   	    updateStatement.setString(++column, SOAUtility.getSPType(params[2]));
		
		// SPID
   	 	updateStatement.setString(++column, params[0]);
   	 	
   	    //  Region ID
   	    updateStatement.setString(++column, region);
   	    
   	    }
}
