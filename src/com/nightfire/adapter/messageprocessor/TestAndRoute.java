/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */
package com.nightfire.adapter.messageprocessor;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.RegexUtils;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * Evaluate a series of conditions (in iterated property order), and 
 * route the input to the first next processor whose condition is true.
 */
public class TestAndRoute extends MessageProcessorBase
{
    /**
     * The condition to evaluate (iterative).
     */
    public static final String CONDITION_PROP = "TEST";

    /**
     * The usage of Regular Expression (iterative).
     */
    public static final String USE_REGEX_PROP = "USE_REGEX";

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
     * Constructor.
     */
    public TestAndRoute ( )
    {
        loggingClassName = StringUtils.getClassName( this );
    }
    
    
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
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, loggingClassName + ": Initializing ..." );

        super.initialize( key, type );

        //Container for error messages
        StringBuffer errorBuffer = new StringBuffer( );

        conditions = new LinkedList( );

        // Loop until all condition properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String test = getPropertyValue( PersistentProperty.getPropNameIteration( CONDITION_PROP, Ix ) );

            // If we can't find a condition value, we are done.
            if ( !StringUtils.hasValue( test ) )
                break;

            String temp = getPropertyValue( PersistentProperty.getPropNameIteration( USE_REGEX_PROP, Ix ) );
            boolean isRegex = StringUtils.getBoolean( temp, false );
            
            String nextProc = getRequiredPropertyValue( PersistentProperty.getPropNameIteration( NEXT_PROCESSOR_PROP, Ix ), errorBuffer );

            conditions.add( new Condition( this, test, nextProc, isRegex ) );
        }

        if ( conditions.size() < 1 )
            errorBuffer.append( "At least one condition to evaluate must be specified." );

        defaultNextProcessorName = getPropertyValue( DEFAULT_NEXT_PROCESSOR_PROP );

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

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Throw-exception-for-no-route value is [" + throwExceptionForNoRouteFlag + "]." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.error( errMsg );

            throw new ProcessingException( errMsg );
        }

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "There are [" + conditions.size() + "] configured conditions." );

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
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input ) 
        throws MessageException, ProcessingException
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": processing ..." );

        if ( input == null )
             return null;

        Iterator iter = conditions.iterator( );

        while( iter.hasNext() )
        {
            Condition cond = (Condition)iter.next( );

            boolean evalResult = cond.evaluate( mpcontext, input );

            if ( evalResult == true )
            {
                NVPair[ ] response = new NVPair[ 1 ];
                response[ 0 ] = new NVPair( cond.getValue(), input );

                return response;
            }
        }

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
        public Condition ( TestAndRoute parent, String condition, String nextProcessor, boolean isRegex )
        {
            this.nextProcessor = nextProcessor;
            this.parent = parent;
            this.isRegex = isRegex;

            // Check for 'location=value' type of rule.
            int index = condition.indexOf( '=' );

            // If we don't have a value to compare ...
            if ( index == -1 )
                location = condition;  // Perform existence check.
            else
            {
                // Perform 'location=value' or 'location=value|value|value...' check.
                location = condition.substring( 0, index );

                value = condition.substring( index + 1 );
            }

            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Condition: " + describe() );
        }

        // Evaluate the condition against the message-processor's input.
        public boolean evaluate ( MessageProcessorContext context, MessageObject input ) 
            throws ProcessingException, MessageException
        {
            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Evaluating condition: " + describe() );

            boolean evalResult = false;  // Assume condition is initially false.

            // No value was given, so this is an existence test only.
            if ( value == null )
                evalResult = parent.nodeExists( location, context, input );            	

            // If the location's value should be tested against the condition's value ...
            else
            {
                // If the location exists ...
                if ( parent.valueExists( location, context, input ) )
                {
                    // Extract the value from the message-processor's input/context.
                    String test = parent.getStringValue( location, context, input );

                    if( isRegex )
                    	evalResult = evaluateRegex(test);
                    else
                    	evalResult = evaluateValue(context, input, test);
                }
            }

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            {
            	StringBuffer sb = new StringBuffer( "Condition: [" ).append( location );

            	if ( value == null )
                    sb.append("] exists? [");
                else
                    sb.append( isRegex?"] matches [":"] = [").append( value ).append( "]? [" ); 
                
                Debug.log( Debug.MSG_STATUS, sb.append( evalResult ).append( "]." ).toString() );
            }
            
            return evalResult;
        }

		/**
		 * Method usually called when the evaluation succeeds. 
		 * @return the value set in the constructor. 
		 */
        public String getValue()
		{
			return nextProcessor;
		}
        
        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );
            
            sb.append( "location [" );
            sb.append( location );
            sb.append( "]" );

            if ( StringUtils.hasValue( value ) )
            {
            	sb.append( isRegex?" matches regular expression [":" = [" );
            	sb.append( value );
            	sb.append( "]?" );
            }
            else
                sb.append( " exists?" );

            sb.append( " -> " );
            sb.append( nextProcessor );
            
            return( sb.toString() );
        }

        /**
         * Perform a regular expression match on an input string using the value field
         * as a regular expression.
		 * @param test Value to perform the Regular Expression match on.
		 * @return whether the value matches the regular expression. 
         * @throws ProcessingException if evaluation fails.
		 */
		private boolean evaluateRegex(String test) throws ProcessingException {
			boolean evalResult = false;
			
			Debug.log( Debug.MSG_STATUS, "Performing Regular Expression evaluation" );
			try {
				evalResult = RegexUtils.match( value, test );
			} 
			catch (FrameworkException e) {
				String err = loggingClassName + ": Regex Evaluation failed: " + e.getMessage();
				Debug.error( err );
				throw new ProcessingException( err );
			}
			return evalResult;
		}

		private boolean evaluateValue(MessageProcessorContext context, MessageObject input, String test) throws ProcessingException, MessageException {

        	boolean evalResult = false; 
        	
			Debug.log( Debug.MSG_STATUS, "Performing value comparison" );

			// Extract out each delimiter-separated value and test each one in turn.
			StringTokenizer st = new StringTokenizer( value, MessageProcessorBase.SEPARATOR );

			while ( st.hasMoreTokens() )
			{
			    String valueItem = st.nextToken( );

			    //check if the value itself indicates a location
			    if ( valueItem.startsWith( "@" ) )
			    {
			        if ( parent.valueExists( valueItem, context, input ) )
			        {
			            // Extract the value from the message-processor's input/context.
			            valueItem = parent.getStringValue( valueItem, context, input );
			        }
			    }

			    // Compare the value with the condition's value.
			    if ( valueItem.equals( test ) )
			    {
			        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
			            Debug.log( Debug.MSG_STATUS, "Got a match on condition: [" + location + "] = [" + valueItem + "]." );

			        evalResult = true;

			        break;
			    }
			}
			return evalResult;
		}

        private TestAndRoute parent;
        private String location;  // The location of the value to check.
        private String value;  // The optional value to check against.
        private boolean isRegex;
        private String nextProcessor;  // The name of the next processor to send the data to if the condition is true.
    }


    private List conditions;

    private String defaultNextProcessorName;

    // For backwards-compatibility, the default is to have the same behavior as before.
    private boolean throwExceptionForNoRouteFlag = false;

    // Abbreviated class name for use in logging.
    private String loggingClassName;
}

