package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

public class SOADateComparator extends MessageProcessorBase{
	
    /**
     * The condition to evaluate (iterative).
     */
    public static final String CONDITION_PROP = "TEST";
    /**
     * The next processor to route the message to if the condition evaluates to 'true' (iterative).
     */
	public static final String NEXT_PROCESSOR_PROP = "NEXT_PROCESSOR";

    /**
     * The default processor to route to when all conditions are false.
     */
    public static final String DEFAULT_NEXT_PROCESSOR_PROP = "DEFAULT_NEXT_PROCESSOR";

    /**
     * Flag indicating if an exception should be thrown if no route is found (and default isn't given).
     */
    public static final String THROW_EXCEPTION_FOR_NO_ROUTE_FLAG_PROP = "THROW_EXCEPTION_FOR_NO_ROUTE_FLAG";
    
    /**
     * Flag indicating if time portion of date should be used.
     */
    public static final String USE_TIME_PORTION = "USE_TIME_PORTION";
    
    /**
     * Flag indicating if time portion of date should be used.
     */
    public static final String COMPARE_DATE_FORMAT = "COMPARE_DATE_FORMAT";
    
    /**
     * Default date format value.
     */
    private static final String defaultDateFormat = "MM-dd-yyyy-hhmmssa"; 
    
    /**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;

	
    /**
     * Constructor.
     */
    public SOADateComparator ()
    {
        loggingClassName = StringUtils.getClassName( this );
    }
    
