/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.text.*;
import java.io.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.*;


/**
 * This is a generic message-processor for checking dates.
 */
public class DateChecker extends MessageProcessorBase
{
    // Location of request date.
    public static final String REFERENCE_DATE_LOC_PROP = "REFERENCE_DATE_LOC";

    // Format of request date.
    public static final String REFERENCE_DATE_FORMAT_PROP = "REFERENCE_DATE_FORMAT";

    // Location of response date.
    public static final String ACTUAL_DATE_LOC_PROP = "ACTUAL_DATE_LOC";

    // Format of response date.
    public static final String ACTUAL_DATE_FORMAT_PROP = "ACTUAL_DATE_FORMAT";

    // Maximum allowable number of days between the reference and actual dates.
    public static final String MAX_DELTA_DATE_PROP = "MAX_DELTA_DATE";

    // Where to place the result if the max-date-delta is exceeded.
    public static final String RESULT_LOC_PROP = "RESULT_LOC";

    // Names of XML nodes in generated result.
    public static final String ROOT_NODE_NAME = "DateComparisonResult";
    public static final String REFERENCE_DATE_NODE_NAME = "ReferenceDate";
    public static final String ACTUAL_DATE_NODE_NAME = "ActualDate";
    public static final String DIFFERENCE_NODE_NAME = "Difference";
    public static final String PASSED_NODE_NAME = "Passed";


    // Default date format using java.text.SimpleDateFormat semantics.
    public static final String DEFAULT_DATE_FORMAT = "MM-dd-yyyy";

    // Number of mseconds in a day (24 x 60 x 60 x 1000).
    public static final long MSECS_PER_DAY = 86400000;


    /**
     * Constructor.
     */
    public DateChecker ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating date-checker message-processor." );
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

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "DateChecker: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );
        
        // Get required configuration properties specific to this processor.
        referenceDateLoc = getRequiredPropertyValue( REFERENCE_DATE_LOC_PROP, errorBuffer );
        actualDateLoc = getRequiredPropertyValue( ACTUAL_DATE_LOC_PROP, errorBuffer );
        String temp = getRequiredPropertyValue( MAX_DELTA_DATE_PROP, errorBuffer );
        resultLoc = getRequiredPropertyValue( RESULT_LOC_PROP, errorBuffer );

        try
        {
            maxDeltaDate = Integer.parseInt( temp );
        }
        catch ( Exception e )
        {
            errorBuffer.append( "ERROR: Invalid maximum-delta-date value [" + temp + "]." );
        }


        // Get optional configuration properties specific to this processor.
        referenceDateFormat = getPropertyValue( REFERENCE_DATE_FORMAT_PROP );

        if ( !StringUtils.hasValue( referenceDateFormat ) )
            referenceDateFormat = DEFAULT_DATE_FORMAT;

        actualDateFormat = getPropertyValue( ACTUAL_DATE_FORMAT_PROP );

        if ( !StringUtils.hasValue( actualDateFormat ) )
            actualDateFormat = DEFAULT_DATE_FORMAT;

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "DateChecker: Initialization done." );
    }


    /**
     * Extract date values from the context/input and compare them.
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
        if ( inputObject == null )
            return null;

        try
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "DateChecker: processing ... " );
            
            XMLMessageGenerator gen = new XMLMessageGenerator( ROOT_NODE_NAME );

            // Get the request date.
            String referenceDate = getDateValue( referenceDateLoc, mpContext, inputObject );
            
            gen.setValue( REFERENCE_DATE_NODE_NAME, referenceDate );

            SimpleDateFormat sdf = new SimpleDateFormat( referenceDateFormat );
            
            long refDate = sdf.parse(referenceDate).getTime( );
            

            // Get the response date.
            String actualDate = getDateValue( actualDateLoc, mpContext, inputObject );
            
            gen.setValue( ACTUAL_DATE_NODE_NAME, actualDate );

            sdf = new SimpleDateFormat( actualDateFormat );
            
            long actDate = sdf.parse(actualDate).getTime();


            // Calculate the delta and set the result accordingly.
            long delta = (actDate - refDate)/MSECS_PER_DAY;

            gen.setValue( DIFFERENCE_NODE_NAME, String.valueOf(delta) );

            if ( delta > maxDeltaDate )
            {
                Debug.log( Debug.MSG_STATUS, "Date delta of [" + delta + 
                           "] days exceeds configured max value of [" + maxDeltaDate + "] days." );

                gen.setValue( PASSED_NODE_NAME, "false" );
            }
            else
                gen.setValue( PASSED_NODE_NAME, "true" );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, gen.generate() );

            // Put the result in its configured location.
            set( resultLoc, mpContext, inputObject, gen.getDocument() );
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR: Date comparison failed:\n" + e.toString() );

            throw new MessageException( e.toString() );
        }

        // Always return input value to provide pass-through semantics.
        return( formatNVPair( inputObject ) );
    }

    
    /**
     * Extract first date value available from the given locations.
     *
     * @param  locations  A set of '|' delimited XML locations to check.
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The date in string form.
     *
     * @exception  MessageException  Thrown if date can't be obtained.
     * @exception  ProcessingException  Thrown if processing fails.
     */
    protected String getDateValue ( String locations, MessageProcessorContext mpContext, 
                                    MessageObject inputObject ) throws MessageException, ProcessingException
    {
        StringTokenizer st = new StringTokenizer( locations, MessageProcessorBase.SEPARATOR );
        
        while ( st.hasMoreTokens() )
        {
            String tok = st.nextToken( );

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Checking location [" + tok + "] for date value ..." );

            if ( exists( tok, mpContext, inputObject ) )
                return( getString( tok, mpContext, inputObject ) );
        }

        throw new MessageException( "ERROR: Couldn't find date value at locations [" + locations + "]." );
    }


    private String referenceDateLoc;
    private String referenceDateFormat;
    private String actualDateLoc;
    private String actualDateFormat;
    private int maxDeltaDate;
    private String resultLoc;
}
