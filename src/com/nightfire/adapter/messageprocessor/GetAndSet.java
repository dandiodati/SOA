/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.db.*;


/**
 * This is a generic message-processor taking values from a location in the
 * MessageObject/MessageProcessorContext/fixed values and putting them
 * into another location in the MessageObject/MessageProcessorContext.
 */
public class GetAndSet extends MessageProcessorBase
{
    // Property prefix giving location of input data.
    public static final String INPUT_LOC_PREFIX_PROP = "INPUT_LOC";

    // Property prefix giving location where fetched data is to be set.
    public static final String OUTPUT_LOC_PREFIX_PROP = "OUTPUT_LOC";

    // Property prefix for default value to be used.
    public static final String DEFAULT_PREFIX_PROP = "DEFAULT";

    // Property prefix for indicating if input location should actually contain data.
    public static final String OPTIONAL_PREFIX_PROP = "OPTIONAL";


    /**
     * Constructor.
     */
    public GetAndSet ( )
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Creating GetAndSet message-processor." );

        getSetList = new LinkedList ( );
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
            Debug.log(Debug.SYSTEM_CONFIG, "GetAndSet: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );

        //Loop until all configuration properties have been read.
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String inputLoc = getPropertyValue( PersistentProperty.getPropNameIteration( INPUT_LOC_PREFIX_PROP, Ix ) );
            String outputLoc = getPropertyValue( PersistentProperty.getPropNameIteration( OUTPUT_LOC_PREFIX_PROP, Ix ) );
            
            //if neither input or output locations are found, we are done.
            if ( !StringUtils.hasValue( inputLoc ) && !StringUtils.hasValue( outputLoc ) )
                break;
            
            if ( !StringUtils.hasValue( outputLoc ) )
            {
                errorBuffer.append( "ERROR: Missing required output-location configuration item." );
                
                continue;
            }
            
            String defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( DEFAULT_PREFIX_PROP, Ix ) );
            String optional = getPropertyValue( PersistentProperty.getPropNameIteration( OPTIONAL_PREFIX_PROP, Ix ) );

            try
            {
                // Create a new GetSetConfig object and add it to the list.
                GetSetConfig gsc = new GetSetConfig( inputLoc, outputLoc, defaultValue, optional );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                    Debug.log( Debug.SYSTEM_CONFIG, gsc.describe() );

                getSetList.add( gsc );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( "ERROR: Could not create getSetValues:\n"
                                               + e.toString() );
            }

        }//for


        // Make sure at least 1 get-set configuration has been initialized.
        if ( getSetList.size() < 1 )
            errorBuffer.append( "ERROR: No get-set configuration values were given." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "GetAndSet: Initialization done." );
    }


    /**
     * Extract values from one set of locations from the context/input/default value
     * and associate them with new locations in the context/input.
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

        // Iterate over get-set configurations.
        Iterator iter = getSetList.iterator( );

        while ( iter.hasNext() )
        {
            GetSetConfig gsc = (GetSetConfig)iter.next( );

            Object sourceValue = null;

            if ( StringUtils.hasValue( gsc.inputLoc ) )
                sourceValue = getValue( gsc.inputLoc, mpContext, inputObject );

            // If no value was found, use the default, if available.
            if (  sourceValue == null )
                sourceValue = gsc.defaultValue;

            // If we still don't have a value, and its not optional, signal error to caller.
            if ( sourceValue == null )
            {
                if ( gsc.optional )
                {
                    Debug.log( Debug.MSG_STATUS, "Skipping missing but optional value for source location [" 
                               + gsc.inputLoc + "]." );
                    continue;
                }
                else
                {
                    throw new MessageException( "ERROR: Couldn't locate input value in [" 
                                                + gsc.inputLoc + "]." );
                }
            }

            // Put the result in its configured location.
            set( gsc.outputLoc, mpContext, inputObject, sourceValue );
        }
        
        // Always return input value to provide pass-through semantics.
        return( formatNVPair( inputObject ) );
    }


    /**
     * Extract first value available from the given locations.
     *
     * @param  locations  A set of '|' delimited XML locations to check.
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The requested value.
     *
     * @exception  MessageException  Thrown on non-processing errors.
     * @exception  ProcessingException  Thrown if processing fails.
     */
    protected Object getValue ( String locations, MessageProcessorContext mpContext,
                                MessageObject inputObject ) throws MessageException, ProcessingException
    {
        StringTokenizer st = new StringTokenizer( locations, MessageProcessorBase.SEPARATOR );

        while ( st.hasMoreTokens() )
        {
            String tok = st.nextToken( );

            Debug.log( Debug.MSG_STATUS, "Checking location [" + tok + "] for value ..." );

            if ( exists( tok, mpContext, inputObject ) )
                return( get( tok, mpContext, inputObject ) );
        }

        return null;
    }


    // Class encapsulating one get-set's worth of configuration information.
    private static class GetSetConfig
    {
        public final String inputLoc;
        public final String outputLoc;
        public final String defaultValue;
        public final boolean optional;

        public GetSetConfig ( String inputLoc, String outputLoc, String defaultValue, String optional )
              throws FrameworkException
        {
            this.inputLoc     = inputLoc;
            this.outputLoc    = outputLoc;
            this.defaultValue = defaultValue;
            
            if ( StringUtils.hasValue( optional ) )
                this.optional = StringUtils.getBoolean( optional );
            else
                this.optional = false;
        }
        
        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Description: Location to fetch value from [" );
            sb.append( inputLoc );

            sb.append( "], location to set value to  [" );
            sb.append( outputLoc );

            if ( StringUtils.hasValue( defaultValue ) )
            {
                sb.append( "], default [" );
                sb.append( defaultValue );
            }

            sb.append( "], optional [" );
            sb.append( optional );

            sb.append( "]." );

            return( sb.toString() );
        }//describe

    }//GetSetConfig


    private List getSetList;
}//end of class GetAndSet