    private String test;
    private String nextProc;
    private String defaultNextProcessorName;
    private boolean throwExceptionForNoRouteFlag = false;
    private String firstDate;
    private String matchDate;
    private String firstDateValue;
    private String matchDateValue;
    private String timePortion;
    private String dateFormat;
    private boolean useTimePortion;
    private Date parsedFirstDate = null;
    private Date parsedMatchDate = null;
    private long parsedFirstDateTime = 0;
    private long parsedMatchDateTime = 0;
    private boolean flag=false;
    
    
	/**
     * Called to initialize this Child of MessageAdapter.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException{
    	
    	if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, loggingClassName + ": Initializing ..." );

        super.initialize( key, type );

        //Container for error messages
        StringBuffer errorBuffer = new StringBuffer( );
        
    	test = getRequiredPropertyValue(CONDITION_PROP);    	
        
    	nextProc = getRequiredPropertyValue(NEXT_PROCESSOR_PROP);
    	
    	defaultNextProcessorName = getPropertyValue( DEFAULT_NEXT_PROCESSOR_PROP );
    	
    	timePortion = getPropertyValue(USE_TIME_PORTION);

    	dateFormat = getPropertyValue(COMPARE_DATE_FORMAT);
    	
        if ( StringUtils.hasValue( defaultNextProcessorName ) && Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Default-next-processor-name value is [" + defaultNextProcessorName + "]." );
        
        String temp = getPropertyValue( THROW_EXCEPTION_FOR_NO_ROUTE_FLAG_PROP );
        
        if ( StringUtils.hasValue( temp ) )
        {
            try 
            {
                throwExceptionForNoRouteFlag = StringUtils.getBoolean( temp );
            }
            catch ( FrameworkException e )
            {
                errorBuffer.append ( "Property value for " + THROW_EXCEPTION_FOR_NO_ROUTE_FLAG_PROP +
                                     " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }
        
        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.error( errMsg );

            throw new ProcessingException( errMsg );
        }
        
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, loggingClassName + ": Initialization done." );
        
    }
    /**
     * Process the input message and (optionally) return
     * a value.
     *
     * @param  input  Input MessageObject that contains the message to be processed.
     * @param  mpcontext The context
     *
     * @return  Optional output NVPair array, or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if processing fails.
     * @throws java.text.ParseException 
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input ) 
        throws MessageException, ProcessingException {
    	
    	ThreadMonitor.ThreadInfo tmti = null;
    	
    	if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
             Debug.log( Debug.MSG_STATUS, loggingClassName + ": processing ..." );

         if ( input == null )
              return null;

         try{
 			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
 			
 			
	         // Check for 'location=value' type of rule.
	         int index = test.indexOf( '=' );
	         
	         // If we don't have a value to compare ...
	         if ( index == -1 )
	         	new ProcessingException("The test condition must be proper and it should be in format 'Condition=Value'. ");        
	         else
	         {
	            // Perform 'location=value' or 'location=value|value|value...' check.
	         	firstDate = test.substring( 0, index );
	         	matchDate = test.substring( index + 1 );            
	         }
	         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	        	 Debug.log(Debug.MSG_STATUS, "FirstDate["+firstDate+"]");
		         Debug.log(Debug.MSG_STATUS, "MatchDate["+matchDate+"]");
	         }
	         
	         firstDateValue = getString(firstDate, mpcontext, input);
	         matchDateValue = getString(matchDate, mpcontext, input);
	         
	         if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        	 Debug.log(Debug.MSG_STATUS, " Use time portion flag value is ["+timePortion+"]");
	         
	         if(timePortion == null || timePortion.equalsIgnoreCase("false")){
	        	 useTimePortion = false;
	         }else{
	        	 useTimePortion = true;
	         }
	         
	         if(dateFormat == null){
	        	 dateFormat = defaultDateFormat;
	        	 
	        	 if( Debug.isLevelEnabled(Debug.MSG_STATUS))
	        		 Debug.log(Debug.MSG_STATUS, " Date format is NULL. So using defualt date format ["+defaultDateFormat+"]");
	         }
	         
	         if( (matchDateValue == null) || matchDateValue.equals("null") ){
	        	 flag = false;// Forwarding to default message processor.
	        	 
	        	 if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        		 Debug.log(Debug.MSG_STATUS, "Fetched DueDate from LSR is NULL");	        	 
	         }else{
	        	
	        	 if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	 	        	 Debug.log(Debug.MSG_STATUS, " Fetched DUEDATE from LSR is not null.");
	        	 
	        	 if(useTimePortion){
	        		 
	        		 SimpleDateFormat dtFormat = new SimpleDateFormat(dateFormat);
	        		 
	        		 parsedFirstDate = dtFormat.parse(firstDateValue.toString());
	        		 parsedMatchDate = dtFormat.parse(matchDateValue.toString());
	        		 
	        		 parsedFirstDateTime = parsedFirstDate.getTime();
		        	 parsedMatchDateTime = parsedMatchDate.getTime();
		        	  
		        	 if(parsedFirstDateTime == parsedMatchDateTime){
		        		 flag = true;
		        	 }
		        	  
	        	 }
	        	 else{
	        		 //get the date portion from given date format.
	        		 SimpleDateFormat sdfDate = new SimpleDateFormat(dateFormat.substring(0,10));
	        		 
	        		 Date d1 = sdfDate.parse(firstDateValue.toString());
	        		 Date d2 = sdfDate.parse(matchDateValue.toString());
	        		 
	        		 if(d1.equals(d2)){
	        			 flag=true;
	        		 }
	        	 }	        	 
	         }
	         if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        	 Debug.log(Debug.MSG_STATUS, "The date comparator result flag value is [" +flag+ "]");
	          
	          if(flag){
	        	  
	        	  if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        		  Debug.log(Debug.MSG_STATUS, "Condition matched, so routing message to given next processor["+nextProc.toString()+"]");
	    		  
	    		  NVPair[] response = new NVPair[1];
	    		  response [0] = new NVPair(nextProc, input);
	    		  return response;
	          }
	          else{
	        	  if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        		  Debug.log (Debug.MSG_STATUS, "No conditions matched, so routing message to default message-processor location." );
	        	  
	        	// If no conditions matched, and a default next-processor-name configuration was given, adjust
	  	        // the configuration so that the formatNVPair() call uses it.
	  	        if ( StringUtils.hasValue( defaultNextProcessorName ) )
	  	        {
	  	            toProcessorNames = new String[ 1 ];
	
	  	            toProcessorNames[ 0 ] = defaultNextProcessorName;
	  	        }
	  	        else
	  	        {
	  	            if ( throwExceptionForNoRouteFlag == true )
	  	            {
	  	                throw new MessageException( "No conditions evaluated against the given data to a configured route." );
	  	            }
	  	        }
	
	          }
         }
         catch(ParseException pe){
        	 if(Debug.isLevelEnabled(Debug.ALL_ERRORS)){
        		 Debug.log(Debug.ALL_ERRORS, "The given due dates could not be parsed." + pe.getMessage());
        	 }
         }
         catch(Exception e){
        	 String errMsg = "ERROR: SOADateComparator processing failed with error: " + e.getMessage();
 			
        	 if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
        		 Debug.log(Debug.ALL_ERRORS, errMsg);
 			
 			// Re-throw the exception to the driver.
 			if (e instanceof MessageException) {
 				
 				throw (MessageException) e;
 				
 			} else {
 				
 				throw new ProcessingException(errMsg);
 				
 			} 
         }
         finally
 		{
 			ThreadMonitor.stop(tmti);
 		}
         // send the result on through the chain according to properties.
         return( formatNVPair( input ) );
    }
    
	/**
	 * This method tokenizes the input string and return an
	 * object for exsisting value in context or messageobject.
	 *
	 * @param  locations as a string
	 *
	 * @return  object
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 
	 */
	protected String getValue(String locations) throws MessageException,
	ProcessingException {
		StringTokenizer st = new StringTokenizer(locations,
				DBMessageProcessorBase.SEPARATOR);
		
		String tok = null;
		
		// While tokens are available ...
		while (st.hasMoreTokens()) {
			tok = st.nextToken();
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "Checking location [" + tok	+ "] for value...");
			
			// if the value of token exists in context or messageobject.
			if (exists(tok, mpContext, inputObject)) {
				return ((String) get(tok, mpContext, inputObject));
			}
		}
		
		return null;
	}
	
    // Abbreviated class name for use in logging.
    private String loggingClassName;
}