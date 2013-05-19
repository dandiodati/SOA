 ///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.file;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * This class processed lines of input that we will simply to insert into
 * the SOA database.
 */
public abstract class NPACTokenConsumer extends DBTokenConsumerBase {

   	/**
     * This is the precompiled SQL statement that this class will
     * use to insert data into the DB.
     */
   	private PreparedStatement insertStatement;
   	
	
   	/**
     * This is the precompiled SQL statement that this class will
     * use to update the data in the DB.
     */
   	private PreparedStatement updateStatement;
   	
   	/**
     * The SQL update statement used to update the SOA_NBRPOOL_BLOCK data.
     */
   	private static final String UPDATE_NBRBLK = "update " + SOAConstants.NBRPOOL_BLOCK_TABLE +
   	" set LRN=?,CLASSDPC=?,CLASSSSN=?,CNAMDPC=?,CNAMSSN=?,ISVMDPC=?,ISVMSSN=?,"+
  	" LIDBDPC=?,LIDBSSN=?,WSMSCDPC=?,WSMSCSSN=?,NPBID=?, ACTIVATIONDATE=?," +
    " STATUS=?,CUSTOMERID=?,NPBTYPE=?,ALTERNATIVESPID=? where NPA=? and NXX=? and DASHX=? and SPID=?";

	/**
	 * This is the variable contains failed record(s).
	 */
   	public List failedList = null;

   	/**
   	 * 
   	 */
   	private String tokenType=null;
   	
   	/**
     *
     * @param filename String the filename is given because it may contain
     *                        important date information.
     * @throws FrameworkException
     */
   	public void init(String filename) throws FrameworkException{
		
      	
      	failedList = new LinkedList();
     
      	// initialize the DB connection
     	super.init(filename);

      	try{
      	    
      	    // This is for inserting the data into the database
         	insertStatement = prepareStatement(getInsertSQL());

      	}
      	catch(SQLException sqlex){

         	Debug.error("Could not prepare insert statement:\n"+
                     getInsertSQL()+
                     "\n"+sqlex);

      	}

   	}

   	/**
     * Called to process a line of data from a delimited file. This first
     * tries to insert the data into the database. If this fails (perhaps
     * because the row already exists), then an attempt is made to update
     * the row instead.
     *
     * @param tokens String[] the tokenized line of input.
     * @throws FrameworkException if the attempt to insert and the attempt
     *                            to update both fail.
     */
    public boolean process(String[] tokens,String tokenType) throws FrameworkException{
		
		// this flag will show whether inserted or updated succcessfuly 
		boolean errorFlag = false;
		 
		PreparedStatement updateNBRStatement = null;
		
		this.tokenType = tokenType;
		
	  	try {
	  	
	  	    
	  	  if(tokenType == null){

	  	      setInsertParams(insertStatement, tokens);
        	
	  	      insertStatement.execute();
	  	      
	  	  }
   	      else if(tokenType.equals(SOAConstants.LRNTOKEN) ||
		  	       tokenType.equals(SOAConstants.NPANXXTOKEN) ||
		  	       tokenType.equals(SOAConstants.NPANXXXTOKEN)||
		  	       tokenType.equals(SOAConstants.NBRBLKTOKEN)||
		  	       tokenType.equals(SOAConstants.SPIDTOKEN)) {
	  	      try{                                                                                           
	  	        
	  				// try to insert
            		setInsertParams(insertStatement, tokens);
            	
        			insertStatement.execute();
        		
       		
	  	       }catch(SQLException sqlIex){
	  	           
	  	         	  	         
	  	          if(tokenType.equals(SOAConstants.NBRBLKTOKEN))
	  	          {
	  	              
	  	              // This is for updating the data into the database
	  	              updateNBRStatement = prepareStatement(UPDATE_NBRBLK);
	  	               
	  	              setUpdateParams(updateNBRStatement, tokens);
	  	              
	  	              updateNBRStatement.execute();
  	                 
	  	                 
	  	          }
	  	          else  {	  	              
						// This is for updating the data into the database
						updateStatement = prepareStatement(getUpdateSQL());
					
						setUpdateParams(updateStatement, tokens);

						updateStatement.execute();	
						
	  	          }						  
	  	       }
   			}   
     	}
     	catch (Exception sqlex) {
     	   
         	if (Debug.isLevelEnabled(Debug.DB_STATUS)) {

           	Debug.log(Debug.DB_STATUS,
                     "Insert failed: " +
                     sqlex.getMessage() + "\nAdding in failed List ...");
	
			errorFlag = true;
			
			tokens[ tokens.length - 2 ] = sqlex.getMessage().trim();
			
			failedList.add(tokens);

        	}

    	}
     	finally{
     	    try {
     	        if ( updateNBRStatement != null)
     	            updateNBRStatement.close();
				if (updateStatement != null)
      				updateStatement.close();

     	    } catch (SQLException e) {}
     	}

    	return errorFlag;

   	}

   	/**
     *  Closes any prepared statements and releases the database connection.
     *
     * @param success boolean whether or not the processing succeeded. This
     *                        is used to determine whether we should rollback
     *                        or commit.
     */
   	public void cleanup(boolean success) {
   	    
   	    

      	try{     

      		if((tokenType != null) && (tokenType.equals(SOAConstants.LRNTOKEN) ||
		  	       tokenType.equals(SOAConstants.NPANXXTOKEN) ||
		  	       tokenType.equals(SOAConstants.NPANXXXTOKEN)||
		  	       tokenType.equals(SOAConstants.SPIDTOKEN))) 
			{
      			if(updateStatement != null)
      				updateStatement.close();
      	    }

         	insertStatement.close();
      	}
      	catch(SQLException sqlex){

         	Debug.error("Could not close insert statement:\n"+
                     sqlex);

      	}

     	// release connection
      	super.cleanup(success);

   	}

   	/**
     * This method must be implemented by child classed to provide the
     * implementation-specific SQL used to insert input into the database.
     *
     * @return String an SQL insert statement.
     */
   	protected abstract String getInsertSQL();
   	
   	/**
     * This method must be implemented by child classed to provide the
     * implementation-specific SQL used to update the date into the database.
     *
     * @return String an SQL update statement.
     */
   	protected abstract String getUpdateSQL();
   	
   	/**
     * This must be implemented by child classes to populate the
     * given statement with the given data.
     *
     * @param insertStatement PreparedStatement the statement whose parameters
     *                                           need to be populated.
     * @param params String[] the line of tokens whose values will be used
     *                         to populate the statement.
     * @throws SQLException thrown if the statement cannot be populated.
     * @throws FrameworkException thrown if some other error occurs.
     *
     */
   	protected abstract void setInsertParams(PreparedStatement insertStatement,
                                           String[] params)
                                           throws SQLException,
                                                FrameworkException;
   	
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
   	protected abstract void setUpdateParams(PreparedStatement updateStatement,
                                           String[] params)
                                           throws SQLException,
                                                FrameworkException;
    
    /**
     * This method will return failed records
     */
    public List getFailedList()
	{
		
		return failedList;
		
	}
	
}
