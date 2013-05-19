/**
 * This takes the NBRPOOL_BLOCK data for a specific service provider
 * in a specific region from lines of an NBRPOOL_BLOCK BDD file and then uses
 * this data to update the SOA_NBRPOOL_BLOCK table in the database.
 *
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants
 * @see com.nightfire.spi.neustar_soa.adapter.NPACConstants
 * @see com.nightfire.spi.neustar_soa.file.NPACTokenConsumer
 */

/**
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			04/12/2004			Created
	2			Ashok			05/06/2004			LASTREQUESTTYPE,
													LASTREQUESTDATE,
													LASTRESPONSE,
													LASTRESPONSEDATE
													assigned to NULL
	3			Ashok			06/14/2004			Modified because
													Database changed and
													included report generation
													code
	4			Ashok			09/13/2004			RegionId column added in
													SOA_NBRPOOL_BLOCK
    5			D.Subbarao		02/15/2006			Implemented abstract methods
    												with empty body.
	6           D.Subbarao		02/16/2006			Added setUpdateNbrPoolBlk()
													and eleminated methods with 
													empty body.																							
    
	7			Manoj Kumar		05/25/2006			NPBType	and Alternative SPID column 											
													added in SOA_NBRPOOL_BLOCK
 */

package com.nightfire.spi.neustar_soa.file;

