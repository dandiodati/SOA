/**
 * Copyright (c) 2002 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //spi/neustar_soa/main/com/nightfire/spi/neustar_soa/adapter/messageprocessor
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.*;
import java.text.*;
import java.io.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.db.*;
import com.nightfire.framework.rules.Value;


/**
 * This is a generic message-processor taking values from a location in the
 * MessageObject/MessageProcessorContext/fixed values and putting them
 * into another location in the MessageObject/MessageProcessorContext.
 */
public class TnRangeSplitter extends MessageProcessorBase
{
    // Property prefix giving location of input data.
    public static final String INPUT_LOC_TNRANGE_PROP = "INPUT_TNRANGE_LOC";

    // Property prefix giving location where fetched data is to be set.
    public static final String OUTPUT_LOC_START_TN_PROP = "OUTPUT_LOC_START_TN";


    // Property prefix giving location where fetched data is to be set.
    public static final String OUTPUT_LOC_END_TN_PROP = "OUTPUT_LOC_END_TN";
    public static final String OUTPUT_LOC_END_STATION_PROP = "OUTPUT_LOC_END_STATION";

    public static final String TN_DELIMITER_PROP = "TN_DELIMITER";
    public static final int TN_LENGTH = 12;

    private static final String DEFAULT_DELIMITER = "-";

    private String outputLocEndTn ="";
    private String outputLocStartTn ="";
    private String inputLoc ="";
    private String tnDelimiter ="";
    private String startTn ="";
    private String endTn ="";
    private String endStation ="";
    private String outputLocEndStation="";



