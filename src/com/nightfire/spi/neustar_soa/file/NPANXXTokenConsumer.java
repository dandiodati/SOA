/**
 * This takes the NPANXX data in a specific
 * region from lines of an NPANXX BDD file and then uses this data to update the
 * SOA_NPANXX table in the database.
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
	3			Ashok			06/16/2004			Database design changed
	4. 			D.Subbarao		02/15/2006			Added some constants and 
													functions to update the NPANXX
													data into the database.																	
	5.			D.Subbarao		02/16/2006			Modified.
  
 */

package com.nightfire.spi.neustar_soa.file;

import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.StringTokenizer;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class NPANXXTokenConsumer extends NPACTokenConsumer{

   	/**
     * The SQL insert statement used to insert new NPANXX data.
     */
   	public static final String INSERT = "insert into "+
                                       SOAConstants.NPANXX_TABLE+
                                       " values ( ?, ?, ?, ?, ?, ?, ?, ?, ?,"+
                                       " ?, ?, ?, ?, ?, ?, ?)";
	/**
     * The SQL update statement used to update the NPANXX data.
     */
   	public static final String UPDATE = "update " + SOAConstants.NPANXX_TABLE +
   				" set SPID=?,NPANXXID=?,EFFECTIVEDATE=?,OBJECTCREATIONDATE=? " +
   				" ,STATUS=? where NPA=? and NXX=? and REGIONID=? ";

   	/**
     * This identifies the region that for the data is being imported.
     * This will be set into the NPANXX table.
     * This is an int value from 0 to 7.
     */
   	private String region = null;

   	/**
     * Constructor.
     *
     * @param region String the index of the region for the NPANXX
     *                      data being processed.
     * @throws NumberFormatException if the given region value is not numeric
     *                               or if it is not in the valid range of
     *                               regions from 0 to 7.
     */
   	public NPANXXTokenConsumer(String region)
                           throws NumberFormatException{

      	int regionValue = Integer.parseInt(region);

      	if( regionValue < 0 ||
          	regionValue >= NPACConstants.REGION_COUNT ){

         	throw new NumberFormatException("["+region+
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
     * Returns the SQL statement used to insert a full row of data into
     * the SOA_NPANXX table.
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
        
        try{
        

		// SPID
     	insertStatement.setString(column, params[0]);

     	// Region ID
     	insertStatement.setInt(++column, Integer.parseInt(region));
     	
		StringTokenizer npaNxxTokens = new StringTokenizer(params[2],"-");

     	// NPA Value
     	insertStatement.setString(++column,npaNxxTokens.nextToken());

     	// NXX Value
     	insertStatement.setString(++column,npaNxxTokens.nextToken());

     	// EffectiveDate value
     	Date effectiveDate = parseDate(params[4]);
     
	 	if(effectiveDate != null){
		
			insertStatement.setTimestamp(++column,
						new Timestamp(effectiveDate.getTime()));
						
	 	}else {
		
			insertStatement.setNull(++column, java.sql.Types.DATE);

	 	}  
	 
	 	// FIRSTUSEDDATE
	 	insertStatement.setNull(++column, java.sql.Types.DATE);       

        // FIRSTUSEDSPID
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

        // Status
        insertStatement.setString(++column, SOAConstants.OK_STATUS);

        // NPANXXID value
        if(params[1] != "")
        {
         
        	insertStatement.setInt(++column, Integer.parseInt(params[1]));
         
        }else {
         	
			insertStatement.setNull(++column, java.sql.Types.INTEGER);
         	
        }
         
		// Last Request
        insertStatement.setNull(++column, java.sql.Types.VARCHAR);
		
        // Last Request Date
		insertStatement.setNull(++column, java.sql.Types.DATE);

        // Last Response
		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

        // Last Response Date
		insertStatement.setNull(++column, java.sql.Types.DATE);

        // Created By
        insertStatement.setString(++column, SOAConstants.SYSTEM_USER);

        // Created Date
        // use current time
        insertStatement.setTimestamp(++column,
                                      new Timestamp(System.currentTimeMillis())
                                      );

        // Object Creation Date
        Date objectCreationDate = parseDate(params[3]);
		if(objectCreationDate != null){
			
			insertStatement.setTimestamp(++column,
							new Timestamp(objectCreationDate.getTime()));
							
		}else {
			
			insertStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		} catch (SQLException sqle) {

			throw new FrameworkException(
				"Unable to set values in insertStatement : " + sqle);

		}

   	}
   	
   	/**
     * Returns the SQL statement used to update the data into the SOA_NPANXX
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
    *                      values on the insert statement.
    * @throws FrameworkException if one of the params is in an invalid format.
    *                            For example, a bad date. This is not expected
    *                            to happen.
    */
  	protected void setUpdateParams(PreparedStatement updateStatement,
                                 String[] params)
                                 throws SQLException,
                                        FrameworkException{

        int column = 1;

        // SPID
        updateStatement.setString(column,params[0]);

        // NPANXX ID
        updateStatement.setInt(++column, Integer.parseInt(params[1]));
        
        // EffectiveDate value
     	Date effectiveDate = parseDate(params[4]);
     
	 	if(effectiveDate != null)
	 	    
			updateStatement.setTimestamp(++column,
						new Timestamp(effectiveDate.getTime()));
	 	
		else 
		
		    updateStatement.setNull(++column, java.sql.Types.DATE);
	 	   
	 	//  Object Creation Date
        Date objectCreationDate = parseDate(params[3]);
        
		if(objectCreationDate != null){
			
		    updateStatement.setTimestamp(++column,
							new Timestamp(objectCreationDate.getTime()));
							
		}else {
			
		    updateStatement.setNull(++column, java.sql.Types.DATE);

		}
		
		 // Status
		updateStatement.setString(++column, SOAConstants.OK_STATUS);
        
	 	
        StringTokenizer npaNxxTokens = new StringTokenizer(params[2],"-");

    	// NPA Value
        updateStatement.setString(++column,npaNxxTokens.nextToken());

    	// NXX Value
        updateStatement.setString(++column,npaNxxTokens.nextToken());
    	
        // Region ID
        updateStatement.setString(++column, region);
 	       
  	}     	
   	   		
}