import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class NBRPoolBlockTokenConsumer extends BDDTokenConsumer {

	/**
	* The SQL insert statement used to insert new NBRPOOL_BLOCK data.
	*/
	
	public static final String INSERT =
		"insert into "
			+ SOAConstants.NBRPOOL_BLOCK_TABLE
			+ " values ( ?, ?, ?, ?, ?, ?, ?, ?, ?,"
			+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
			+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

	
	
	/**
	* The SQL update statement used to update existing
	* rows of NPANXX_X table.
	*/
	public static final String UPDATE =
			"update "
			+ SOAConstants.NPANXXX_TABLE
			+ " set "
			+ SOAConstants.ACTIVEFLAG_COL
			+ " ="
			+ SOAConstants.NPANXXX_ACTIVEFLAG
			+ " where "
			+ SOAConstants.SPID_COL
			+ " = ? and "
			+ SOAConstants.NPA_COL
			+ " = ? and "
			+ SOAConstants.NXX_COL
			+ " = ? and "
			+ SOAConstants.DASHX_COL
			+ " = ? ";
	     
	/**
	* This identifies the region that for the data is being imported.
	* This will be set into the NBRPOOL_BLOCK table.
	* This is an int value from 0 to 7.
	*/
	private String region;

	/**
	* This is the customer ID to which the imported records belong. Only
   * users for this customer will be allowed to view these copies of the
   * number pool block records.
	*/
	private String customerID;

	/**
	* Constructor.
	*
	* @param region String the index of the region for the NBRPOOL_BLOCK
	*                      data being processed.
	* @throws NumberFormatException if the given region value is not numeric
	*                               or if it is not in the valid range of
	*                               regions from 0 to 7.
	*/
	public NBRPoolBlockTokenConsumer(String region, String customerID)
		throws NumberFormatException {

		int regionValue = Integer.parseInt(region);

		if (regionValue < 0 || regionValue >= NPACConstants.REGION_COUNT) {

			throw new NumberFormatException(
				"[" + region + "] is not a valid region.");

		}

      this.region = region;
      this.customerID = customerID;

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
	  * Returns the SQL statement used to insert a full row of data
	  * into the SOA_NBRPOOL_BLOCK table.
	  *
	  * @return String the SQL insert statement.
	  */
	protected String getInsertSQL() {

		return INSERT;

	}

	/**
	* Returns the SQL statement used to update a row of data
	* in the NPANXX_X table.
	*
	* @return String the SQL update statement.
	*/
	protected String getUpdateSQL() {

	    
		return UPDATE;

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
	*                             to happen.
	*/
	protected void setInsertParams(
		PreparedStatement insertStatement,
		String[] params)
		throws SQLException, FrameworkException {

		int column = 1;

		try {
		  
			// SPID
			insertStatement.setString(column, params[3]);

			// NPA
			insertStatement.setString(++column, params[1].substring(0, 3));

			// NXX
			insertStatement.setString(++column, params[1].substring(3, 6));

			// DASHX
			insertStatement.setString(++column, params[1].substring(6));

			// LRN value
			insertStatement.setString(++column, params[2]);

			// CLASSDPC value
			insertStatement.setString(++column, params[5]);

			// CLASSSSN value
			insertStatement.setString(++column, params[6]);

			// CNAMDPC value
			insertStatement.setString(++column, params[11]);

			// CNAMSSN value
			insertStatement.setString(++column, params[12]);

			// ISVMDPC value
			insertStatement.setString(++column, params[9]);

			// ISVMSSN value
			insertStatement.setString(++column, params[10]);

			// LIDBDPC value
			insertStatement.setString(++column, params[7]);

			// LIDBSSN value
			insertStatement.setString(++column, params[8]);

			// WSMSCDPC value
			insertStatement.setString(++column, params[13]);

			// WSMSCSSN value
			insertStatement.setString(++column, params[14]);

			// NBR_POOL_BLOCK_ID value
			insertStatement.setInt(++column, Integer.parseInt(params[0]));

			// SOA_ORIGINATION value
			insertStatement.setInt(++column, SOAConstants.SOA_ORIGINATION);

			// Status
			insertStatement.setString(++column, SOAConstants.ACTIVE_STATUS);

			// Last Request
			insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			// Last Request Date
			insertStatement.setNull(++column, java.sql.Types.DATE);

			// Last Response
			insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			// Last Response Date
			insertStatement.setNull(++column, java.sql.Types.DATE);

			//Activation Date
			Date activationDate = parseDate(params[4]);
			if (activationDate != null) {

				insertStatement.setTimestamp(
					++column,
					new Timestamp(activationDate.getTime()));

			} else {

				insertStatement.setNull(++column, java.sql.Types.DATE);

			}

			// FAILEDSPFLAG
			insertStatement.setNull(++column, java.sql.Types.INTEGER);

			// Created By
			insertStatement.setString(++column, SOAConstants.SYSTEM_USER);

			// Created Date
			// used current time
			insertStatement.setTimestamp(
				++column,
				new Timestamp(System.currentTimeMillis()));

			// OBJECTCREATIONDATE
			insertStatement.setNull(++column, java.sql.Types.DATE);

			//	REGIONID
			insertStatement.setString(++column, region );

			//	CUSTOMERID
			insertStatement.setString(++column, customerID );

			
			// NPBType 
			if (params.length>15)
			{
				if (params[16] == null || params[16].equals(""))
				{
					insertStatement.setNull(++column, java.sql.Types.VARCHAR);
				}
				else{
					insertStatement.setString(++column,SOAUtility.getSVType(params[16]));
				}
			}
			else{
					insertStatement.setNull(++column, java.sql.Types.VARCHAR);
			}
			
			// Alternative SPID 
			if (params.length>16)
			{
				if (params[17] == null || params[17].equals(""))
				{
					insertStatement.setNull(++column, java.sql.Types.VARCHAR);
				}
				else{
					insertStatement.setString(++column,params[17]);
				}
			}
			else{
					insertStatement.setNull(++column, java.sql.Types.VARCHAR);
			}
			
			
			
			
		} catch (SQLException sqle) {

			throw new FrameworkException(
				"Unable to set values in insertStatement : " + sqle);

		}

	}
	/**
	* This method will update the row in NPANXX_X table
	*
	* @param updateStatement PreparedStatement the precompiled update statement.
	* @param params String[] The tokens from a line of input. These
	*                        values are used to populate parameters of the
	*                        updateStatement.
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

		int column = 1;

		try{
		
		if(tokenType == null){
			
			// SPID
			updateStatement.setString(column, params[3]);
	
			// NPA
			updateStatement.setString(++column, params[1].substring(0, 3));
	
			// NXX
			updateStatement.setString(++column, params[1].substring(3, 6));
	
			//DASHX
			updateStatement.setString(++column, params[1].substring(6));
		}
		else {
		    
		    if(tokenType.equals(SOAConstants.NBRBLKTOKEN)){
		        
		     //This will be used to update the BDD data into SOA_NBRPOOL_BLOCK table. 
		       setUpdateNbrPoolBlk(updateStatement, params);
		        
		    }
		}
		} catch (SQLException sqle) {

			throw new FrameworkException(
				"Unable to set values in updateStatement : " + sqle);

		}

	}
	/**
	* This will be used to update the BDD data into SOA_NBRPOOL_BLOCK table.
	* @param params contains the data which is extracted from BDD file.
	* @throws SQLException if an error occurs while setting the parameter
	*                      values on the update statement.
	* @throws FrameworkException if one of the params is in an invalid format.
	*                            For example, a bad date. This is not expected
	*                            to happen.
	*/
	private void setUpdateNbrPoolBlk(PreparedStatement updateStatement,
	        String[] params)throws SQLException, FrameworkException {
	    
	    try{
	     
	        int column = 1;
		   
			// LRN
			updateStatement.setString(column, params[2]);
				
			// CLASSDPC
			updateStatement.setString(++column, params[5]);
				
			// CLASSSSN
			updateStatement.setString(++column, params[6]);
				
			// CNAMDPC
			updateStatement.setString(++column, params[11]);
				
			// CNAMSSN
			updateStatement.setString(++column, params[12]);
			
			// ISVMDPC
			updateStatement.setString(++column, params[9]);
	
			// ISVMSSN
			updateStatement.setString(++column, params[10]);
	
			// LIDBDPC
			updateStatement.setString(++column, params[7]);
	
			// LIDBSSN
			updateStatement.setString(++column, params[8]);
				
			// WSMSCDPC
			updateStatement.setString(++column, params[13]);
			
			// WSMSCSSN
			updateStatement.setString(++column, params[14]);
				
			// NBR_POOL_BLOCK_ID value
			updateStatement.setInt(++column, Integer.parseInt(params[0]));
	
			// Activation Date
			Date activationDate = parseDate(params[4]);
			if (activationDate != null) {
	
				    updateStatement.setTimestamp(
						++column,
						new Timestamp(activationDate.getTime()));
	
				} else {
	
				    updateStatement.setNull(++column, java.sql.Types.DATE);
	
				}
			//	Status
			updateStatement.setString(++column, SOAConstants.ACTIVE_STATUS);

			//	CUSTOMERID
			updateStatement.setString(++column, customerID );
			
			
			//NPBType
			if (params.length > 15) {
				if (params[16] == null || params[16].equals(""))
				{
					updateStatement.setNull(++column, java.sql.Types.VARCHAR);
				}
				else {
					updateStatement.setString(++column, SOAUtility.getSVType(params[16]));
				}
			}
			else {
				updateStatement.setNull(++column, java.sql.Types.VARCHAR);
			}

			//AlternativeSPID
			if (params.length > 16) {
			
				if (params[17] == null || params[17].equals(""))
				{
					updateStatement.setNull(++column, java.sql.Types.VARCHAR);
				}
				else {
					updateStatement.setString(++column, params[17]);
				}		
			}
			else {
				updateStatement.setNull(++column, java.sql.Types.VARCHAR);
			}			
			
			// NPA
			updateStatement.setString(++column, params[1].substring(0, 3));
		
		
			// NXX
			updateStatement.setString(++column, params[1].substring(3, 6));
		
			//DASHX
			updateStatement.setString(++column, params[1].substring(6));
				
			// SPID
			updateStatement.setString(++column, params[3]);
			
			
			
			} catch (SQLException sqle) {
		        
				throw new FrameworkException(
					"Unable to set values in updateStatement : " + sqle);
			}
			    
	}

}
