/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */
package com.nightfire.adapter.messageprocessor;

//JDK packages
import java.util.*;

//NightFire packages
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;

/**
 * Evaluate a series of conditions (in property order), updating the
 * output or context with the result of the first condition that evaluates 
 * to true.
 */
public class ConditionTester extends MessageProcessorBase
{
    /**
     * The condition to evaluate (iterative).
     */
    public static final String CONDITION_PROP = "TEST";

    /**
     * The result value to use if the condition evaluates to 'true' (iterative).
     */
    public static final String RESULT_VALUE_PROP = "RESULT_VALUE";

    /**
     * Output result location - in MessageProcessorContext/MessageObject.
     */
    public static final String RESULT_LOCATION_PROP = "RESULT_LOCATION";


    /**
     * Called to initialize this component.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Initializing ConditionTester..." );

        super.initialize( key, type );

        //Container for error messages
        StringBuffer errorBuffer = new StringBuffer( );

        resultLocation = getRequiredPropertyValue( RESULT_LOCATION_PROP );

        conditions = new LinkedList( );

        // Loop until all condition properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String test = getPropertyValue( PersistentProperty.getPropNameIteration( CONDITION_PROP, Ix ) );

            // If we can't find a condition value, we are done.
            if ( !StringUtils.hasValue( test ) )
                break;

            String result = getPropertyValue( PersistentProperty.getPropNameIteration( RESULT_VALUE_PROP, Ix ) );

            conditions.add( new Condition( this, test, result ) );
        }

        if ( conditions.size() < 1 )
            errorBuffer.append( "At least one condition to evaluate must be specified." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, "ConditionTester: Initialization done." );

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
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input ) 
        throws MessageException, ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "ConditionTester processing.");

        if ( input == null )
             return null;

        Iterator iter = conditions.iterator( );

        boolean passed = false;

        Condition cond = null;

        while( iter.hasNext() )
        {
            cond = (Condition)iter.next( );

            boolean result = cond.evaluate( mpcontext, input );

            if ( result == true )
            {
                if ( StringUtils.hasValue( cond.getResult() ) )
                    set( resultLocation, mpcontext, input, cond.getResult() );

                passed = true;

                break;
            }
        }

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
        {
            if ( passed == true )
                Debug.log( Debug.MSG_STATUS, "A condition evaluated to 'true', resulting in a return-value of [" 
                           + cond.getResult() + "]." );
            else
                Debug.log( Debug.MSG_STATUS, "All conditions evaluated to 'false'." );
            
            Debug.log (Debug.MSG_STATUS, "ConditionTester processing done.");
        }

        // send the result on through the chain according to properties.
        return( formatNVPair( input ) );
    }

    // Adapts protected method on base class for use by Condition class.
    public boolean valueExists ( String name, MessageProcessorContext context, MessageObject input )
    {
        return( exists( name, context, input ) );
    }
 
    // Adapts protected method on base class for use by Condition class.
    public boolean nodeExists ( String name, MessageProcessorContext context, MessageObject input )
    {
        return( exists( name, context, input, false ) );
    }
 
    // Adapts protected method on base class for use by Condition class.
    public String getStringValue ( String name, MessageProcessorContext context, MessageObject input )
        throws ProcessingException, MessageException
    {
        return( getString( name, context, input ) );
    }


    // Class used to encapsulate one condition to evaluate.
    private class Condition
    {
        // Create the condtion object.
        public Condition ( ConditionTester parent, String condition, String result )
        {
            this.parent = parent;

            // Check for 'location=value' type of rule.
            int index = condition.indexOf( '=' );

            // If we don't have a value to compare ...
            if ( index == -1 )
                location = condition;  // Perform existence check.
            else
            {
                // Perform 'location=value' check.
                location = condition.substring( 0, index );

                value = condition.substring( index + 1 );
            }

            this.result = result;
        }

        // Evaluate the condition against the message-processor's input.
        public boolean evaluate ( MessageProcessorContext context, MessageObject input ) 
            throws ProcessingException, MessageException
        {
            boolean evalResult = false;  // Assume condition is initially false.

            // If the location's value should be tested against the condition's value ...
            if ( value != null )
            {
                // If the location exists ...
                if ( parent.valueExists( location, context, input ) )
                {
                    // Extract the value from the message-processor's input/context.
                    String test = parent.getStringValue( location, context, input );
                    
                    // Compare the value with the condition's value.
                    if ( value.equals( test ) )
                        evalResult = true;
                }
            }
            else  // No value was given, so this is an existence test only.
                evalResult = parent.nodeExists( location, context, input );

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            {
                if ( value == null )
                    Debug.log( Debug.MSG_STATUS, "Condition: [" + location + "] exists? [" + evalResult + "]." );
                else
                    Debug.log( Debug.MSG_STATUS, "Condition: [" + location + "] = [" + value + "]? [" + evalResult + "]." );
            }
            
            return evalResult;
        }


        public String getResult ( )
        {
            return result;
        }


        private ConditionTester parent;
        private String location;  // The location of the value to check.
        private String value;  // The optional value to check against.
        private String result;  // The result to return if the condition is true.
    }


    private String resultLocation;
    private List conditions;
}

