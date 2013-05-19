/**
 * This class uses NPASplitProcessor class to insert record in 
 * SOA_NPA_SPLIT_NXX table.
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.framework.db.DBInterface
 * @see com.nightfire.framework.util.Debug
 * 
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			06/24/2004			Created
	2			Ashok 			07/15/2004			FI review comment 
													incorporated
	
  
 */

package com.nightfire.spi.neustar_soa.file;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public class NPASplit {

   
   /**
    * This is the command-line interface to insert record
    *
    * @param args String[] the command line arguments. See the usage string for
    *                      description.
    */
   	public static void main( String[] args )
   	{
		
		// Database name
      	String dbname = null;
      	
      	// User Name
      	String dbuser = null;
      	
      	// Password
      	String dbpass = null;
      	
		String usage = "java "+NPASplit.class.getName()+
							 " [-verbose] <db name> <db user> <db password>";
		
		if( args.length == 3 || args.length == 4 )
		{
				
	     	for( int i = 0; i < args.length; i++ )
	     	{
	
		        if( args[i].equals("-verbose") ){
		        	
		            Debug.enableAll();
		            
		        }
		        else if( dbname == null ){
		         	
		           dbname = args[i];
		           
		        }
		        else if( dbuser == null ){
		        	
		           dbuser = args[i];
		           
		        }
		        else if( dbpass == null ){
		        	
		            dbpass = args[i];
		            
		        }
	
	      	}
	      	
		} else 
		{
			
			System.err.println( usage );
			System.exit( -1 );
			
		}
      	
      	// check to see if all command line parameters were given, and if not,
      	// print usage string and exit
      	if(	dbname == null 	||
         	dbuser == null 	||
         	dbpass == null )
      	{

         	System.err.println( usage );
         	System.exit( -1 );

      	}
      
		NPASplitProcessor npaSplitProcessor = null ;
		
		boolean success = true;

      	try
      	{

         	// Intialize the database interface. This only needs to be done once
         	// per java process.
         	DBInterface.initialize( dbname, dbuser, dbpass );
         
			npaSplitProcessor = new NPASplitProcessor();
         
			npaSplitProcessor.init();
         	
         	npaSplitProcessor.process( );
         	
         	success = true;
                

      	}
      	catch( FrameworkException fex )
      	{
		
			success = false ;
		
			// we don't want to lose any valuable stack trace
	 		// info about where this exception came from
	 		Debug.logStackTrace( fex );	 
	

      	}
      	finally
	  	{

			npaSplitProcessor.cleanup( success );

	 	}

   	}

}
