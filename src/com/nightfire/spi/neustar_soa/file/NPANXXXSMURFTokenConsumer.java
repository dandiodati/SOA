/**
 * This takes the NPANXXX data from lines of an NPANXXX SMURF file and then uses 
 * this data to update the SOA_NPANXX_X table in the database .
 * 
 * @author 		Ashok Kumar
 * @version		1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.spi.neustar_soa.file.SMURFTokenConsumer
 * @see			com.nightfire.spi.neustar_soa.utils.SOAConstants
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			05/04/2004			Created
	2			Ashok			05/11/2004			Modified because Base class
													(SMURFTokenConsumer) has
													been modified
  
 */

package com.nightfire.spi.neustar_soa.file;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NPANXXXSMURFTokenConsumer extends SMURFTokenConsumer {

	/**
	* The SQL update statement used to update existing rows of NPA_NXX_X data.
	*/
	public static final String UPDATE =
		"update "
			+ SOAConstants.NPANXXX_TABLE
			+ " set "
			+ SOAConstants.SPID_COL
			+ " = ? "
			+ " where "
			+ SOAConstants.SPID_COL
			+ " = ? and "
			+ SOAConstants.NPA_COL
			+ " = ? and "
			+ SOAConstants.NXX_COL
			+ " = ? and "
			+ SOAConstants.DASHX_COL
			+ " = ? ";

	/**This method will initialize the DB connection 
	 * by calling base class method
	 *
	 * @param filename String the filename is given because it may contain
	 *                        important date information.
	 * 
	 * @return void
	 * @throws FrameworkException
	 */
	public void init(String filename, String fileType)
		throws FrameworkException {

		// initialize the DB connection
		super.init(filename, fileType);

	}

	/**
	* Returns the SQL statement used to update a row of data in the NPANXX_X
	* table.
	*
	* @return String the SQL update statement.
	*/
	protected String getUpdateSQL() {

		return UPDATE;

	}

	/**
	*This method is used to populate Prepared Statement to update NPANXX_X table
	*
	* @param updateStatement PreparedStatement the precompiled update statement.
	* @param params String[] The tokens from a line of input. These
	*                        values are used to populate parameters of the
	*                        updateStatement.
	* 
	* @return void
	* @throws SQLException if an error occurs while setting the parameter
	*                      values on the update statement.
	* @throws FrameworkException if one of the params is in an invalid format.
	*                            For example, a bad date. This is not expected
	*                            to happen.
	*/
	protected void setUpdateParams(
		PreparedStatement updateStatement,
		String[] params)
		throws SQLException, FrameworkException {
		int i = 0;

		try {

			updateStatement.setString(++i, params[1]);

			updateStatement.setString(++i, params[0]);

			updateStatement.setString(++i, params[2].substring(0, 3));

			updateStatement.setString(++i, params[2].substring(3, 6));

			updateStatement.setString(++i, params[2].substring(6));
		} catch (SQLException sqle) {

			throw new FrameworkException(
				"Unable to set values for updateStatement : " + sqle);
		}

	}

	/**
	 * This method will return array of records which contains no of
	 * updated record in base table , Sv table and NPB table
	 * 
	 * @return int[]
	 */
	public int[] getRecords() {
		int[] recordsArr = new int[3];

		recordsArr[0] = super.getRecordsSkipped();

		recordsArr[1] = 0;

		recordsArr[2] = 0;

		return recordsArr;

	}

} //end of class NPANXXXSMURFTokenConsumer
