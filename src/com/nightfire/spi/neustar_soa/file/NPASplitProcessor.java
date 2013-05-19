/**
 * This class used to insert record(s) in SOA_NPA_SPLIT_NXX table if
 * any record found in SOA_NPANXX table for OldNPA and NewNPA's NXX
 *    
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants
 * @see com.nightfire.framework.util.FrameworkException
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			06/24/2004			Created
	2			Ashok			07/14/2004			Review comments incorporated
  
 */

package com.nightfire.spi.neustar_soa.file;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class NPASplitProcessor extends DBBase 
{
	/**
	 * This select query used to select OldNPA and NewNPA
	 */
	private static final String SELECT_NPA_SQL = "SELECT "+
										   	 	SOAConstants.NEWNPA_COL + ", " +
										   	 	SOAConstants.OLDNPA_COL +
											 	" FROM "+
											 	SOAConstants.NPA_SPLIT_TABLE ;
	/**
	 * This select query used to select NXX
	 */
	private static final String SELECT_NXX_SQL = "SELECT /*+ INDEX(" + SOAConstants.NPANXX_TABLE+ " SOA_NPANXX_INDEX_1) */ "+
											 	SOAConstants.NXX_COL +
											 	" FROM "+
											 	SOAConstants.NPANXX_TABLE +
											 	" WHERE " +
											 	SOAConstants.NPA_COL +
											 	" = ?";
	/**
	 * This select query used to select NPA ,NXX
	 */
	private static final String SELECT_SQL 	 = 	"SELECT /*+ INDEX(" + SOAConstants.NPANXX_TABLE + " SOA_NPANXX_INDEX_1) */ "+
											   SOAConstants.NPA_COL + ", "+
											   SOAConstants.NXX_COL +
											   " FROM "+
											   SOAConstants.NPANXX_TABLE +
											   " WHERE " +
											   SOAConstants.NPA_COL +
											   " = ? and " +
											   SOAConstants.NXX_COL +
											   " = ? " ;
	/**
	 * This select query used to insert record 
	 */
	private static final String INSERT_SQL  =  "INSERT INTO " +
											  SOAConstants.NPA_SPLIT_NXX_TABLE + 
											  " values (? ,? ,?)" ;
	
	/**
	 * This Prepared Statement used to select OldNPA and NewNPA
	 */										 
	private PreparedStatement selectNPAStatement = null;
	
	/**
	 * This Prepared Statement used to select NXX
	 */
	private PreparedStatement selectNXXStatement = null;
	
	/**
	 * This Prepared Statement used to select NPA and NXX
	 */
	private PreparedStatement selectStatement = null;
	
	/**
	 * This Prepared Statement used to insert record
	 */
	private PreparedStatement insertStatement = null;
	
	/**
	 * This method used to initialize Prepared Statement(s)
	 * 
	 * @throws FrameworkException when initialization fails
	 */										 
	public void init()throws FrameworkException
	{
		
		// not required to give file name so passing null value
		super.init( );
		
		Debug.log( Debug.SYSTEM_CONFIG, "NPASplitProcessor: Initializing..." );
		
		try
		{
			
			selectNPAStatement = prepareStatement( SELECT_NPA_SQL );
			
		}
		catch ( SQLException sqlex ) {

			Debug.error( "Could not prepare Select statement :\n"
						+ SELECT_NPA_SQL
						+ "\n"
						+ sqlex );

		}
		
		try
		{
	
			selectNXXStatement = prepareStatement( SELECT_NXX_SQL );
	
		}
		catch ( SQLException sqlex ) {

			Debug.error( "Could not prepare Select statement :\n"
						+ SELECT_NXX_SQL
						+ "\n"
						+ sqlex );

		}
		
		try
		{
	
			selectStatement = prepareStatement( SELECT_SQL );
	
		}
		catch ( SQLException sqlex ) {

			Debug.error( "Could not prepare Select statement :\n"
						+ SELECT_SQL
						+ "\n"
						+ sqlex );

		}
		
		try
		{
	
			insertStatement = prepareStatement( INSERT_SQL );
	
		}
		catch ( SQLException sqlex ) {

			Debug.error( "Could not prepare insert statement :\n"
						+ INSERT_SQL
						+ "\n"
						+ sqlex);

		}
		
		Debug.log( Debug.SYSTEM_CONFIG, 
									"NPASplitProcessor: Initialization done." );
		
	}
	
	/**
	 * This method first select OldNPA ,NewNPA from SOA_NPA_SPLIT table ,
	 * select NXX from SOA_NPANXX table for selected NewNPA.
	 * If any record found for selected OldNPA and NXX , 
	 * insert the record in SOA_NPA_SPLIT_NXX table.
	 *
	 * @param tokens String[] the tokenized line of input.
	 * @throws FrameworkException if the attempt to select and the attempt
	 *                            to insert both fail.
	 */
	public void process( ) throws FrameworkException
	{
		
		Debug.log( Debug.SYSTEM_CONFIG, 
								"NPASplitProcessor: Processing started..." );
								
		ResultSet selectNpaRs = null;
		
		ResultSet selectNxxRs = null;
		
		ResultSet selectRs = null;
						
		try
		{
			
			// Get result set which contains OldNPA and NewNPA
			selectNpaRs = selectNPAStatement.executeQuery();			
			
			String oldNpa = null;
			
			String newNpa = null;
			
			String nxx = null;
			
			// Loop for each record
			while( selectNpaRs.next() )
			{
				
				oldNpa = selectNpaRs.getString( SOAConstants.OLDNPA_COL );
				
				Debug.log( Debug.DB_DATA , " Old NPA value : "+oldNpa );
				
				newNpa = selectNpaRs.getString( SOAConstants.NEWNPA_COL );
				
				Debug.log( Debug.DB_DATA , " New NPA value : "+newNpa );
				
				// Set NewNPA value in SelectNXX Prepared statement
				selectNXXStatement.setString( 1 , newNpa );
				
				// Get ResultSet which contains NXX value
				selectNxxRs = selectNXXStatement.executeQuery();
			
				// loop for all NXX
				while( selectNxxRs.next() )
				{
					
					nxx = selectNxxRs.getString( SOAConstants.NXX_COL );
					
					Debug.log( Debug.DB_DATA , " NXX value : " + nxx );
					
					// set OldNPA in Select Prepared Statement
					selectStatement.setString( 1 , oldNpa );
					
					//	set NXX in Select Prepared Statement
					selectStatement.setString( 2 , nxx );
					
					// Get ResultSet
					selectRs = selectStatement.executeQuery();
					
					if( selectRs.next() )
					{
						
						try
						{
							// Set OldNpa
							insertStatement.setString( 1 , oldNpa );
							
							// Set NewNPA
							insertStatement.setString( 2 , newNpa );
							
							// Set NXX
							insertStatement.setString( 3 , nxx );
							
							insertStatement.execute();
							
							Debug.log( Debug.DB_DATA , 
										"Record successfully inserted in " +
										SOAConstants.NPA_SPLIT_NXX_TABLE + 
										" table for \n " +
										"OLDNPA = " + oldNpa +
										" , NEWNPA = " + newNpa +
										" , NXX = " + nxx );										
													
						}
						catch( SQLException sqlException )
						{
							
							selectRs.close();
							
							Debug.error( "Could not insert record in " +
								SOAConstants.NPA_SPLIT_NXX_TABLE + 
								" table because record already exist for \n " + 
								"OLDNPA = " + oldNpa +
								" , NEWNPA = " + newNpa +
								" , NXX = " + nxx );
							
						}
						
												
					}
					
					// closing Result Set
					selectRs.close();
					
					
				}
				 
				// Closing Result Set 
				selectNxxRs.close();
				
				 
			}
			
			// Closing Result Set
			selectNpaRs.close();
			
			
		}
		catch( SQLException sqlException )
		{
			
			throw new FrameworkException( " Could not process Npa " +
				"split :"+ sqlException.toString() );
			
		}finally
		{
			try
			{
				
				if(selectNxxRs != null)
				{
				
					selectNxxRs.close();
					selectNxxRs = null;
					
				}
				
				if(selectNpaRs != null)
				{
				
					selectNpaRs.close();
					selectNpaRs = null;
					
				}
				
			}catch (SQLException sqlException)
			{
				
				Debug.error( "Could not close result set:\n" + sqlException );
				
				
			}
			
			
		}
		
		Debug.log( Debug.SYSTEM_CONFIG, 
									"NPASplitProcessor: Processing ended..." );
		
	}
	
	/**
	 * Closes prepared statements and releases the database connection.
	 *
	 * @param success boolean whether or not the processing succeeded. This
	 *                        is used to determine whether we should rollback
	 *                        or commit.
	 */
	public void cleanup( boolean success ) 
	{

		try 
		{
		
			selectNPAStatement.close();	
			
			selectNXXStatement.close();	
			
			selectStatement.close();
			
			insertStatement.close();
			
			selectNPAStatement = null;	
			
			selectNXXStatement = null;
			
			selectStatement = null;
			
			insertStatement = null;

		} 
		catch ( SQLException sqlException ) {

			Debug.error( "Could not close statement:\n" + sqlException );

		}
	
		// release connection
		super.cleanup( success );

	}

}
