///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.file;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;

import com.nightfire.framework.util.Debug;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.file.TokenConsumer;

/**
 * This is a generic class for reading in a file line by line,
 * tokenizing the line based on a delimiter, and then passing
 * the tokens off to an abstract process method for specific processing.
 */
public class DelimitedFileReader {
	private String className="DelimitedFileReader";
	private String methodName=null;
	// It holds the status whether process was successful or not.
	boolean success = true;

	FileReader reader = null;
	LineNumberReader buffer = null;
	// These dates are used to get start time and end time
	Date startTime, endTime;
	// A list for holding the tokens
	List list = null;

	// This variable used to count no of records in download file
	int recordsRead = 0;

	// This variable used to count not of records updated or inserted
	int recordUpdated = 0;
	// This variable used to count no of records failed
	int recordFailed = 0;
	// This determines whether the BDD data insertion is success or not.
	protected boolean bddStatus=true;
   	/**
     * The delimiter used in all of the NPAC files is a pipe, |, so
     * we will use this as the default delimiter.
     */
   	public static final String DEFAULT_DELIMITER = "|";

   	/**
     * The delimiter used in tokenizing each line.
     */
   	private String delimiter;

    /**
     * Constructs a file reader using the default delimiter.
     *
     */
   	public DelimitedFileReader(){

      	this(DEFAULT_DELIMITER);

   	}
	public void init(String file) throws FileNotFoundException {
		reader = new FileReader(file);
		buffer = new LineNumberReader(reader);
	}

   	/**
     * Constructs a file reader that will use the given delimiter.
     *
     * @param delimiter String the string (usually a single char) that
     *                         indicates where one token starts and another
     *                         begins in a line of input.
     */
   	public DelimitedFileReader(String delimiter){
		 
      	this.delimiter = delimiter;

   	}
     /**
     * This will be used to read a line in notification BDD file and also it
     * passes that read data to consumer to process for inserting it into the
     * respective tables.
     * 
     * @param consumer
     *            will process the data that is read from a file.
     * @param reportFile
     *            contains the name of report.
     * @throws IOException
     *             will be thrown if file cann't find or read.
     * @throws FrameworkException
     *             will be thrown if any application related errors occurred.
     *             Ex: while converting from a string to date format.
     */
    public void read(NotificationTokenConsumer consumer,
            		String reportFile) throws IOException, FrameworkException {
	    try {
		
		methodName="read";
		
		String line = buffer.readLine();

		    startTime = new Date();

		    Debug.log(Debug.SYSTEM_CONFIG, className + "[" + methodName +
			       "] Processing started....");
		    // This gets the database connection.
		    consumer.init();
		    
		    // It will be encountered when line is null.
		    while (line != null) 
		    {
		       		        
			    if (line.length() == 0)
				    break;
			    list = new LinkedList();
			    
			    recordsRead++;
			    // tokenize the line of input

			    consumer.bddToken=line;
			    
			    StringTokenizer tokens = new StringTokenizer(line, delimiter,
					    true);

			    // get the tokens
			    getTokens(tokens, list);

			    // copy the tokens into an array
			    String[] array = new String[list.size() + 2];

			    list.toArray(array);
						    
			    consumer.errorFlag=false;
			    
			    boolean errorFlag = consumer.process(array);

			    array[list.size() + 1] = line;

			    if (errorFlag) {
				    recordFailed++;
			    }
			    // clear out the result list
			    list.clear();
			    // read next line
			    line = buffer.readLine();
		    }
		    if (recordsRead > 0) {
			
			    recordUpdated = recordsRead - recordFailed;
		    }
		    
		    else {
		    
		        Debug.log(Debug.SYSTEM_CONFIG, className + "[" + methodName +
			       "] There is no records read, The BDD file might be empty....");
		        
		        System.out.println("***** UNSUCCESS:The Bdd data insertion has" +
	            " been failed! Check .log file to see the more details");
		        
		        System.exit(-1);
		    }    
		    
		    endTime = new Date();
		    
		    List failedList = (LinkedList) consumer.getFailedList();
					    
		    try
		    {
			
			if(recordFailed!=0 && failedList.size()!=0){
			    
			 NotificationReportGenerator reportGenerator = new
				NotificationReportGenerator(reportFile);
			
			    reportGenerator.addBody(failedList);
			    
			    reportGenerator.summary(startTime, endTime, recordsRead,
				    recordUpdated, recordFailed, null);
			    
			    reportGenerator.generateReport();
			    
			    bddStatus=false;
			    
			}
		    } catch (FrameworkException fe) {

			    throw new FrameworkException(" Could not generate report : "
					    + fe.getMessage());
		    }
		    Debug.log(Debug.SYSTEM_CONFIG,className + "[" + methodName +
			       "] Processing ended....");
		    
	    } catch (IOException ioex) {

		    success = false;
		    
		    Debug.logStackTrace(ioex);
		    
		    throw new FrameworkException(ioex.getMessage());
		    
	    } catch (FrameworkException fex) {
		    
		success = false;
		    // we don't want to lose any valuable stack trace
		    // info about where this exception came from
		    Debug.logStackTrace(fex);
		    // add in the line number of the line that failed
		    throw new FrameworkException("Line " + buffer.getLineNumber()
				    + ": " + fex.getMessage());
	    }
    }