    /**
     * Constructor.
     */
    public TnRangeSplitter ()
    {
        if( Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE) ){
			Debug.log( Debug.OBJECT_LIFECYCLE, "Creating TnRangeSplitter message-processor." );
		}
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
         Debug.log( Debug.SYSTEM_CONFIG, "TnRangeSplitter: Initializing..." );
		}

        StringBuffer errorBuffer = new StringBuffer( );
        inputLoc = getRequiredPropertyValue( INPUT_LOC_TNRANGE_PROP,errorBuffer );
        outputLocStartTn = getRequiredPropertyValue( OUTPUT_LOC_START_TN_PROP,errorBuffer);
        outputLocEndTn = getRequiredPropertyValue( OUTPUT_LOC_END_TN_PROP,errorBuffer);

        //This is not a required for Async. Response Chain..
        outputLocEndStation = getPropertyValue(OUTPUT_LOC_END_STATION_PROP);

        tnDelimiter = getPropertyValue(TN_DELIMITER_PROP);

        if(!StringUtils.hasValue(tnDelimiter))
        tnDelimiter = DEFAULT_DELIMITER;



        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
         Debug.log( Debug.SYSTEM_CONFIG, "TnRangeSplitter: Initialization done." );
		}
    }


    /**
     * Calls the splitTnRange method and splits the Range into StartTn and EndTn.
     * Also sets the StartTn and EndTn into context or MessageObject.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                        throws MessageException, ProcessingException
    {
    	ThreadMonitor.ThreadInfo tmti = null;
        if ( inputObject == null )
            return null;
        try
        {
        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
        
        String tnRangeValue = (String)getValue( inputLoc, mpContext, inputObject );
        // Does no processing if the Tn value is not available..
        if(!StringUtils.hasValue(tnRangeValue))
            return formatNVPair(inputObject);


        if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
			Debug.log(Debug.MSG_STATUS,"TnRangeSplitter: Splitting the TNRange.");
		}

        splitTnRange(tnRangeValue);
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
        Debug.log(Debug.MSG_STATUS,"TnRangeSplitter: Setting the StartTn with value = "
        +startTn+"and EndTn with value = "+endTn);
		}

        set( outputLocStartTn, mpContext, inputObject, startTn );
        set( outputLocEndTn, mpContext, inputObject, endTn );

        if(StringUtils.hasValue(outputLocEndStation))
        set( outputLocEndStation, mpContext, inputObject, endStation );
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
          Debug.log(Debug.MSG_STATUS,"TnRangeSplitter: Set the StartTn and EndTn values.");
		}

        }
        finally
        {
        	ThreadMonitor.stop(tmti);
        }
        // Always return input value to provide pass-through semantics.
        return( formatNVPair( inputObject ) );
    }

    /**
     * This method tokenizes the input string and return an object for exsisting value
     * in context or messageobject.
     */
    protected Object getValue ( String locations, MessageProcessorContext mpContext,
                                MessageObject inputObject ) throws MessageException, ProcessingException
    {
        StringTokenizer st = new StringTokenizer( locations, MessageProcessorBase.SEPARATOR );

        while ( st.hasMoreTokens() )
        {
            String tok = st.nextToken( );
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
              Debug.log( Debug.MSG_STATUS, "Checking location [" + tok + "] for value ..." );
			}

            if ( exists( tok, mpContext, inputObject ) )
                return( get( tok, mpContext, inputObject ) );
        }

        return null;
    }

    /**
     * This method splits the Telephone number range into the start and end telephone number.
     * If the telephoneRange is of type NPA-NXX-XXXX then the start and end TN are equal
     * to the telephoneRange. If the telephoneRange is of type NPA-NXX-XXXX-YYYY
     * then the StartTn is set as NPA-NXX-XXXX and EndTn is set as NPA-NXX- YYYY.
     * Also the endStation value is populated for the XML2IDL Mapper.
     *
     * @param  tnRangeValue The TnRange as a string.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     */
    private void splitTnRange(String tnRangeValue)throws ProcessingException
    {

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(Debug.MSG_STATUS,"TnRangeSplitter: Entered the splitTnRange method.");
		}

        Value tnRange = new Value(tnRangeValue);

        try
        {
            StringTokenizer tokenizeRange = new StringTokenizer(tnRangeValue,tnDelimiter);
            String npaVal = tokenizeRange.nextToken();
            String nxxVal = tokenizeRange.nextToken();

            if(Debug.isLevelEnabled(Debug.UNIT_TEST))
            Debug.log(Debug.UNIT_TEST,"Npa-->"+npaVal+" NXX->"+nxxVal+tnRange.isTelephoneNumberWithExtension()+tnRange.isTelephoneNumber()+tnRange.hasFormat("NNN-NNN-NNNN"));
            
			if(tnRangeValue.length()>TN_LENGTH)
            {

                startTn = npaVal+tnDelimiter+nxxVal+tnDelimiter+tokenizeRange.nextToken();
                endStation = tokenizeRange.nextToken();
                endTn = npaVal+tnDelimiter+nxxVal+tnDelimiter+endStation;
            }
            else
            {
                endStation = tokenizeRange.nextToken();
                startTn = tnRangeValue;
                endTn = tnRangeValue;
            }
        }
        catch(NoSuchElementException nse)
        {
            //will never happen as this processor is after the rule processor.
            throw new ProcessingException("TnRangeSplitter: Error: The Telephone Number Range is not in correct format."
            +" The correct format is NNN-NNN-NNNN-NNNN/NNN-NNN-NNNN ");
        }

    }
     //--------------------------For Testing---------------------------------//

    public static void main(String[] args)
    {

        Properties props = new Properties();
        props.put( "DEBUG_LOG_LEVELS", "all" );
        props.put( "LOG_FILE", "d:\\logmap.txt" );
        Debug.showLevels( );
        Debug.configureFromProperties( props );
        if (args.length != 3)
        {
          Debug.log (Debug.ALL_ERRORS, "TnRangeSplitter: USAGE:  "+
          " jdbc:oracle:thin:@172.25.161.72:1521:nf test test ");
          return;
        }
        try
        {

            DBInterface.initialize(args[0], args[1], args[2]);
        }
        catch (DatabaseException e)
        {
             Debug.log(null, Debug.MAPPING_ERROR, ": " +
                      "Database initialization failure: " + e.getMessage());
        }


        TnRangeSplitter fgn = new TnRangeSplitter();

        try
        {
            fgn.initialize("SOAORDERPATH","OP_TNRANGE_FORMATTER");
            MessageProcessorContext mpx = new MessageProcessorContext();
            MessageObject mob = new MessageObject();
            //mpx.set("Range","123-333-4444-5555");
            mpx.set("Range1","123-333-4444-7777");
            fgn.process(mpx,mob);

            Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

        }
        catch(ProcessingException pex)
        {
            System.out.println(pex.getMessage());
        }
        catch(MessageException mex)
        {
            System.out.println(mex.getMessage());
        }
    } //end of main method

}//end of class TnRangeSplitter
