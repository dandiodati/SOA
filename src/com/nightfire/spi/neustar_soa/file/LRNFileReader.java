///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.file;

import java.io.FileNotFoundException;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * This class uses an LRN Token Consumer to update the SOA_LRN table based
 * on the contents of an LRN BDD file.
 */
public class LRNFileReader extends DelimitedFileReader{

   	/**
     * This reads the named file and updated the LRN table based on its
     * contents.
     *
     * @param file String an LRN BDD file
     * @param spid String only LRNs with this SPID value will be processed.
     * @param region String all LRNs will be assigned to this region.
     * @throws FileNotFoundException the named file was not found or was
     *                               inaccessable.
     * @throws FrameworkException thrown if some other error occurs during
     *                            processing. e.g. a database error or
     *                            bad formatting in the input file.
     */
   	public void read(String file, String region)
                       throws FileNotFoundException,
                              FrameworkException{

      	LRNTokenConsumer lrnConsumer = null;      	
      
      	boolean success = true;
      
      	try{ 		
      
			lrnConsumer = new LRNTokenConsumer( region );
			
			lrnConsumer.init(file);	
			
			super.read(file, lrnConsumer, SOAConstants.LRN_BDD_FILE_TYPE , 
															region , null );
	                
      	}catch( NumberFormatException nfex ){

			success = false;
	
			// we don't want to lose any valuable stack trace
			// info about where this exception came from
			Debug.logStackTrace( nfex );

		}catch( FrameworkException fex ){

		  	success = false;
	
		  	// we don't want to lose any valuable stack trace
		  	// info about where this exception came from
		  	Debug.logStackTrace(fex);		  

     	} catch (FileNotFoundException fex) {

			success = false;
	
			// we don't want to lose any valuable stack trace
			// info about where this exception came from
			Debug.log(
				Debug.ALL_ERRORS,
				"LRNFileReader:   "
					+ "Could not read file ["
					+ file
					+ "]: "
					+ fex);

		}
	 	finally{
	 		
			if( lrnConsumer != null)
			{
			
				lrnConsumer.cleanup(success);
	
			}		

	 	}

   	}

   	/**
     * This is the command-line interface for processing an LRN BDD file and
     * updated the DB with its contents.
     *
     * @param args String[] the command line arguments. See the usage string for
     *                      description.
     */
   	public static void main(String[] args){

      	String file = null;
      	
      	String region = null;

      	String dbname = null;
      	
      	String dbuser = null;
      	
      	String dbpass = null;

      	for(int i = 0; i < args.length; i++){

         	if( args[i].equals("-verbose") ){
            	
            	Debug.enableAll();
            	
         	}
         	else if( file == null ){
            	
            	file = args[i];
            	
         	}
         	else if(region == null){
            	
            	region = args[i];
            	
         	}
         	else if(dbname == null){
            
            	dbname = args[i];
            
         	}
         	else if(dbuser == null){
            
            	dbuser = args[i];
            
         	}
         	else if(dbpass == null){
            	
            	dbpass = args[i];
            	
         	}

      	}


		StringBuffer sb = new StringBuffer();
		
		sb.append("java ");
		
		sb.append(LRNFileReader.class.getName());
		
		sb.append( " [-verbose] <file name> <region> ");
		
		sb.append( "<db name> <db user> <db password>");

		String usage = sb.toString();

      	// check to see if all command line parameters were given, and if not,
      	// print usage string and exit
      	if(file   == null ||
          region == null ||
         dbname == null ||
         dbuser == null ||
         dbpass == null )
      	{

         	System.err.println(usage);
         
         	System.exit(-1);

      	}

      	LRNFileReader reader = new LRNFileReader();

      	try{

         	// Intialize the database interface. This only needs to be done once
         	// per java process.
         	DBInterface.initialize(dbname, dbuser, dbpass);
			
         	reader.read(file, region);

      	}
      	catch(NumberFormatException nfex){

         	System.err.println("["+region+"] is not a valid numeric region value.");

      	}
      	catch(FileNotFoundException fnfex){

         	System.err.println("Could not read file ["+file+"]: "+fnfex);

      	}
      	catch(FrameworkException fex){

         	System.err.println( fex.getMessage() );

      	}
      	catch(Exception ex){

         	ex.printStackTrace();

      	}

   	}

}
