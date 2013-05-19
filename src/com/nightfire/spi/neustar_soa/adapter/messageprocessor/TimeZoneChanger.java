/**
 * Copyright (c) 2002 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //spi/neustar_soa/main/com/nightfire/spi/neustar_soa/adapter/messageprocessor
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;


import java.util.*;
import java.text.*;
import java.io.*;

import org.w3c.dom.Node;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.util.xml.CachingXPathAccessor;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.db.*;



/**
 * This message processor takes times from locations in the
 * MessageObject/MessageProcessorContext, changes the time zone
 * and/or format and puts them into locations in the
 * MessageObject/MessageProcessorContext.
 */
public class TimeZoneChanger extends MessageProcessorBase {


    /**
     * <code>INPUT_TIME_ZONE_PROP</code> takes the static final String
     * value "INPUT_TIME_ZONE". This required property gives the time
     * zone of the times in the input message. The time zone values
     * are those supported by the java.util.TimeZone class as given by
     * the static method java.util.TimeZone.getAvailableIDs(). Setting
     * the Value to "LOCAL" will give the local time zone.
     *
     */
    public static String INPUT_TIME_ZONE_PROP = "INPUT_TIME_ZONE";

    /**
     * <code>OUTPUT_TIME_ZONE_PROP</code> takes the static final
     * String value "OUTPUT_TIME_ZONE". This required property gives
     * the time zone of the times in the output message.
     *
     */
    public static String OUTPUT_TIME_ZONE_PROP = "OUTPUT_TIME_ZONE";

    /**
     * <code>INPUT_FORMAT_PROP</code> takes the static final
     * String value "INPUT_FORMAT". This required property gives
     * the format that will be used to parse the times in the
     * input message. The time formats are those supported by
     * the java.text.SimpleDateFormat class.
     *
     */
    public static String INPUT_FORMAT_PROP = "INPUT_FORMAT";

    /**
     * <code>OUTPUT_FORMAT_PROP</code> takes the static final
     * String value "OUTPUT_FORMAT". This required property gives
     * the format of the times in the output message.
     *
     */
    public static String OUTPUT_FORMAT_PROP = "OUTPUT_FORMAT";

    /**
     * <code>INPUT_LOC_PREFIX_PROP</code> takes the static final
     * String value "INPUT_LOC". This required property gives
     * the locations of the times in the input message that will
     * be converted.
     *
     */
    public static final String INPUT_LOC_PREFIX_PROP = "INPUT_LOC";

    /**
     * <code>OUTPUT_LOC_PREFIX_PROP</code> takes the static final
     * String value "OUTPUT_LOC". This required property gives
     * the locations to which the times will be written in the
     * output message.
     *
     */
    public static final String OUTPUT_LOC_PREFIX_PROP = "OUTPUT_LOC";

    /**
     * This Property indicates whether the node is inside a
     * container node and the processing is then handled accordingly.
     */
    private static final String CONTAINER_CHILDNODE_INDICATOR = "(*)";

    /**
     * The length of the CONTAINER_CHILDNODE_INDICATOR variable.
     */
    private static final int CHILDNODE_INDICATOR_LENGTH = CONTAINER_CHILDNODE_INDICATOR.length();

    /**
     * The Round brackets used to access the child nodes.
     * For eg: in xml
     * <Container type="container">
     *    <ActivityDetail>
     *      <Datetime = "20021212101010.GMT">
     *    </ActivityDetail>
     *    <ActivityDetail>
     *      <Datetime = "20021010121234.GMT">
     *    </ActivityDetail>
     * </Container>
     * The Datetime node is accessed as Container.ActivityDetail(0).Datetime
     * and Container.ActivityDetail(1).Datetime.
     */
    private static final String OPEN_PAREN = "(";
    private static final String CLOSED_PAREN = ")";