    /**
     * This reads the file with the given name line by line, and tokenizes
     * each line based on this reader's delimiter. The list of tokens is
     * then passed to the abstract process() method for processing.
     *
     * @param file String the path to the file to be read.
     * @param consumer TokenConsumer will process the file
     * @param fileType String will contain file type
     * @param region String region for which current file belongs
     * @param spid String will contain spid value
     * @throws FileNotFoundException thrown if the file cannot be found.
     * @throws FrameworkException
     */
   	public void read( String file,TokenConsumer consumer , String fileType , 
                    						String region , String spid )
                    						throws FileNotFoundException,
                           					FrameworkException{

      	// whether processing was successful or not
      	boolean success = true;
	  		  		
      	FileReader reader = new FileReader(file);      
      
      	LineNumberReader buffer = new LineNumberReader(reader);
      
      	// These dates are used to get start time and end time 
	  	Date startTime , endTime ;      
      
      	// a list for holding the tokens
      	List list = new LinkedList();
      
      	// this variable used to count no of records in download file
      	int recordsRead =0;
      
      	// this variable used to count not of records updated or inserted
      	int recordUpdated=0;
      
      	// this variable used to count no of records failed
      	int recordFailed=0;        

      	try
      	{

         	String line = buffer.readLine();
         	
			startTime = new Date();
			
			Debug.log(Debug.SYSTEM_CONFIG , "Processing started....");
		
         	while(line != null)
         	{
         		
				recordsRead++;
				
	            // tokenize the line of input
	            StringTokenizer tokens 
	            				= new StringTokenizer(line, delimiter, true);
	
	            // get the tokens
	            getTokens( tokens, list );
	
	            // copy the tokens into an array
	            String[] array = new String[ list.size() + 2 ];
	            
	            list.toArray( array );
	            
	            boolean errorFlag =false;
	                
	            if(consumer instanceof LRNTokenConsumer)
	            
	                errorFlag = consumer.process( array, SOAConstants.LRNTOKEN);
	            
	            else if(consumer instanceof NPANXXTokenConsumer)
	            	
	            	errorFlag = consumer.process( array, SOAConstants.NPANXXTOKEN);
	            
	            else if(consumer instanceof NPANXXXTokenConsumer)
	            
	            	errorFlag = consumer.process( array, SOAConstants.NPANXXXTOKEN);
	            
	            else if(consumer instanceof NBRPoolBlockTokenConsumer)
	                
	                errorFlag = consumer.process( array, SOAConstants.NBRBLKTOKEN);

				else if(consumer instanceof SPIDTokenConsumer){
	                
	                errorFlag = consumer.process( array, SOAConstants.SPIDTOKEN);
	            }
	            
	            else
                
	                errorFlag = consumer.process( array,null);
	            
				array[ list.size() + 1 ] = line ;
	            
	            if( errorFlag )
	            {
	            	recordFailed++;
	            	
	            }
	            	
	            // clear out the result list
	            list.clear();
	
	            // read next line
	            line = buffer.readLine();

	        }
	     	
	     	// create int array to store counts to be used MASS SPID UPDATE
			int [] recordsArr = new int[3];
	     	
	     	if( recordsRead > 0 )
	     	{
				recordUpdated = recordsRead - recordFailed ;
	     		
				if(	fileType.equalsIgnoreCase( 
										SOAConstants.LRN_FILE_TYPE)  	|| 
					fileType.equalsIgnoreCase(
										SOAConstants.NPA_NXX_FILE_TYPE) || 
					fileType.equalsIgnoreCase( 
								SOAConstants.NPA_NXX_X_FILE_TYPE )  ||
					fileType.equalsIgnoreCase( 
								SOAConstants.SV_BDD_FILE_TYPE ) ||
				    fileType.equalsIgnoreCase( 
							SOAConstants.NPB_BDD_FILE_TYPE ))
			    
					
				{
	     		
					recordsArr =  consumer.getRecords();
	     			
	     			// actual records updated for mass spid update
					recordUpdated = recordUpdated - recordsArr[0];
	     			
				}
	     		
	  		}
	  		
		    endTime =new Date();
			
			List failedList = (LinkedList)consumer.getFailedList();
			
			try
			{			
			
				ReportGenerator reportGenerator 
				= new ReportGenerator( file , fileType , region , spid );
				
				reportGenerator.titleHeader( );
					
				reportGenerator.addBody( failedList );
						
				reportGenerator.summary( startTime ,endTime , recordsRead , 
											recordUpdated,recordFailed ,
											recordsArr );
				
				reportGenerator.generateReport( );
			
			}catch (FrameworkException fe )
			{
				
				throw new FrameworkException( " Could not generate report : "
											+ fe.getMessage() );
			}
				
			Debug.log(Debug.SYSTEM_CONFIG , "Processing ended....");
			
      	}
      	catch(IOException ioex){

         	success = false;
         	
         	throw new FrameworkException(ioex.getMessage());

      	}
      	catch(FrameworkException fex){

         	success = false;

         	// we don't want to lose any valuable stack trace
         	// info about where this exception came from
         	Debug.logStackTrace(fex);

         	// add in the line number of the line that failed
         	throw new FrameworkException("Line "+buffer.getLineNumber()+
                                      ": "+fex.getMessage());

      	}finally{
      		if(reader != null){
      			try {
					reader.close();
				} catch (IOException e) {
					
					if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
						Debug.log(Debug.ALL_ERRORS, className + " : Exception occures at the time to close the " +
								"stream..." +e.getMessage());
					}
				}
      		}
      		if(buffer != null){
      			try {
					buffer.close();
				} catch (IOException e) {
					if (Debug.isLevelEnabled(Debug.ALL_ERRORS)) {
						Debug.log(Debug.ALL_ERRORS, className + " : Exception occures at the time to close the " +
								"stream..." +e.getMessage());
					}
				}
      		}
      	}	

   	}

   	/**
     * This adds the String tokens found in the given tokenizer to
     * the given results list.
     *
     * @param tokens StringTokenizer a tokenized line of input.
     * @param results List the individual token values will be added to this
     *                     list.
     */
   	private void getTokens(StringTokenizer tokens, List results){

      	String next;
      	
      	String last = delimiter;

      	while( tokens.hasMoreTokens() ) {

         	next = tokens.nextToken();

         	// Check to see if next token is a delimiter.
         	if( next.equals(delimiter) ){

            	// We ignore delimiters, unless the last token was a delimiter too.
            	if( last.equals(delimiter) ) {

               	// We found two consecutive delimiters. This indicates an
               	// empty field, so we will add an empty value to the results.
               	results.add("");
            	}

         	}
         	else{

            	// add the token
            	results.add( next );

         	}

         	last = next;

      	}

      	if ( last.equals(delimiter) ) {

         	// If the last character on a line is the delimiter,
         	// this means that there was one last empty field.
         	results.add("");

      	}

   	}

   	/**
     * This utility method is used to create an String out of a String array
     * for logging purposes.
     *
     * @param array String[] an array of strings.
     * @return String all of the elements found in the array delimited by commas.
     */
   	public static String getString(String[] array){

      	StringBuffer result = new StringBuffer();

      	for(int i = 0; i < array.length; i++){

         	if(i > 0){
            	
            	result.append(", ");
            	
         	}

         	result.append( array[i] );

      	}

      	return result.toString();

   	}
   
}
