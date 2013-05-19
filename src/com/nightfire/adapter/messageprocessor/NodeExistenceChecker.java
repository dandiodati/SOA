/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.messageprocessor;

import java.util.*;
import java.io.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.db.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;

/**
 * Checks for the existence of a node in the given xml input message 
 *
 * and puts a predefined value into the context.
 */
public class NodeExistenceChecker extends MessageProcessorBase
{

    public static final String OUTPUT_LOC_TAG = "OUTPUT_LOCATION";

    public static final String THROW_EXCEPTION_TAG = "THROW_EXCEPTION";

    public static final String INPUT_NODE_NAME_TAG = "INPUT_NODE_NAME";

    public static final String OUTPUT_VALUE_TAG = "OUTPUT_VALUE";

    public static final String NODE_NOT_FOUND = "NodeNotFound";

    private String outputLocation;

    private String throwExceptionFlag;
    
    /**
     * Initializes this adapter with persistent properties
     *
     * @param  key  Property-key to use for locating initialization properties.
     *
     * @param  type Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization is not successful.
     */
    public void initialize( String key, String type ) throws ProcessingException
    {
        super.initialize(key, type);

        StringBuffer sb = new StringBuffer();
        
        outputLocation = getRequiredPropertyValue ( OUTPUT_LOC_TAG, sb );

        throwExceptionFlag = getRequiredPropertyValue ( THROW_EXCEPTION_TAG, sb );

        //Throw error if any of the required properties are missing 
        if ( sb.length() > 0 )
        {
            throw new ProcessingException( sb.toString() );
        }
        
    }


    /**
     * Processes the input message and (optionally) return
     * a name / value pair.
     *
     * @param  context The Message Processor context
     *
     * @param  inputObject  Input message Object to process.
     *
     * @return  Optional NVPair containing a Destination name and a Document,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if the given input message is bad.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject inputObject )
        throws MessageException, ProcessingException 
    {
        String outputValue = null;
        
        if ( inputObject == null || inputObject.getDOM() == null )
        {
            return null;
        }
        
        XMLMessageParser parser = new XMLMessageParser( inputObject.getDOM() );

        int counter = 0;

        while (true)
        {
            
            String theNodeNameTag;

            String theNodeName;

            String theOutputValueTag;

            String theOutputValue;

            theNodeNameTag = PersistentProperty.getPropNameIteration( INPUT_NODE_NAME_TAG, counter );

            theOutputValueTag = PersistentProperty.getPropNameIteration( OUTPUT_VALUE_TAG, counter );

            theNodeName = (String) getPropertyValue( theNodeNameTag );

            theOutputValue = (String) getPropertyValue( theOutputValueTag );
            
            if ( ! StringUtils.hasValue ( theNodeName ) )
            {			
                break;
            }
                            
            Debug.log(Debug.MSG_STATUS, "Checking the existance of node [" + theNodeName +"] in input message...");

            if ( parser.exists ( theNodeName ) )
            {
                
                outputValue = theOutputValue;

                Debug.log(Debug.MSG_STATUS, "Found node [" + theNodeName +"], therefore value ["
                          +theOutputValue +"] will be set in the message processor context " );
                break;
            }

            counter ++;
        }

        //Check whether the node was found 
        if ( outputValue == null )
        {

            //Throw exception if required
            if ( Boolean.valueOf( throwExceptionFlag ).booleanValue() == true )
            {
                throw new ProcessingException(" NodeExistenceChecker could not locate any of the specified" +
                                              " nodes in the given input message ");            
            }

            else 
            {
                outputValue = NODE_NOT_FOUND ;                
            }
        }
        
        Debug.log(Debug.MSG_STATUS, "Setting value = [" +outputValue +
                  "] to message processor context at location [" +outputLocation +"] ");

        set( outputLocation, context, inputObject, outputValue );

        return formatNVPair(inputObject);
    }



}
