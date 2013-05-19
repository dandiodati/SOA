/**
 * This class processed lines of input that we will simply to update into
 * the SOA database.
 * 
 * @author 		Ashok Kumar
 * @version		1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.spi.neustar_soa.file.DBTokenConsumerBase
 * @see			com.nightfire.spi.neustar_soa.utils.SOAConstants
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			05/04/2004			Created
	2			Ashok			05/11/2004			Removed methods for NPB & SV
	3			Ashok			06/21/2004			Added logic for report 
  
 */

package com.nightfire.spi.neustar_soa.file;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public abstract class SMURFTokenConsumer extends DBTokenConsumerBase {

	/**
	 * this variable contains failed records
	 */
	protected  List failedList = null;
	
	/**
	 * this variable contains records skipped
	 */
	protected  int recordSkipped = 0;
	/**
	 * this variable contains file type
	 */
	protected String fileType = null;
	
	/**
	 * This is the precompiled SQL statement that this class will
	 * use to update data into the DB.
	 */
	private PreparedStatement updateStatement;
   
   	/**This method will initialize the DB connection 
     * by calling base class method
     *
     * @param filename String the filename is given because it may contain
     *                        important date information.
     * @param fileType String gives file type
     * @throws FrameworkException
     */
   	public void init(String filename , String fileType) 
   										throws FrameworkException {
			
		failedList = new LinkedList();
		
		this.fileType = fileType;
		
      	// initialize the DB connection
      	super.init(filename);
      
		try{
		 	
			updateStatement = prepareStatement(getUpdateSQL());	
 	
 		   
	 	}catch(SQLException sqlex){

			Debug.error("Could not prepare update statement :\n"+
						getUpdateSQL()+
						"\n"+sqlex);

		}
   	}

   	/**
     * Called to process a line of data from a delimited file.If record found
     * then update that record
     *
     * @param tokens String[] the tokenized line of input.
     * @throws FrameworkException if the attempt to insert and the attempt
     *                            to update both fail.
     */
   	public boolean process( String[] tokens, String tokenType ) throws FrameworkException{				
		
		boolean errorFlag = false;	
		
        try {	
        		
           		setUpdateParams(updateStatement, tokens);
           		 
           		if(! ( updateStatement.executeUpdate() > 0 ) )
           		{
           			
					recordSkipped ++ ;
           			 
           		}
           		
		       	
		}catch (SQLException sqlex2) {

			errorFlag = true;
					
			if(fileType.equalsIgnoreCase(SOAConstants.LRN_FILE_TYPE))
			{
			
				tokens[3] = sqlex2.getMessage().trim() 
							+" while updating [ " 
							+ SOAConstants.LRN_TABLE + " ] ,[ " 
							+ SOAConstants.SV_TABLE + " ] , [ "
							+ SOAConstants.NBRPOOL_BLOCK_TABLE + " ] tables. " ;
				 
			}else if(fileType.equalsIgnoreCase(
									SOAConstants.NPA_NXX_FILE_TYPE)) 
			{
				
				tokens[3] = sqlex2.getMessage().trim() 
									+" while updating [ " 									 
									+ SOAConstants.NPANXX_TABLE + " ] , [ "
									+ SOAConstants.SV_TABLE + " ] tables. " ;
				
			}else if( fileType.equalsIgnoreCase(
									SOAConstants.NPA_NXX_X_FILE_TYPE))
			{
				
				tokens[3] = sqlex2.getMessage().trim() 
							+" while updating [" 
							+ SOAConstants.NPANXXX_TABLE +"] table. " ;
				
			}
		
			failedList.add(tokens);	

        } 
		
		return errorFlag;
        
   	}

   	/**
     * Closes any prepared statements and releases the database connection.
     *
     * @param success boolean whether or not the processing succeeded. This
     *                        is used to determine whether we should rollback
     *                        or commit.
     */
   	public void cleanup(boolean success) {

      	try{
      	
         	updateStatement.close();
         
      	}
      	catch(SQLException sqlex){

         	Debug.error("Could not close update statement:\n"+
                     sqlex);

      	}
      	// release connection
      	super.cleanup(success);

   	}
   
	/**
	 * This method will return failed records
	 * 
	 * @return list of failed records
	 */   
   	public List getFailedList()
   	{
   	
   		return failedList;
   	
   	}
   	
	/**
	 * This method will return count of updated records
	 * 
	 * @return int number of updated records
	 */   
	public int getRecordsSkipped()
	{

		return recordSkipped;

	}
   
   	/**
     * This method must be implemented by child classed to get the
     * implementation-specific SQL used to update existing data in the database.
     *
     * @return String an SQL update statement.
     */
   	protected abstract String getUpdateSQL();	

   	/**
     * This must be implemented by child classes to populate the
     * given statement with the given data. 
     *
     * @param updateStatement PreparedStatementthe statement whose parameters
     *                                             need to be populated.
     * @param params String[] the line of tokens whose values will be used
     *                        to populate the statement.
     * @throws SQLException thrown if the statement cannot be populated.
     * @throws FrameworkException thrown if some other error occurs.
     */
   	protected abstract void setUpdateParams(PreparedStatement updateStatement,
                                           String[] params)
                                           throws SQLException,
                                                  FrameworkException; 
	
}//end of class SMURFTokenConsumer