    /**
     * Constructor.
     */
    public TimeZoneChanger ( )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log( Debug.OBJECT_LIFECYCLE, "Creating TimeZoneChanger message-processor." );
		}

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
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "TimeZoneChanger: Initializing..." );
		}

        StringBuffer errorBuffer = new StringBuffer( );

	String timeZone = getRequiredPropertyValue ( INPUT_TIME_ZONE_PROP, errorBuffer );

	if ( timeZone.equalsIgnoreCase("LOCAL") ) {
	    inputTimeZone = TimeZone.getDefault();
	} else {
	    inputTimeZone = TimeZone.getTimeZone( timeZone );
	}

        timeZone = getRequiredPropertyValue ( OUTPUT_TIME_ZONE_PROP, errorBuffer );

	if ( timeZone.equalsIgnoreCase("LOCAL") ) {
	    outputTimeZone = TimeZone.getDefault();
	} else {
	    outputTimeZone = TimeZone.getTimeZone( timeZone );
	}

	inputFormat = getRequiredPropertyValue ( INPUT_FORMAT_PROP, errorBuffer );

	outputFormat = getRequiredPropertyValue ( OUTPUT_FORMAT_PROP, errorBuffer );


        //Loop until all location properties have been read.
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String inputLoc = getPropertyValue( PersistentProperty.getPropNameIteration( INPUT_LOC_PREFIX_PROP, Ix ) );
            String outputLoc = getPropertyValue( PersistentProperty.getPropNameIteration( OUTPUT_LOC_PREFIX_PROP, Ix ) );
            // allow the user to input only the input location if the output location is the same as the input:
	    if ( !StringUtils.hasValue( outputLoc ) ) {
		outputLoc = inputLoc;
	    }

            //if neither input or output locations are found, we are done.
            if ( !StringUtils.hasValue( inputLoc ) && !StringUtils.hasValue( outputLoc ) )
                break;

            try
            {
                // Create a new TimeZoneChangerConfig object and add it to the list.
                TimeZoneChangerConfig tzcc = new TimeZoneChangerConfig( inputLoc, outputLoc );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                    Debug.log( Debug.SYSTEM_CONFIG, tzcc.describe() );

                getSetList.add( tzcc );
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
        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
         Debug.log( Debug.SYSTEM_CONFIG, "TimeZoneChanger: Initialization done." );
		}
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
    	ThreadMonitor.ThreadInfo tmti = null;
        if ( inputObject == null )
            return null;
        try
        {
        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
        
        // Iterate over get-set configurations.
        Iterator iter = getSetList.iterator( );

        while ( iter.hasNext() )
        {
            TimeZoneChangerConfig tzcc = (TimeZoneChangerConfig)iter.next( );

            Object sourceValue = null;
            if((tzcc.inputLoc).indexOf(CONTAINER_CHILDNODE_INDICATOR)!=-1)
            {
                setChildNodeVal( tzcc.inputLoc,mpContext,inputObject);
                continue;
            }
            if ( StringUtils.hasValue( tzcc.inputLoc ) )
                sourceValue = getValue( tzcc.inputLoc, mpContext, inputObject );

            // If we still don't have a value signal error to caller.
            if ( sourceValue == null ||sourceValue.equals(""))
            {
		       continue;
            }

	        String convertedTime = convertTime( (String) sourceValue );


            // Put the result in its configured location.
            set( tzcc.outputLoc, mpContext, inputObject, convertedTime );
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
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
              Debug.log( Debug.MSG_STATUS, "Checking location [" + tok + "] for value ..." );
			}

            if ( exists( tok, mpContext, inputObject ) )
                return( get( tok, mpContext, inputObject ) );
        }

        return null;
    }

    /**
     * This method sets the conatiner child nodes with coneverted datetime.
     * The output location is same as input Location.
     * @param  inputLoc  a set of '|' delimited XML locations to check.
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @exception  MessageException  Thrown on non-processing errors.
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private void setChildNodeVal(String inputLoc,MessageProcessorContext mpContext,
    MessageObject inputObject)throws ProcessingException,MessageException
    {
        int index =inputLoc.indexOf(CONTAINER_CHILDNODE_INDICATOR);
        //Counter for container child nodes.
        int j =0;
        String inputNode ="";
        String inputInnerNode ="";

        if(index !=-1)
        {
            inputInnerNode = inputLoc.substring(0,index);
            inputNode = inputInnerNode+OPEN_PAREN+j+
                        CLOSED_PAREN+inputLoc.substring
                                             (index+CHILDNODE_INDICATOR_LENGTH);
            //Extract a location alternative.
            
            String loc = inputInnerNode;

            // Replace "." with "/" to make it XPATH expresison.
            loc = "//SOAMessage/" + loc.replaceAll("[.]", "/");

            // Attempt to get the value from the context or input object.
            // get all the matching nodes of the given input location.
            Node matchingNodes[] = getAllMatchingNodes(inputObject, loc);
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS,"matchingNodes.length :"+matchingNodes.length);
            
           if(matchingNodes != null ){
	            while(j < matchingNodes.length )
	            {
	                if(this.exists(inputNode,mpContext,inputObject)){
		            	inputLoc = inputNode;
		
		                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		                Debug.log(Debug.MSG_STATUS,"The input location is :"+inputNode);
		
		                Object sourceValue = getValue( inputLoc, mpContext, inputObject );
		
		                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		                Debug.log(Debug.MSG_STATUS,"The Val for date is  :"+(String)sourceValue);
		
		                //increment the counter for [*] nodes.
		                j= j+1;
		
		                //Looking for next child node inside the container node.
		                inputNode = inputInnerNode+OPEN_PAREN+j+CLOSED_PAREN
		                            +inputLoc.substring(index+CHILDNODE_INDICATOR_LENGTH);
		
		                if ( sourceValue == null ||sourceValue.equals(""))
		                {
		                    continue;
		                }
		
		                String convertedTime = convertTime( (String) sourceValue );
		
		                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		                Debug.log(Debug.MSG_STATUS,"The Converted Time is :"+convertedTime);
		
		                // Put the result in its configured location.
		                set( inputLoc, mpContext, inputObject, convertedTime );
		                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		                 Debug.log(Debug.MSG_STATUS,"Set the value in the output node.");
						}
	                }else{
	                   //increment the counter for [*] nodes.
		                j= j+1;
		
		                //Looking for next child node inside the container node.
		                inputNode = inputInnerNode+OPEN_PAREN+j+CLOSED_PAREN
		                            +inputLoc.substring(index+CHILDNODE_INDICATOR_LENGTH);
		              }
	              }
              }
        }
        return;
    }
    /**
     * Returns all the child nodes of a node, represented by
     * the given inputLocation.
     *
     * @param inputObject The input object
     * @param inputLocation Input location for the values of multi valued field.
     *
     * @return return all the children of the node represented by input
     * 		   location or return an empty array of Node.
     *
     */
    private Node[] getAllMatchingNodes ( MessageObject inputObject, String inputLocation )
    {
        if(inputObject == null)
        {
            return null;
        }

        try
        {
            CachingXPathAccessor cc = new CachingXPathAccessor(inputObject.getDOM());
            return cc.getNodes(inputLocation);
        }
        catch (Exception e)
        {
            /* if nothing available on the given location,
             * just return an empty array of Node. */
            return new Node[0];
        }
    }
    // Parse the time from the input string using the input time format and
    // input time zone. Change the time zone and return the reformatted string.
    private String convertTime ( String inputTime ) throws MessageException {

	SimpleDateFormat sdf = new SimpleDateFormat( inputFormat );
	sdf.setTimeZone( inputTimeZone );

	Date date = null;
	try {

	    date = sdf.parse( inputTime );

	} catch (ParseException e) {

	    throw new MessageException( "Source time, [" + inputTime + "], cannot be parsed.");
	}

	sdf  = new SimpleDateFormat( outputFormat );
	sdf.setTimeZone( outputTimeZone );

	return sdf.format( date );
    }




    // Class encapsulating one get-set's worth of configuration information.
    private static class TimeZoneChangerConfig
    {
        public final String inputLoc;
        public final String outputLoc;

        public TimeZoneChangerConfig ( String inputLoc, String outputLoc )
              throws FrameworkException
        {
            this.inputLoc     = inputLoc;
            this.outputLoc    = outputLoc;
        }

        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Description: Location to fetch value from [" );
            sb.append( inputLoc );

            sb.append( "], location to set value to  [" );
            sb.append( outputLoc );

            sb.append( "]." );

            return( sb.toString() );
        }//describe

    }//TimeZoneChangerConfig

    private TimeZone inputTimeZone;
    private TimeZone outputTimeZone;

    private String inputFormat;
    private String outputFormat;

    private List getSetList;
}//end of class TimeZoneChanger
