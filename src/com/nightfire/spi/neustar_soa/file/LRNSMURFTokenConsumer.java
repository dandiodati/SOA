/**
 * This takes the LRN data from lines of an LRN SMURF file and then uses 
 * this data to update the SOA_LRN,SOA_SUBSCRIPTION_VERSION,
 * SOA_NBRPOOL_BLOCK tables in the database .
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
	3			Ashok 			06/16/2004			Modified to generate report
  
 */

package com.nightfire.spi.neustar_soa.file;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class LRNSMURFTokenConsumer extends SMURFTokenConsumer{

   	/**
     * The SQL update statement used to update existing rows of LRN table.
     */
   	public static final String UPDATE = "update "+
                                       SOAConstants.LRN_TABLE+
                                       " set "+
                                       SOAConstants.SPID_COL+
                                       " = ? "+                                       
                                       " where "+
									   SOAConstants.SPID_COL+
									   " = ? and "+
                                       SOAConstants.LRN_COL+
                                       " = ? ";
                                       
	/**
	 * The SQL update statement used to update existing rows of SV table.
	 */
    public static final String SV_UPDATE = "update /*+ index( "+
    								   SOAConstants.SV_TABLE+
    								   " SOA_SV_INDEX_4) */ "+
									   SOAConstants.SV_TABLE+
									   " set "+
									   SOAConstants.SPID_COL+
									   " = ? , "+ 
									   SOAConstants.NNSP_COL+
									   " = ? "+                                      
									   " where "+
									   SOAConstants.SPID_COL+
									   " = ? and "+
									   SOAConstants.NNSP_COL+
									   " = ? and "+
									   SOAConstants.LRN_COL+
									   " = ? and ("+
									   SOAConstants.STATUS_COL+
									   " IN('active','disconnect-pending',"+
									   "'download-failed-partial')OR ("+
									   SOAConstants.STATUS_COL+
									   " ='old' and "+
									   SOAConstants.FAILED_SP_FLAG_COL+
									   " = 1 ) )";								  
	/**
	 * The SQL update statement used to update existing rows of 
	 * NBRPOOL_BLOCK table.
	 */
    public static final String NPB_UPDATE = "update "+
										   SOAConstants.NBRPOOL_BLOCK_TABLE+
										   " set "+
										   SOAConstants.SPID_COL+
										   " = ? "+                                       
										   " where "+
										   SOAConstants.SPID_COL+
										   " = ? and "+
										   SOAConstants.LRN_COL+
										   " = ? and ("+
										   SOAConstants.STATUS_COL+
										   " IN('active','disconnect-pending',"+
										   "'download-failed-partial') OR ("+
										   SOAConstants.STATUS_COL+
										   " ='old' and "+
										   SOAConstants.FAILED_SP_FLAG_COL+
										   "  = 1))";
										   
	/**
	 * This is the precompiled SQL statement that this class will
	 * use to update data in SV table.
	 */
	private PreparedStatement svUpdateStatement = null;

	/**
	 * This is the precompiled SQL statement that this class will
	 * use to update data in Number Pool Block.
	 */
	private PreparedStatement npbUpdateStatement = null;
	
	/**
	 * this variable contains records updated in SV table
	 */
	private  int svRecordsUpdated = 0;
	
	/**
	 * this variable contains records updated in NPB table
	 */
	private  int npbRecordsUpdated = 0;
										   
	/**
     *
     * @param filename String the filename is given because it may contain
     *                        important date information.
     * 
     * @return void
     *  @param fileType String fileType is given because generate report
     * @throws FrameworkException
     */
    public void init(String filename , String fileType) 
    											throws FrameworkException
    {

		// initialize the DB connection
		super.init(filename , fileType);
		 
		try
		{
		 	
			svUpdateStatement = prepareStatement(SV_UPDATE);	
		 		 		   
		}catch(SQLException sqlex){

			Debug.error("Could not prepare update statement for SV Update:\n"+
					SV_UPDATE+
					"\n"+sqlex);
		}
	 	
		try
		{
		 	
			npbUpdateStatement = prepareStatement(NPB_UPDATE);	
 	 		   
		}catch(SQLException sqlex){

			Debug.error("Could not prepare update statement for Number " +
				"Pool block update:\n"+
				SV_UPDATE+
				"\n"+sqlex);
		}

	} 
	
   	/**
	 * Called to process a line of data from a delimited file.If record found
	 * then update that record
	 * 
	 * @return boolean
	 *
	 * @param tokens String[] the tokenized line of input.
	 * @throws FrameworkException if the attempt to insert and the attempt
	 *                            to update both fail.
	 */
	public boolean process(String[] tokens, String tokenType) throws FrameworkException
	{
		
		// this flag shows whether record updated successfully or not	 
		boolean errorFlag = super.process( tokens, null );		
   				
		try
		{
				
	   		setSVUpdateParams(svUpdateStatement, tokens);				
			
			if( svUpdateStatement.executeUpdate() > 0 )
			{
				svRecordsUpdated ++ ;
       			 
			}  
	   		
		}catch (SQLException sqlex2) {
	
			errorFlag = true;			

		}
		
		try{
								
			setNPBUpdateParams(npbUpdateStatement, tokens);
   
			if( npbUpdateStatement.executeUpdate() > 0 )
			{
       			
				npbRecordsUpdated ++ ;
       			 
			} 	       	
			   			   		
   		
		}catch (SQLException sqlex2) {
	
			errorFlag = true;					
			  
		}

		return errorFlag ;
		
   	} 
   
    /**
     * Returns the SQL statement used to update a row of data in the LRN
     * table.
     *
     * @return String the SQL update statement.
     */
    protected String getUpdateSQL(){

      return UPDATE;

    }
      

  	/**
     * This method is used to populate Prepared Statement to update LRN table
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
    protected void setUpdateParams(PreparedStatement updateStatement,
                                   String[] params)throws SQLException,
                                         				  FrameworkException{	
	  int i = 0;
	  try{
	  
      updateStatement.setString(++i,params[1]);

	  updateStatement.setString(++i,params[0]);

	  updateStatement.setString(++i,params[2]);	  
	  
	  }catch(SQLException sqle){
	  	
		throw new FrameworkException("Unable to set values for " +
												"updateStatement : "+sqle);
	  }

    }
    
	/**
     * This method is used to populate Prepared Statement to update SV table
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
   
    private void setSVUpdateParams(PreparedStatement updateStatement,
   									 String[] params)throws SQLException,
		  													FrameworkException{		  	
	  int i = 0;
	  
	  try{
	  
		  updateStatement.setString(++i,params[1]);
	
		  updateStatement.setString(++i,params[1]);
	
		  updateStatement.setString(++i,params[0]);
		  
		  updateStatement.setString(++i,params[0]);
		  
		  updateStatement.setString(++i,params[2]);   
		
	  }catch(SQLException sqle){
	  	
		throw new FrameworkException("Unable to set values for " +
												"updateStatement : "+sqle);
	  }		  	
		  	
    }
   
    /**
     * This method is used to populate Prepared Statement to update NBR table
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
    private void setNPBUpdateParams(PreparedStatement updateStatement,
	  								  String[] params)throws SQLException,
			 												 FrameworkException{			 	
		int i = 0;
		
		try{	  
		
			updateStatement.setString(++i,params[1]);
	
			updateStatement.setString(++i,params[0]);
	
			updateStatement.setString(++i,params[2]);
		
		}catch(SQLException sqle){
	  	
		  throw new FrameworkException("Unable to set values for " +
												  "updateStatement : "+sqle);
		}		  	
		  	
	}
	 
	/**
	 * Closes any prepared statements and releases the database connection.
	 * 
	 * @return void
	 *
	 * @param success boolean whether or not the processing succeeded. This
	 *                        is used to determine whether we should rollback
	 *                        or commit.
	 */ 
	public void cleanup(boolean success) {

	  	try{

		 	svUpdateStatement.close();
 
	  	}
	  	catch(SQLException sqlex){

		 	Debug.error("Could not close SV update statement:\n"+sqlex);

	  	}
	  
	  	try{

			npbUpdateStatement.close();
 
	  	}catch(SQLException sqlex){

			Debug.error("Could not close Number Pool Block update statement:\n"+
					sqlex);

	  	}
	  	
	  	// release connection
	  	super.cleanup(success);

   	}
   	
	/**
	 * This method will return array of records which contains no of
	 * updated record in base table , Sv table and NPB table
	 * 
	 * @return int[]
	 */
	public int[] getRecords()
	{
		int [] recordsArr = new int[3];

		recordsArr[0] = super.getRecordsSkipped();

		recordsArr[1] = svRecordsUpdated ;

		recordsArr[2] = npbRecordsUpdated;

		return recordsArr;
   
	}		

}//end of class LRNSMURFTokenConsumer
