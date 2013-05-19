/**
 * This takes the NPANXX data from lines of an NPANXX SMURF file and then uses 
 * this data to update the SOA_NPANXX,
 * SOA_SUBSCRIPTION_VERSION tables in the database .
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
	3			Ashok			06/14/2004			Modified because 
													included report generation
													code
    4			Jigar Talati	03/17/2006			Added update statement for ONSP in SV Table.
 */

package com.nightfire.spi.neustar_soa.file;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NPANXXSMURFTokenConsumer extends SMURFTokenConsumer{

   	/**
     * The SQL update statement used to update existing rows of NPA_NXX data.
     */
   	public static final String UPDATE = "update /*+ index( "+
   									   SOAConstants.NPANXX_TABLE+
   									   " SOA_NPANXX_INDEX_1) */ "+
                                       SOAConstants.NPANXX_TABLE+
                                       " set "+
                                       SOAConstants.SPID_COL+
                                       " = ? "+                                       
                                       " where "+
									   SOAConstants.SPID_COL+
									   " = ? and "+
                                       SOAConstants.NPA_COL+
                                       " = ? and "+
                                       SOAConstants.NXX_COL+
                                       " = ? ";								   
									   
	/**
	 * The SQL update statement used to update existing rows of SV data.
	 */
   	public static final String SV_UPDATE = "update /*+ index( "+
									   SOAConstants.SV_TABLE+
									   " SOA_SV_INDEX_2 ) */ "+
									   SOAConstants.SV_TABLE+
									   " set "+
									   SOAConstants.SPID_COL+
									   " = ? , "+ 
									   SOAConstants.ONSP_COL+
									   " = ? "+                                      
									   " where "+
									   SOAConstants.SPID_COL+
									   " = ? and "+
									   SOAConstants.ONSP_COL+
									   " = ? and "+
									   SOAConstants.PORTINGTN_COL+" LIKE "+
									   " ? and ("+
									   SOAConstants.STATUS_COL+
									   " IN('active','disconnect-pending',"+
									   "'download-failed-partial') OR ("+
									   SOAConstants.STATUS_COL+
									   " ='old' and "+
									   SOAConstants.FAILED_SP_FLAG_COL+
									   " = 1 ))";
									   
	/**
	 * The SQL update statement used to update the only ONSP value in the SV table. 
	 */
   	
   	public static final String SV_UPDATE_ONSP = "update /*+ index( "+
   										   SOAConstants.SV_TABLE+
   										   " SOA_SV_INDEX_2) */ "+
										   SOAConstants.SV_TABLE+
										   " set "+
										   SOAConstants.ONSP_COL+
										   " = ? "+                                      
										   " where "+
										   SOAConstants.ONSP_COL+
										   " = ? and "+
										   SOAConstants.PORTINGTN_COL+" LIKE "+
										   " ? and ("+
										   SOAConstants.STATUS_COL+
										   " IN('active','disconnect-pending',"+
										   "'download-failed-partial') OR ("+
										   SOAConstants.STATUS_COL+
										   " ='old' and "+
										   SOAConstants.FAILED_SP_FLAG_COL+
										   " = 1 ))";
   	
   
	/**
	 * This is the precompiled SQL statement that this class will
	 * use to update data in SV table.
	 */
    private PreparedStatement updateStatement= null;
    
    private PreparedStatement updateONSP = null;
    
	/**
	 * this variable contains records updated in SV table
	 */
	protected  int svRecordsUpdated = 0;
    
	/**
     *
     * @param filename String the filename is given because it may contain
     *                        important date information.
     * @param fileType	String gives the fileType
     * 
     * @return void
     * @throws FrameworkException
     */
    public void init(String filename , String fileType) 
    										throws FrameworkException{

		// initialize the DB connection
		super.init(filename , fileType);
		 
		try{
		 	
			updateStatement = prepareStatement(SV_UPDATE);	
		 	
			updateONSP = prepareStatement(SV_UPDATE_ONSP);
		 		   
		}catch(SQLException sqlex){

			Debug.error("Could not prepare update statement for SV:\n"+
					SV_UPDATE+
					"\n"+sqlex);
		}

	}
	
   	/**
	 * Called to process a line of data from a delimited file.If record found
	 * then update SV  record
	 *
	 * @param tokens String[] the tokenized line of input.
	 * 
	 * @return boolean
	 * @throws FrameworkException if the attempt to update fail.
	*/
   public boolean process(String[] tokens, String tokenType ) throws FrameworkException{
   	
		boolean errorFlag = super.process( tokens, null);
		
		try{
	   		
			setSVUpdateParams(updateStatement, tokens);
   			
			if( updateStatement.executeUpdate() > 0 )
			{
       			
				svRecordsUpdated ++ ;				
       			 
			}
   			
			setSVONSPUpdateParams(updateONSP, tokens);
			
			if( updateONSP.executeUpdate() > 0 )
			{
			   svRecordsUpdated ++ ;
			}
			
	   	}catch (SQLException sqlex2) {
	
			errorFlag = true;	
	  
   		}
   		
   		return errorFlag; 	
   	
   	}	  
   
   
   	private void setSVONSPUpdateParams(PreparedStatement updateONSP, String[] params) throws 
   																SQLException, FrameworkException{
   		
   		int i = 0;
	  	
	  	try{
	  	
		updateONSP.setString(++i,params[1]);

		updateONSP.setString(++i,params[0]);
  		
		updateONSP.setString(++i,
					params[2].substring(0,3)+"-"+params[2].substring(3)+"%");
					
		}catch(SQLException sqle){
	  	
			throw new FrameworkException("Unable to set values for " +
														"updateStatement : "+sqle);
		}		  	
	}

	/**
     * Returns the SQL statement used to update a row of data in the NPANXX
     * table.
     *
     * @return String the SQL update statement.
     */
   	protected String getUpdateSQL(){

      	return UPDATE;

   	}
	 

   	/**
     * This method is used to populate Prepared Statement to update NPANXX table
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

	  	updateStatement.setString(++i,params[2].substring(0,3));
	  
	  	updateStatement.setString(++i,params[2].substring(3));
	  	
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
  		
		updateStatement.setString(++i,
					params[2].substring(0,3)+"-"+params[2].substring(3)+"%");
					
		}catch(SQLException sqle){
	  	
			throw new FrameworkException("Unable to set values for " +
														"updateStatement : "+sqle);
		}		  	
		  	
   	}  
	
   	/**
     * Closes any prepared statements and releases the database connection.
     *
     * @param success boolean whether or not the processing succeeded. This
     *                        is used to determine whether we should rollback
     *                        or commit.
     * 
     * @return void
     */  								 
   	public void cleanup(boolean success) {

		try{
      	
			updateStatement.close();
			updateONSP.close();
		}catch(SQLException sqlex){

			Debug.error("Could not close SV update statement:\n"+
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
	
		recordsArr[2] = 0;
	
		return recordsArr;
	   
	}	  								 
	  								 
}//end of class NPANXXSMURFTokenConsumer
