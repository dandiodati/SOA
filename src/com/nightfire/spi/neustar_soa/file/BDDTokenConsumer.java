/**
 * This class processes the line of input that is used to update 
 * NPANXX_X table.
 * 
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.spi.neustar_soa.file.NPACTokenConsumer
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			05/06/2004			Created
	2			Ashok			07/05/2004			Review comments incoporated
	3			D.Subbarao		02/15/2006.			Added a new parameter to
	 												process() of parent class.
    4 			D.Subbarao		02/16/2006			Modified process method.	 	 												
  
 */

package com.nightfire.spi.neustar_soa.file;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;


public abstract class BDDTokenConsumer extends NPACTokenConsumer {

    /**
     * This is the precompiled SQL statement that this class will
     * use to update data into the DB.
     */
   	private PreparedStatement updateStatement;
   
   	/**
     *  This variable contains true for SV file and false for NPBfile
     */
   	protected boolean svBDD;
   
   	/**
     * variable contains NPA value
     */
   	private String npa 	= null;
   
   	/**
     * variable contains NXX value
     */
   	private String nxx 	= null;
   
   	/**
     * variable contains DASHX value
     */
   	private String dashx = null;   
   
   	/** 
   	 * variable contains Token Type value. 
   	 */
   	protected String tokenType=null;
   	
   	/**
     *
     * @param filename String the filename is given because it may contain
     *                        important date information.
     * @throws FrameworkException
     */
   	public void init(String filename) throws FrameworkException {

      	// initialize the DB connection
      	super.init(filename);

      	try
      	{
   	        updateStatement = prepareStatement( getUpdateSQL() );
         	
      	}
      	catch(SQLException sqlex){

         	Debug.error("Could not prepare update statement:\n"+
                     getUpdateSQL()+
                     "\n"+sqlex);
    	}
      	

   	}

   	/**
     * Called to process a line of data from a delimited file. This first
     * tries to insert the data into the database. If this fails (perhaps
     * because the row already exists), then create the record in log file.
     *
     * @param tokens String[] the tokenized line of input.
     * @param tokenType containts the type of BDD tokens.
     * @return boolean 
     * @throws FrameworkException if the attempt to insert failed.
     */
   	public boolean process(String[] tokens, String tokenType) throws FrameworkException{

      	boolean errorFlag;
      	
      	this.tokenType = tokenType;
      	
      	
      	// It contains null when BDD is SVTokenConsumer
      	if(tokenType==null)     	    
      	    errorFlag=super.process(tokens, null);
      	// It contains the token when the BDD type is NBRPoolBlockTokenConsumer
      	else
      	    errorFlag=super.process(tokens, tokenType);
      
      	try
      	{
      	    
      		if( !errorFlag )
			{
				// If SV BDD
				if( svBDD ){		
					
					// to check whether record already updated or not
					String npa   = tokens[1].substring( 0, 3 );
						 
					String nxx 	 = tokens[1].substring( 3, 6 );

					String dashx = tokens[1].substring( 6,7 );		
	      	   
					if(	this.npa != null && 
						this.nxx != null && 
						this.dashx != null )
					{						
						
						if(	!(this.npa.equals(npa) && 
							 this.nxx.equals(nxx) && 
							 this.dashx.equals(dashx))) 
						{	
						
				           // Update NPANXX_X table
				           setUpdateParams(updateStatement, tokens);
				           
				           updateStatement.execute();
				           
						}
						
					}else{
						
						// Update NPANXX_X table
						setUpdateParams(updateStatement, tokens);
				           
						updateStatement.execute();
						
					}
					
					this.npa = npa;
					
					this.nxx = nxx;
					
					this.dashx = dashx;
					
				}else{
					
				    this.tokenType=null;
				    
					//	Update NPANXX_X table
					setUpdateParams(updateStatement, tokens);
				           
					updateStatement.execute();
					
				}
				
			}
			
        }catch (SQLException sqlex2) 
        {

           String error =
              "Could not update database with values: " +
              DelimitedFileReader.getString(tokens) +
              ": " + sqlex2;

           Debug.error( error );
           
		   throw new FrameworkException("Could not prepare statement : "+sqlex2);

        }        
	
		return errorFlag;
	
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
   	public void cleanup( boolean success ) {
	
	  	try{
	  	
         	updateStatement.close();
         
      	} catch(SQLException sqlex){

         	Debug.error( "Could not close update statement:\n"+ sqlex );

      	}

      	// release connection
      	super.cleanup(success);

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
     * 
     * @return void
     * @throws SQLException thrown if the statement cannot be populated.
     * @throws FrameworkException thrown if some other error occurs.
     */
   	protected abstract void setUpdateParams(PreparedStatement updateStatement,
                                           	String[] params)
                                           	throws  SQLException,
                                                  	FrameworkException;
   


}
